// src/app/components/home/home.component.ts

import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { PublicacaoServico, PublicacaoServicoService, TipoPublicacao } from '../../service/publicacao-servico.service';
import { LocationService } from '../../service/location.service';

type DisponibilidadeFiltro = 'TODOS' | 'URGENTE' | 'HOJE';
type AtendimentoFiltro = 'TODOS' | 'DOMICILIO' | 'ONLINE' | 'PRESENCIAL';

interface CategoriaServico {
  nome: string;
  subcategorias: string[];
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, OnDestroy {
  titulo = 'Encontre prestação e contratação de serviços no BR-Jobs';

  termoBusca = '';
  filtroTipo: 'TODAS' | TipoPublicacao = 'TODAS';

  categoriaSelecionada = '';
  subcategoriaSelecionada = '';
  tagsSelecionadas: string[] = [];
  localizacaoBusca = '';
  precoMin: number | null = null;
  precoMax: number | null = null;
  avaliacaoMinima = 0;
  disponibilidadeFiltro: DisponibilidadeFiltro = 'TODOS';
  atendimentoFiltro: AtendimentoFiltro = 'TODOS';

  readonly categoriasServico: CategoriaServico[] = [
    {
      nome: 'Reformas e Construção',
      subcategorias: ['Pedreiro', 'Reforma em geral', 'Assentamento de piso/porcelanato', 'Gesso / Drywall', 'Impermeabilização']
    },
    {
      nome: 'Elétrica',
      subcategorias: ['Instalação elétrica', 'Manutenção', 'Curto / emergência', 'Instalação de chuveiro', 'Iluminação']
    },
    {
      nome: 'Hidráulica',
      subcategorias: ['Encanador', 'Vazamentos', 'Instalação de torneiras', 'Desentupimento', "Caixa d'água"]
    },
    {
      nome: 'Marcenaria e Móveis',
      subcategorias: ['Móveis planejados', 'Montagem de móveis', 'Reparos', 'Carpintaria']
    },
    {
      nome: 'Pintura e Acabamento',
      subcategorias: ['Pintura residencial', 'Pintura comercial', 'Textura / grafiato', 'Retoques']
    },
    {
      nome: 'Limpeza e Serviços Domésticos',
      subcategorias: ['Faxina', 'Diarista', 'Limpeza pós-obra', 'Limpeza pesada', 'Passadeira']
    },
    {
      nome: 'Marido de Aluguel',
      subcategorias: ['Pequenos reparos', 'Instalações', 'Serviços rápidos']
    },
    {
      nome: 'Climatização',
      subcategorias: ['Ar-condicionado (instalação)', 'Manutenção', 'Higienização']
    },
    {
      nome: 'Assistência Técnica',
      subcategorias: ['Geladeira', 'Máquina de lavar', 'TV', 'Micro-ondas', 'Computadores']
    },
    {
      nome: 'Automotivo',
      subcategorias: ['Mecânico', 'Elétrica automotiva', 'Socorro / emergência']
    },
    {
      nome: 'Eventos',
      subcategorias: ['Organização de eventos', 'Garçom', 'Buffet', 'Decoração', 'Som e iluminação']
    },
    {
      nome: 'Jardim e Exterior',
      subcategorias: ['Jardinagem', 'Limpeza de terreno', 'Corte de grama', 'Paisagismo']
    },
    {
      nome: 'Segurança e Instalações',
      subcategorias: ['Câmeras (CFTV)', 'Alarmes', 'Cerca elétrica', 'Interfone']
    }
  ];

  readonly tagsDisponiveis = ['urgente', '24h', 'barato', 'residencial', 'comercial'];

  publicacoesFiltradas: PublicacaoServico[] = [];
  carregando = false;
  erro = '';
  paginaAtual = 0;
  readonly tamanhoPagina = 20;
  totalElementos = 0;
  ultimaPagina = true;

  private resultadosLocais: PublicacaoServico[] = [];
  private usandoFiltroLocal = false;
  private cachePublicacoes: PublicacaoServico[] = [];

  private debounceHandle: ReturnType<typeof setTimeout> | null = null;
  private localizacaoDebounceHandle: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private readonly publicacaoService: PublicacaoServicoService,
    private readonly locationService: LocationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.buscarPublicacoes(true);
  }

