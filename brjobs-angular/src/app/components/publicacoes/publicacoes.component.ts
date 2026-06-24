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
import { LocationService } from "../../service/location.service";
import { AccountService } from "../../service/account.service";

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
  erroCarregamento = "";
  sucesso = "";
  paginaAtual = 0;
  readonly tamanhoPagina = 20;
  ultimaPagina = true;

  publicacoes: PublicacaoServico[] = [];
  usuarioLogado = false;
  usuarioLogadoId: number | null = null;
  subcategoriaSelecionada = "";
  removendoPublicacaoId: number | null = null;
  // R3: publicação bloqueada por e-mail não confirmado — oferece reenvio.
  mostrarReenvioConfirmacao = false;
  reenvioMensagem = "";

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
    enderecoPublicacao: "",
    cepPublicacao: "",
    cidadePublicacao: "",
    estadoPublicacao: "",
    latitude: undefined,
    longitude: undefined,
    geocodeProvider: undefined,
    geocodePrecision: undefined,
    preco: undefined,
    orcamentoMin: undefined,
    orcamentoMax: undefined
  };

  constructor(
    private readonly publicacaoService: PublicacaoServicoService,
    private readonly authService: AuthService,
    private readonly locationService: LocationService,
    private readonly accountService: AccountService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.usuarioLogado = this.authService.isLoggedIn();
    this.atualizarUsuarioLogadoId();
    this.authSubscription = this.authService.isLoggedIn$.subscribe((logged) => {
      this.usuarioLogado = logged;
      this.atualizarUsuarioLogadoId();
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
    this.erroCarregamento = "";

    const tipo = this.tipoFiltro === "TODAS" ? undefined : this.tipoFiltro;
    this.publicacaoService.buscarPaginado({
      tipo,
      page: this.paginaAtual,
      size: this.tamanhoPagina,
      lat: this.locationService.currentLocation?.lat,
      lng: this.locationService.currentLocation?.lng
    }).subscribe({
      next: (resp) => {
        const payload = resp as unknown as { content?: PublicacaoServico[]; last?: boolean } | PublicacaoServico[];

        if (Array.isArray(payload)) {
          this.aplicarPaginacaoLocal(payload);
        } else {
          this.publicacoes = payload.content ?? [];
          this.ultimaPagina = payload.last ?? true;
        }

        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        // Fallback para compatibilidade com APIs sem endpoint paginado.
        this.publicacaoService.listar(tipo).subscribe({
          next: (lista) => {
            this.aplicarPaginacaoLocal(lista ?? []);
            this.carregando = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.erroCarregamento = "Não foi possível carregar as publicações.";
            this.publicacoes = [];
            this.ultimaPagina = true;
            this.carregando = false;
            this.cdr.markForCheck();
          }
        });
      }
    });
  }

  private aplicarPaginacaoLocal(lista: PublicacaoServico[]): void {
    const start = this.paginaAtual * this.tamanhoPagina;
    const end = start + this.tamanhoPagina;
    this.publicacoes = lista.slice(start, end);
    this.ultimaPagina = end >= lista.length;
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
    this.mostrarReenvioConfirmacao = false;
    this.reenvioMensagem = "";

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

    if (!this.form.enderecoPublicacao?.trim()) {
      this.erro = "Informe o endereço da publicação.";
      this.cdr.markForCheck();
      return;
    }

    const payload: CriarPublicacaoServicoDTO = {
      ...this.form,
      categoria: (this.form.categoria || "").trim() || undefined,
      subcategoria: (this.subcategoriaSelecionada || "").trim() || undefined,
      titulo: this.form.titulo.trim(),
      descricao: this.form.descricao.trim(),
      enderecoPublicacao: this.form.enderecoPublicacao.trim(),
      cepPublicacao: this.form.cepPublicacao?.trim() || undefined,
      cidadePublicacao: this.form.cidadePublicacao?.trim() || undefined,
      estadoPublicacao: this.form.estadoPublicacao?.trim().toUpperCase() || undefined
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
          enderecoPublicacao: "",
          cepPublicacao: "",
          cidadePublicacao: "",
          estadoPublicacao: "",
          latitude: undefined,
          longitude: undefined,
          geocodeProvider: undefined,
          geocodePrecision: undefined,
          preco: undefined,
          orcamentoMin: undefined,
          orcamentoMax: undefined
        };
        this.subcategoriaSelecionada = "";
        this.cdr.markForCheck();
        this.carregarPublicacoes();
      },
      error: (err) => {
        // R3: e-mail não confirmado -> mensagem clara + opção de reenviar a confirmação.
        if (err?.status === 403 && err?.error?.code === "EMAIL_NOT_CONFIRMED") {
          this.erro = err.error.message || "Confirme seu e-mail para publicar.";
          this.mostrarReenvioConfirmacao = true;
        } else {
          this.erro = (typeof err?.error === "string" ? err.error : err?.error?.message) || "Falha ao criar publicação.";
        }
        this.cdr.markForCheck();
      }
    });
  }

  /** Reenvia o e-mail de confirmação para o usuário logado (bloqueado de publicar). */
  reenviarConfirmacaoEmail(): void {
    const email = this.authService.getUsuarioAtual()?.email || localStorage.getItem("usuario_email");
    if (!email) {
      return;
    }
    this.reenvioMensagem = "";
    this.accountService.reenviarConfirmacao(email).subscribe({
      next: (res) => {
        this.reenvioMensagem = res?.message || "E-mail de confirmação reenviado. Verifique sua caixa de entrada (e o spam).";
        this.cdr.markForCheck();
      },
      error: () => {
        this.reenvioMensagem = "Não foi possível reenviar agora. Tente novamente em instantes.";
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

  formatarDistancia(publicacao: PublicacaoServico): string {
    if (publicacao.distanceKm == null) {
      return "";
    }

    return `a ${publicacao.distanceKm.toLocaleString("pt-BR", {
      minimumFractionDigits: publicacao.distanceKm < 10 ? 1 : 0,
      maximumFractionDigits: 1
    })} km de você`;
  }

  usarLocalizacaoAtual(): void {
    this.erro = "";
    this.locationService.requestBrowserLocation()
      .then((location) => {
        this.form.latitude = location.lat;
        this.form.longitude = location.lng;
        this.form.geocodeProvider = "browser";
        this.form.geocodePrecision = "exact";
        this.sucesso = "Localização atual adicionada à publicação.";
        this.carregarPublicacoes(true);
        this.cdr.markForCheck();
      })
      .catch((error) => {
        this.erro = error?.message || "Não foi possível obter sua localização. Preencha o endereço manualmente.";
        this.cdr.markForCheck();
      });
  }

  podeRemoverPublicacao(publicacao: PublicacaoServico): boolean {
    return this.usuarioLogado && this.usuarioLogadoId != null && publicacao.usuarioId === this.usuarioLogadoId;
  }

  removerPublicacao(publicacao: PublicacaoServico): void {
    this.erro = "";
    this.sucesso = "";

    if (!this.podeRemoverPublicacao(publicacao)) {
      this.erro = "Você não tem permissão para remover esta publicação.";
      this.cdr.markForCheck();
      return;
    }

    const confirmou = window.confirm("Tem certeza que deseja remover esta publicação?");
    if (!confirmou) {
      return;
    }

    this.removendoPublicacaoId = publicacao.id;

    this.publicacaoService.encerrar(publicacao.id).subscribe({
      next: () => {
        this.sucesso = "Publicação removida com sucesso.";
        this.removendoPublicacaoId = null;
        this.carregarPublicacoes(false);
      },
      error: (err) => {
        if (err?.status === 403) {
          this.erro = "Apenas o usuário que publicou pode remover a publicação.";
        } else if (typeof err?.error === "string") {
          this.erro = err.error;
        } else {
          this.erro = "Falha ao remover publicação.";
        }
        this.removendoPublicacaoId = null;
        this.cdr.markForCheck();
      }
    });
  }

  private atualizarUsuarioLogadoId(): void {
    if (!this.usuarioLogado) {
      this.usuarioLogadoId = null;
      return;
    }

    const idAuth = this.authService.getUsuarioAtual()?.id;
    const idStorage = Number(localStorage.getItem("usuario_id") || "0");
    const resolvedId = idAuth || idStorage;
    this.usuarioLogadoId = resolvedId > 0 ? resolvedId : null;
  }
}
