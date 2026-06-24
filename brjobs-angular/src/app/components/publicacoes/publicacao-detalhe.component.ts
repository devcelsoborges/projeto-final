import { ChangeDetectorRef, Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { catchError, of, switchMap } from "rxjs";
import { PublicacaoServico, PublicacaoServicoService } from "../../service/publicacao-servico.service";
import { AuthService } from "../../service/auth.service";
import { HighlightService, HighlightPlan } from "../../service/highlight.service";
import { RatingComponent } from "../rating/rating.component";
import { environment } from "../../environments/environment";

interface UsuarioPerfil {
  id: number;
  nome: string;
  email: string;
  telefone?: string;
  endereco?: string;
  cidade?: string;
  bairro?: string;
  tipoUsuario?: string;
}

interface PrestadorPerfil {
  id: number;
  funcao?: string;
  experienciaProfissional?: string;
  especialidades?: string;
  descricao?: string;
}

interface AvaliacaoItem {
  id: number;
  nota: number;
  comentario?: string;
  dataCriacao?: string;
}

interface AvaliacaoStats {
  media_avaliacao: number;
  total_avaliacoes: number;
}

@Component({
  selector: "app-publicacao-detalhe",
  standalone: true,
  imports: [CommonModule, RouterModule, RatingComponent],
  templateUrl: "./publicacao-detalhe.component.html",
  styleUrls: ["./publicacao-detalhe.component.css"]
})
export class PublicacaoDetalheComponent implements OnInit {
  private readonly apiBase = `${environment.apiUrl}/api`;

  carregando = true;
  erro = "";

  publicacao: PublicacaoServico | null = null;
  usuario: UsuarioPerfil | null = null;
  prestador: PrestadorPerfil | null = null;
  avaliacoes: AvaliacaoItem[] = [];
  stats: AvaliacaoStats = { media_avaliacao: 0, total_avaliacoes: 0 };

  // Destaque (Stripe)
  planosDestaque: HighlightPlan[] = [];
  mostrarPlanos = false;
  carregandoPlanos = false;
  destacando = false;
  erroDestaque = "";

  constructor(
    private readonly route: ActivatedRoute,
    private readonly http: HttpClient,
    private readonly publicacaoService: PublicacaoServicoService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly highlightService: HighlightService
  ) {}

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const id = Number(params.get("id"));
          if (!id) {
            this.erro = "Publicação inválida.";
            this.carregando = false;
            this.cdr.markForCheck();
            return of(null);
          }

          return this.publicacaoService.obterPorId(id).pipe(
            catchError(() => {
              this.erro = "Não foi possível carregar a publicação.";
              this.carregando = false;
              this.cdr.markForCheck();
              return of(null);
            })
          );
        })
      )
      .subscribe((pub) => {
        if (!pub) {
          this.cdr.markForCheck();
          return;
        }

        this.publicacao = pub;
        this.cdr.markForCheck();
        this.carregarPerfilEAvaliacoes(pub.usuarioId);
      });
  }

  private carregarPerfilEAvaliacoes(usuarioId: number): void {
    this.http.get<UsuarioPerfil>(`${this.apiBase}/usuarios/${usuarioId}`)
      .pipe(catchError(() => of(null)))
      .subscribe((usuario) => {
        this.usuario = usuario;
        this.cdr.markForCheck();

        if (!usuario) {
          this.carregando = false;
          this.cdr.markForCheck();
          return;
        }

        this.carregarDadosPrestador(usuarioId);
        this.carregarAvaliacoesUsuario(usuarioId);
      });
  }

  private carregarDadosPrestador(usuarioId: number): void {
    this.http.get<PrestadorPerfil>(`${this.apiBase}/prestadores/usuario/${usuarioId}`)
      .pipe(catchError(() => of(null)))
      .subscribe((prestador) => {
        this.prestador = prestador;
        this.cdr.markForCheck();
      });
  }

  private carregarAvaliacoesUsuario(usuarioId: number): void {
    this.http.get<AvaliacaoItem[]>(`${this.apiBase}/avaliacoes/usuario/${usuarioId}/recebidas`)
      .pipe(catchError(() => of([])))
      .subscribe((avaliacoes) => {
        this.avaliacoes = avaliacoes;
        this.cdr.markForCheck();
      });

    this.http.get<AvaliacaoStats>(`${this.apiBase}/avaliacoes/v1/usuario/${usuarioId}/stats`)
      .pipe(catchError(() => of({ media_avaliacao: 0, total_avaliacoes: 0 })))
      .subscribe((stats) => {
        this.stats = stats;
        this.carregando = false;
        this.cdr.markForCheck();
      });
  }

  podeAvaliar(): boolean {
    return this.authService.isLoggedIn() && !!this.usuario?.id && !this.isProprioPerfil();
  }

  recarregarAvaliacoes(): void {
    if (this.usuario?.id) {
      this.carregarAvaliacoesUsuario(this.usuario.id);
    }
  }

  /** Só o dono LOGADO pode destacar, e só se ainda não estiver destacada. */
  podeDestacar(): boolean {
    return this.authService.isLoggedIn() && this.isProprioPerfil() && !this.publicacao?.isHighlighted;
  }

  abrirDestaque(): void {
    this.erroDestaque = "";
    this.mostrarPlanos = true;
    if (this.planosDestaque.length === 0) {
      this.carregandoPlanos = true;
      this.cdr.markForCheck();
      this.highlightService.listPlans().subscribe({
        next: (planos) => {
          this.planosDestaque = planos;
          this.carregandoPlanos = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.erroDestaque = "Não foi possível carregar os planos de destaque.";
          this.carregandoPlanos = false;
          this.cdr.markForCheck();
        }
      });
    }
  }

  destacar(planId: number): void {
    if (!this.publicacao?.id) {
      return;
    }
    // Vai para o nosso checkout (Payment Element), sem redirecionar para a Stripe.
    this.router.navigate(["/destacar", this.publicacao.id], { queryParams: { plano: planId } });
  }

  getEstrelas(nota?: number | null): string {
    const estrelas = Math.max(0, Math.min(5, Math.round(nota ?? 0)));
    return `${"★".repeat(estrelas)}${"☆".repeat(5 - estrelas)}`;
  }

  formatarValor(publicacao: PublicacaoServico): string {
    if (publicacao.tipoPublicacao === "PRESTACAO") {
      return `R$ ${publicacao.preco?.toFixed(2) ?? "0,00"}`;
    }

    const min = publicacao.orcamentoMin?.toFixed(2) ?? "0,00";
    const max = publicacao.orcamentoMax?.toFixed(2) ?? "0,00";
    return `R$ ${min} - R$ ${max}`;
  }

  formatarTipo(tipo: string): string {
    return tipo === "PRESTACAO" ? "Prestação de serviço" : "Contratação de serviço";
  }

  podeEntrarEmContato(): boolean {
    return !!this.usuario && !this.isProprioPerfil();
  }

  isProprioPerfil(): boolean {
    if (!this.usuario) {
      return false;
    }

    const idAuth = this.authService.getUsuarioAtual()?.id;
    const idStorage = Number(localStorage.getItem("usuario_id") || "0");
    const usuarioLogadoId = idAuth || idStorage;

    if (!usuarioLogadoId) {
      return false;
    }

    return this.usuario.id === usuarioLogadoId;
  }

  getTextoBotaoContato(): string {
    return this.authService.isLoggedIn()
      ? "Entrar em contato por chat"
      : "Entrar para conversar";
  }

  entrarEmContato(): void {
    if (!this.usuario) {
      return;
    }

    if (!this.authService.isLoggedIn()) {
      this.router.navigate(["/login"]);
      return;
    }

    this.router.navigate(["/chat"], {
      queryParams: {
        usuarioId: this.usuario.id,
        nome: this.usuario.nome,
        publicacaoId: this.publicacao?.id
      }
    });
  }

  getCidadeExibicao(): string {
    const cidadeDoUsuario = (this.usuario?.cidade ?? "").trim();
    if (this.isCidadeValida(cidadeDoUsuario)) {
      return cidadeDoUsuario;
    }

    const cidadeDaPublicacao = (this.publicacao?.usuarioCidade ?? "").trim();
    if (this.isCidadeValida(cidadeDaPublicacao)) {
      return cidadeDaPublicacao;
    }

    const cidadeDoEnderecoPublicacao = this.extrairCidadeDoEndereco(this.publicacao?.usuarioEndereco);
    if (this.isCidadeValida(cidadeDoEnderecoPublicacao)) {
      return cidadeDoEnderecoPublicacao;
    }

    const cidadeDoEnderecoUsuario = this.extrairCidadeDoEndereco(this.usuario?.endereco);
    if (this.isCidadeValida(cidadeDoEnderecoUsuario)) {
      return cidadeDoEnderecoUsuario;
    }

    return "";
  }

  private extrairCidadeDoEndereco(endereco?: string): string {
    const texto = (endereco ?? "").trim();
    if (!texto) {
      return "";
    }

    const partes = texto
      .split(",")
      .map((parte) => parte.trim())
      .filter(Boolean);

    const parteCidadeUf = partes.find((parte) => /\s-\s[A-Za-z]{2}$/.test(parte));
    if (parteCidadeUf) {
      return parteCidadeUf.replace(/\s-\s[A-Za-z]{2}$/, "").trim();
    }

    const semCep = partes.filter((parte) => !/^\d{5}-?\d{3}$/.test(parte));
    if (semCep.length === 0) {
      return "";
    }

    const indiceUf = semCep.findIndex((parte) => /^[A-Za-z]{2}$/.test(parte));
    if (indiceUf > 0) {
      return semCep[indiceUf - 1].trim();
    }

    if (semCep.length >= 2) {
      return semCep[semCep.length - 1].trim();
    }

    return "";
  }

  private isCidadeValida(valor: string): boolean {
    const cidade = (valor ?? "").trim();
    if (!cidade) {
      return false;
    }

    if (/\d/.test(cidade)) {
      return false;
    }

    if (/^(rua|r\.|avenida|av\.|travessa|tv\.|estrada|rodovia|alameda|ladeira)\b/i.test(cidade)) {
      return false;
    }

    return true;
  }
}