  ngOnDestroy(): void {
    if (this.debounceHandle) {
      clearTimeout(this.debounceHandle);
    }
    if (this.localizacaoDebounceHandle) {
      clearTimeout(this.localizacaoDebounceHandle);
    }
  }

  buscarPublicacoes(resetPage = false): void {
    if (resetPage) {
      this.paginaAtual = 0;
    }

    this.carregando = true;
    this.erro = '';

    const tipo = this.filtroTipo === 'TODAS' ? undefined : this.filtroTipo;
    const termo = this.buildTermoBusca();

    if (this.temFiltrosAvancadosAtivos()) {
      this.usandoFiltroLocal = true;
      this.carregarComFiltrosLocais(tipo);
      return;
    }

    this.usandoFiltroLocal = false;

    this.publicacaoService.buscarPaginado({
      tipo,
      termo: termo || undefined,
      page: this.paginaAtual,
      size: this.tamanhoPagina,
      lat: this.locationService.currentLocation?.lat,
      lng: this.locationService.currentLocation?.lng
    }).subscribe({
      next: (resp) => {
        const lista = this.extrairListaResposta(resp);
        if (lista.length === 0 && this.paginaAtual === 0) {
          this.carregarComFallback(tipo, termo);
          return;
        }

        this.publicacoesFiltradas = lista;
        if (this.paginaAtual === 0 && lista.length > 0) {
          this.cachePublicacoes = [...lista];
        }
        this.resultadosLocais = [];
        this.totalElementos = typeof resp?.totalElements === 'number' ? resp.totalElements : lista.length;
        this.ultimaPagina = typeof resp?.last === 'boolean' ? resp.last : true;
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.carregarComFallback(tipo, termo);
      }
    });
  }

  private carregarComFiltrosLocais(tipo?: TipoPublicacao): void {
    this.publicacaoService.listar(tipo).subscribe({
      next: (lista) => {
        this.resultadosLocais = this.aplicarFiltrosLocais(lista);
        this.totalElementos = this.resultadosLocais.length;
        this.aplicarPaginacaoLocal();
        this.erro = '';
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.publicacoesFiltradas = [];
        this.resultadosLocais = [];
        this.totalElementos = 0;
        this.ultimaPagina = true;
        this.erro = 'Não foi possível carregar as publicações agora.';
        this.carregando = false;
        this.cdr.markForCheck();
      }
    });
  }

  private aplicarPaginacaoLocal(): void {
    const inicio = this.paginaAtual * this.tamanhoPagina;
    const fim = inicio + this.tamanhoPagina;
    this.publicacoesFiltradas = this.resultadosLocais.slice(inicio, fim);
    this.ultimaPagina = fim >= this.resultadosLocais.length;
  }

