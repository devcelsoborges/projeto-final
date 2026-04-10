import { ChangeDetectorRef, Component, OnDestroy, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { Subscription } from "rxjs";
import {
  CriarPublicacaoServicoDTO,
  PublicacaoServico,
  PublicacaoServicoService,
  TipoPublicacao
} from "../../service/publicacao-servico.service";
import { AuthService } from "../../service/auth.service";

interface CategoriaServico {
  nome: string;
  subcategorias: string[];
}

@Component({
  selector: "app-publicacoes",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: "./publicacoes.component.html",
  styleUrls: ["./publicacoes.component.css"]
})
export class PublicacoesComponent implements OnInit, OnDestroy {
  tipoFiltro: "TODAS" | TipoPublicacao = "TODAS";
  carregando = false;
  erro = "";
  sucesso = "";
  paginaAtual = 0;
  readonly tamanhoPagina = 10;
  ultimaPagina = true;

  publicacoes: PublicacaoServico[] = [];
  usuarioLogado = false;
  subcategoriaSelecionada = "";

  readonly categoriasServico: CategoriaServico[] = [
    {
      nome: "Reformas e Construção",
      subcategorias: ["Pedreiro", "Reforma em geral", "Assentamento de piso/porcelanato", "Gesso / Drywall", "Impermeabilização"]
    },
    {
      nome: "Elétrica",
      subcategorias: ["Instalação elétrica", "Manutenção", "Curto / emergência", "Instalação de chuveiro", "Iluminação"]
    },
    {
      nome: "Hidráulica",
      subcategorias: ["Encanador", "Vazamentos", "Instalação de torneiras", "Desentupimento", "Caixa d'água"]
    },
    {
      nome: "Marcenaria e Móveis",
      subcategorias: ["Móveis planejados", "Montagem de móveis", "Reparos", "Carpintaria"]
    },
    {
      nome: "Pintura e Acabamento",
      subcategorias: ["Pintura residencial", "Pintura comercial", "Textura / grafiato", "Retoques"]
    },
    {
      nome: "Limpeza e Serviços Domésticos",
      subcategorias: ["Faxina", "Diarista", "Limpeza pós-obra", "Limpeza pesada", "Passadeira"]
    },
    {
      nome: "Marido de Aluguel",
      subcategorias: ["Pequenos reparos", "Instalações", "Serviços rápidos"]
    },
    {
      nome: "Climatização",
      subcategorias: ["Ar-condicionado (instalação)", "Manutenção", "Higienização"]
    },
    {
      nome: "Assistência Técnica",
      subcategorias: ["Geladeira", "Máquina de lavar", "TV", "Micro-ondas", "Computadores"]
    },
    {
      nome: "Automotivo",
      subcategorias: ["Mecânico", "Elétrica automotiva", "Socorro / emergência"]
    },
    {
      nome: "Eventos",
      subcategorias: ["Organização de eventos", "Garçom", "Buffet", "Decoração", "Som e iluminação"]
    },
    {
      nome: "Jardim e Exterior",
      subcategorias: ["Jardinagem", "Limpeza de terreno", "Corte de grama", "Paisagismo"]
    },
    {
      nome: "Segurança e Instalações",
      subcategorias: ["Câmeras (CFTV)", "Alarmes", "Cerca elétrica", "Interfone"]
    }
  ];

  private authSubscription?: Subscription;

  form: CriarPublicacaoServicoDTO = {
    tipoPublicacao: "PRESTACAO",
    titulo: "",
    descricao: "",
    categoria: "",
    preco: undefined,
    orcamentoMin: undefined,
    orcamentoMax: undefined
  };

  constructor(
    private readonly publicacaoService: PublicacaoServicoService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.usuarioLogado = this.authService.isLoggedIn();
    this.authSubscription = this.authService.isLoggedIn$.subscribe((logged) => {
      this.usuarioLogado = logged;
      this.cdr.markForCheck();
    });

    this.carregarPublicacoes(true);
  }

  ngOnDestroy(): void {
    this.authSubscription?.unsubscribe();
  }

  carregarPublicacoes(resetPage = false): void {
    if (resetPage) {
      this.paginaAtual = 0;
    }

    this.carregando = true;
    this.erro = "";

    const tipo = this.tipoFiltro === "TODAS" ? undefined : this.tipoFiltro;
    this.publicacaoService.buscarPaginado({
      tipo,
      page: this.paginaAtual,
      size: this.tamanhoPagina
    }).subscribe({
      next: (resp) => {
        this.publicacoes = resp.content;
        this.ultimaPagina = resp.last;
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.erro = "Não foi possível carregar as publicações.";
        this.carregando = false;
        this.cdr.markForCheck();
      }
    });
  }

  get subcategoriasDisponiveis(): string[] {
    if (!this.form.categoria) {
      return [];
    }

    const categoria = this.categoriasServico.find((item) => item.nome === this.form.categoria);
    return categoria?.subcategorias ?? [];
  }

  onCategoriaChange(): void {
    this.subcategoriaSelecionada = "";
  }

  restringirCampoMonetario(event: KeyboardEvent): void {
    const teclasPermitidas = [
      "Backspace", "Tab", "Delete", "ArrowLeft", "ArrowRight", "Home", "End", "Enter"
    ];

    if (teclasPermitidas.includes(event.key) || event.ctrlKey || event.metaKey) {
      return;
    }

    if (event.key === "e" || event.key === "E" || event.key === "+" || event.key === "-") {
      event.preventDefault();
      return;
    }

    const isNumero = /^[0-9]$/.test(event.key);
    const isSeparador = event.key === "." || event.key === ",";

    if (!isNumero && !isSeparador) {
      event.preventDefault();
    }
  }

  trocarTipoFiltro(valor: "TODAS" | TipoPublicacao): void {
    this.tipoFiltro = valor;
    this.carregarPublicacoes(true);
  }

  paginaAnterior(): void {
    if (this.paginaAtual === 0 || this.carregando) {
      return;
    }
    this.paginaAtual -= 1;
    this.carregarPublicacoes(false);
  }

  proximaPagina(): void {
    if (this.ultimaPagina || this.carregando) {
      return;
    }
    this.paginaAtual += 1;
    this.carregarPublicacoes(false);
  }

  criarPublicacao(): void {
    this.erro = "";
    this.sucesso = "";

    if (!this.usuarioLogado) {
      this.erro = "Você precisa estar logado para publicar um serviço.";
      this.cdr.markForCheck();
      return;
    }

    if (!this.form.titulo.trim() || !this.form.descricao.trim()) {
      this.erro = "Preencha título e descrição.";
      this.cdr.markForCheck();
      return;
    }

    const payload: CriarPublicacaoServicoDTO = {
      ...this.form,
      categoria: (this.subcategoriaSelecionada || this.form.categoria || "").trim() || undefined,
      titulo: this.form.titulo.trim(),
      descricao: this.form.descricao.trim()
    };

    if (payload.tipoPublicacao === "PRESTACAO") {
      if (payload.preco == null || Number.isNaN(payload.preco) || payload.preco <= 0) {
        this.erro = "Informe um preço válido maior que zero.";
        this.cdr.markForCheck();
        return;
      }
      payload.orcamentoMin = undefined;
      payload.orcamentoMax = undefined;
    } else {
      if (
        payload.orcamentoMin == null ||
        payload.orcamentoMax == null ||
        Number.isNaN(payload.orcamentoMin) ||
        Number.isNaN(payload.orcamentoMax) ||
        payload.orcamentoMin < 0 ||
        payload.orcamentoMax <= 0 ||
        payload.orcamentoMax < payload.orcamentoMin
      ) {
        this.erro = "Informe um orçamento válido (máximo maior ou igual ao mínimo).";
        this.cdr.markForCheck();
        return;
      }
      payload.preco = undefined;
    }

    this.publicacaoService.criar(payload).subscribe({
      next: () => {
        this.sucesso = "Publicação criada com sucesso.";
        this.form = {
          tipoPublicacao: this.form.tipoPublicacao,
          titulo: "",
          descricao: "",
          categoria: "",
          preco: undefined,
          orcamentoMin: undefined,
          orcamentoMax: undefined
        };
        this.subcategoriaSelecionada = "";
        this.cdr.markForCheck();
        this.carregarPublicacoes();
      },
      error: (err) => {
        this.erro = err?.error || "Falha ao criar publicação.";
        this.cdr.markForCheck();
      }
    });
  }

  formatarValor(publicacao: PublicacaoServico): string {
    if (publicacao.tipoPublicacao === "PRESTACAO") {
      return `R$ ${publicacao.preco?.toFixed(2) ?? "0,00"}`;
    }

    const min = publicacao.orcamentoMin?.toFixed(2) ?? "0,00";
    const max = publicacao.orcamentoMax?.toFixed(2) ?? "0,00";
    return `R$ ${min} - R$ ${max}`;
  }
}