  private aplicarFiltrosLocais(lista: PublicacaoServico[]): PublicacaoServico[] {
    const termo = this.normalizar(this.termoBusca);
    const categoria = this.normalizar(this.categoriaSelecionada);
    const subcategoria = this.normalizar(this.subcategoriaSelecionada);
    const localizacao = this.normalizar(this.localizacaoBusca);
    const disponibilidade = this.disponibilidadeFiltro === 'TODOS' ? '' : this.normalizar(this.disponibilidadeFiltro);
    const atendimento = this.atendimentoFiltro === 'TODOS' ? '' : this.normalizar(this.atendimentoFiltro);
    const precoMin = this.normalizarNumero(this.precoMin);
    const precoMax = this.normalizarNumero(this.precoMax);

    return lista.filter((pub) => {
    const cidadeCard = this.getCidadeCard(pub);

      const textoBase = this.normalizar([
        pub.titulo,
        pub.descricao,
        pub.categoria,
        pub.usuarioNome,
        pub.usuarioBairro,
        pub.cidadePublicacao,
        pub.enderecoPublicacao,
        cidadeCard
      ].filter(Boolean).join(' '));

      const localizacaoPublicacao = this.normalizar([
        pub.usuarioBairro,
        pub.cidadePublicacao,
        pub.enderecoPublicacao,
        cidadeCard
      ].filter(Boolean).join(' '));
      const possuiLocalizacaoEstruturada = localizacaoPublicacao.length > 0;

      const tagMatch = this.tagsSelecionadas.length === 0 || this.tagsSelecionadas.some((tag) => textoBase.includes(this.normalizar(tag)));
      const termoMatch = !termo || textoBase.includes(termo);
      const categoriaMatch = !categoria || textoBase.includes(categoria);
      const subcategoriaMatch = !subcategoria || textoBase.includes(subcategoria);
      const localizacaoMatch = !localizacao
        || !possuiLocalizacaoEstruturada
        || localizacaoPublicacao.includes(localizacao)
        || textoBase.includes(localizacao);
      const disponibilidadeMatch = !disponibilidade || textoBase.includes(disponibilidade);
      const atendimentoMatch = !atendimento || textoBase.includes(atendimento);

      const valor = this.extrairValorReferencia(pub);
      const precoMinMatch = precoMin == null || valor >= precoMin;
      const precoMaxMatch = precoMax == null || valor <= precoMax;

      const notaMedia = (pub as PublicacaoServico & { notaMedia?: number; avaliacaoMedia?: number }).notaMedia
        ?? (pub as PublicacaoServico & { notaMedia?: number; avaliacaoMedia?: number }).avaliacaoMedia;
      const avaliacaoMatch = this.avaliacaoMinima <= 0
        || typeof notaMedia !== 'number'
        || notaMedia >= this.avaliacaoMinima;

      return (
        termoMatch &&
        categoriaMatch &&
        subcategoriaMatch &&
        tagMatch &&
        localizacaoMatch &&
        disponibilidadeMatch &&
        atendimentoMatch &&
        precoMinMatch &&
        precoMaxMatch &&
        avaliacaoMatch
      );
    });
  }

  private carregarComFallback(tipo?: TipoPublicacao, termo?: string): void {
    this.publicacaoService.listar(tipo).subscribe({
      next: (lista) => {
        this.cachePublicacoes = [...lista];
        const termoNormalizado = (termo ?? '').trim().toLowerCase();

        if (!termoNormalizado) {
          this.publicacoesFiltradas = lista;
        } else {
          this.publicacoesFiltradas = lista.filter((pub) => {
            const titulo = (pub.titulo ?? '').toLowerCase();
            const descricao = (pub.descricao ?? '').toLowerCase();
            const categoria = (pub.categoria ?? '').toLowerCase();
            const usuarioNome = (pub.usuarioNome ?? '').toLowerCase();
            return (
              titulo.includes(termoNormalizado) ||
              descricao.includes(termoNormalizado) ||
              categoria.includes(termoNormalizado) ||
              usuarioNome.includes(termoNormalizado)
            );
          });
        }

        this.totalElementos = this.publicacoesFiltradas.length;
        this.ultimaPagina = true;
        this.erro = '';
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        if (this.cachePublicacoes.length > 0) {
          this.publicacoesFiltradas = [...this.cachePublicacoes];
          this.totalElementos = this.publicacoesFiltradas.length;
          this.ultimaPagina = true;
          this.erro = 'Exibindo publicações salvas localmente.';
        } else {
          this.publicacoesFiltradas = [];
          this.totalElementos = 0;
          this.ultimaPagina = true;
          this.erro = 'Não foi possível carregar as publicações agora.';
        }
        this.carregando = false;
        this.cdr.markForCheck();
      }
    });
  }

  private extrairListaResposta(resp: unknown): PublicacaoServico[] {
    if (Array.isArray(resp)) {
      return resp as PublicacaoServico[];
    }

    const valor = resp as { content?: PublicacaoServico[]; items?: PublicacaoServico[] } | null;
    if (Array.isArray(valor?.content)) {
      return valor.content;
    }

    if (Array.isArray(valor?.items)) {
      return valor.items;
    }

    return [];
  }

  onTermoBuscaInput(): void {
    if (this.debounceHandle) {
      clearTimeout(this.debounceHandle);
    }
    this.debounceHandle = setTimeout(() => this.buscarPublicacoes(true), 350);
  }

  onCategoriaChange(): void {
    this.subcategoriaSelecionada = '';
    this.buscarPublicacoes(true);
  }

  onSubcategoriaChange(): void {
    this.buscarPublicacoes(true);
  }

  onFiltroAvancadoChange(): void {
    this.buscarPublicacoes(true);
  }

  onLocalizacaoInput(): void {
    if (this.localizacaoDebounceHandle) {
      clearTimeout(this.localizacaoDebounceHandle);
    }
    this.localizacaoDebounceHandle = setTimeout(() => this.buscarPublicacoes(true), 300);
  }

  alternarTag(tag: string): void {
    if (this.tagsSelecionadas.includes(tag)) {
      this.tagsSelecionadas = this.tagsSelecionadas.filter((item) => item !== tag);
    } else {
      this.tagsSelecionadas = [...this.tagsSelecionadas, tag];
    }
    this.buscarPublicacoes(true);
  }

  limparFiltrosAvancados(): void {
    this.categoriaSelecionada = '';
    this.subcategoriaSelecionada = '';
    this.tagsSelecionadas = [];
    this.localizacaoBusca = '';
    this.precoMin = null;
    this.precoMax = null;
    this.avaliacaoMinima = 0;
    this.disponibilidadeFiltro = 'TODOS';
    this.atendimentoFiltro = 'TODOS';
    this.buscarPublicacoes(true);
  }

  usarMinhaLocalizacao(): void {
    this.locationService.requestBrowserLocation()
      .then(() => {
        this.erro = '';
        this.buscarPublicacoes(true);
      })
      .catch((error) => {
        this.erro = error?.message || 'Não foi possível obter sua localização. Informe bairro ou cidade no filtro.';
        this.cdr.markForCheck();
      });
  }

  onFiltroChange(): void {
    this.buscarPublicacoes(true);
  }

  paginaAnterior(): void {
    if (this.paginaAtual === 0 || this.carregando) {
      return;
    }
    this.paginaAtual -= 1;
    if (this.usandoFiltroLocal) {
      this.aplicarPaginacaoLocal();
      this.cdr.markForCheck();
      return;
    }
    this.buscarPublicacoes(false);
  }

  proximaPagina(): void {
    if (this.ultimaPagina || this.carregando) {
      return;
    }
    this.paginaAtual += 1;
    if (this.usandoFiltroLocal) {
      this.aplicarPaginacaoLocal();
      this.cdr.markForCheck();
      return;
    }
    this.buscarPublicacoes(false);
  }

  get subcategoriasDisponiveis(): string[] {
    if (!this.categoriaSelecionada) {
      return [];
    }

    const categoria = this.categoriasServico.find((item) => item.nome === this.categoriaSelecionada);
    return categoria?.subcategorias ?? [];
  }

  private temFiltrosAvancadosAtivos(): boolean {
    const precoMin = this.normalizarNumero(this.precoMin);
    const precoMax = this.normalizarNumero(this.precoMax);

    return Boolean(
      this.categoriaSelecionada ||
      this.subcategoriaSelecionada ||
      this.tagsSelecionadas.length > 0 ||
      this.localizacaoBusca.trim() ||
      precoMin != null ||
      precoMax != null ||
      this.avaliacaoMinima > 0 ||
      this.disponibilidadeFiltro !== 'TODOS' ||
      this.atendimentoFiltro !== 'TODOS'
    );
  }

  private buildTermoBusca(): string {
    const partes = [
      this.termoBusca,
      this.categoriaSelecionada,
      this.subcategoriaSelecionada,
      ...this.tagsSelecionadas,
      this.localizacaoBusca,
      this.disponibilidadeFiltro !== 'TODOS' ? this.disponibilidadeFiltro : '',
      this.atendimentoFiltro !== 'TODOS' ? this.atendimentoFiltro : ''
    ];

    return partes
      .map((parte) => (parte ?? '').trim())
      .filter(Boolean)
      .join(' ');
  }

  private extrairValorReferencia(pub: PublicacaoServico): number {
    if (pub.tipoPublicacao === 'PRESTACAO') {
      return pub.preco ?? 0;
    }

    if (pub.orcamentoMin != null && pub.orcamentoMax != null) {
      return (pub.orcamentoMin + pub.orcamentoMax) / 2;
    }

    return pub.orcamentoMax ?? pub.orcamentoMin ?? 0;
  }

  private normalizar(valor: string): string {
    return valor
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }

  private normalizarNumero(valor: unknown): number | null {
    if (valor == null || valor === '') {
      return null;
    }

    const numero = typeof valor === 'number' ? valor : Number(String(valor).replace(',', '.'));
    return Number.isFinite(numero) ? numero : null;
  }

  trackByPublicacao(_: number, pub: PublicacaoServico): number {
    return pub.id;
  }

  formatarTipo(tipo: TipoPublicacao): string {
    return tipo === 'PRESTACAO' ? 'Prestação de serviço' : 'Contratação de serviço';
  }

  formatarValor(pub: PublicacaoServico): string {
    if (pub.tipoPublicacao === 'PRESTACAO') {
      const preco = pub.preco ?? 0;
      return `R$ ${preco.toFixed(2)}`;
    }

    const min = (pub.orcamentoMin ?? 0).toFixed(2);
    const max = (pub.orcamentoMax ?? 0).toFixed(2);
    return `R$ ${min} - R$ ${max}`;
  }

  formatarDistancia(pub: PublicacaoServico): string {
    if (pub.distanceKm == null) {
      return '';
    }

    return `a ${pub.distanceKm.toLocaleString('pt-BR', {
      minimumFractionDigits: pub.distanceKm < 10 ? 1 : 0,
      maximumFractionDigits: 1
    })} km de você`;
  }

  getEstrelas(nota?: number | null): string {
    const estrelas = Math.max(0, Math.min(5, Math.round(nota ?? 0)));
    return `${'★'.repeat(estrelas)}${'☆'.repeat(5 - estrelas)}`;
  }

  getCidadeCard(pub: PublicacaoServico): string {
    const cidadePublicacao = this.normalizarCidadeExibicao(pub.cidadePublicacao);
    if (cidadePublicacao) {
      return cidadePublicacao;
    }

    const cidadeNormalizada = this.normalizarCidadeExibicao(pub.usuarioCidade);
    if (cidadeNormalizada) {
      return cidadeNormalizada;
    }

    return this.normalizarCidadeExibicao(this.extrairCidadeDoEndereco(pub.usuarioEndereco));
  }

  private normalizarCidadeExibicao(valor?: string): string {
    const cidade = (valor ?? '').trim();
    if (!cidade) {
      return '';
    }

    if (/\d/.test(cidade)) {
      return '';
    }

    if (/^(rua|r\.|avenida|av\.|travessa|tv\.|estrada|rodovia|alameda|ladeira)\b/i.test(cidade)) {
      return '';
    }

    return cidade;
  }

  private extrairCidadeDoEndereco(endereco?: string): string {
    const texto = (endereco ?? '').trim();
    if (!texto) {
      return '';
    }

    const partes = texto
      .split(',')
      .map((parte) => parte.trim())
      .filter(Boolean);

    const parteCidadeUf = partes.find((parte) => /\s-\s[A-Za-z]{2}$/.test(parte));
    if (parteCidadeUf) {
      return parteCidadeUf.replace(/\s-\s[A-Za-z]{2}$/, '').trim();
    }

    const parteCidadeLabel = partes.find((parte) => /^cidade\s*:/i.test(parte));
    if (parteCidadeLabel) {
      return parteCidadeLabel.replace(/^cidade\s*:/i, '').trim();
    }

    return '';
  }
}
