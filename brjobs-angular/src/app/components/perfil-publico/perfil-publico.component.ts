import { ChangeDetectorRef, Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { catchError, of, switchMap } from "rxjs";
import { AuthService } from "../../service/auth.service";
import { RatingComponent } from "../rating/rating.component";
import { environment } from "../../environments/environment";

interface UsuarioPerfil {
  id: number;
  nome: string;
  email: string;
  telefone?: string;
  endereco?: string;
  bairro?: string;
  cidade?: string;
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
  selector: "app-perfil-publico",
  standalone: true,
  imports: [CommonModule, RouterModule, RatingComponent],
  templateUrl: "./perfil-publico.component.html",
  styleUrls: ["./perfil-publico.component.css"]
})
export class PerfilPublicoComponent implements OnInit {
  private readonly apiBase = `${environment.apiUrl}/api`;

  carregando = true;
  erro = "";

  usuario: UsuarioPerfil | null = null;
  prestador: PrestadorPerfil | null = null;
  avaliacoes: AvaliacaoItem[] = [];
  stats: AvaliacaoStats = { media_avaliacao: 0, total_avaliacoes: 0 };
  usuarioIdSolicitado = 0;
  fotoIndisponivel = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly http: HttpClient,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const usuarioId = Number(params.get("usuarioId"));
          this.usuarioIdSolicitado = usuarioId;
          if (!usuarioId) {
            this.erro = "Perfil inválido.";
            this.carregando = false;
            this.cdr.markForCheck();
            return of(null);
          }

          return this.http.get<UsuarioPerfil>(`${this.apiBase}/usuarios/${usuarioId}`).pipe(
            catchError(() => {
              if (this.hidratarPerfilLocalSeForProprio(usuarioId)) {
                this.erro = "";
                this.carregando = false;
                this.cdr.markForCheck();
                return of(this.usuario);
              }

              this.erro = "Não foi possível carregar o perfil.";
              this.carregando = false;
              this.cdr.markForCheck();
              return of(null);
            })
          );
        })
      )
      .subscribe((usuario) => {
        if (!usuario) {
          return;
        }

        this.usuario = usuario;
        this.fotoIndisponivel = false;
        this.cdr.markForCheck();
        this.carregarDadosPrestador(usuario.id);
      });
  }

  private carregarDadosPrestador(usuarioId: number): void {
    this.http.get<PrestadorPerfil>(`${this.apiBase}/prestadores/usuario/${usuarioId}`)
      .pipe(catchError(() => of(null)))
      .subscribe((prestador) => {
        this.prestador = prestador;
        this.cdr.markForCheck();

        this.carregarAvaliacoesUsuario(usuarioId);
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

  private carregarAvaliacoes(prestadorId: number): void {
    this.http.get<AvaliacaoItem[]>(`${this.apiBase}/avaliacoes/prestador/${prestadorId}`)
      .pipe(catchError(() => of([])))
      .subscribe((avaliacoes) => {
        this.avaliacoes = avaliacoes;
        this.cdr.markForCheck();
      });

    this.http.get<AvaliacaoStats>(`${this.apiBase}/avaliacoes/v1/prestador/${prestadorId}/stats`)
      .pipe(catchError(() => of({ media_avaliacao: 0, total_avaliacoes: 0 })))
      .subscribe((stats) => {
        this.stats = stats;
        this.carregando = false;
        this.cdr.markForCheck();
      });
  }

  getEstrelas(nota?: number | null): string {
    const estrelas = Math.max(0, Math.min(5, Math.round(nota ?? 0)));
    return `${"★".repeat(estrelas)}${"☆".repeat(5 - estrelas)}`;
  }

  /** URL da foto do usuário (o endpoint serve o .webp; cai no placeholder se 404). */
  get fotoUrl(): string {
    return this.usuario ? `${this.apiBase}/usuarios/${this.usuario.id}/foto` : "";
  }

  onFotoErro(): void {
    this.fotoIndisponivel = true;
    this.cdr.markForCheck();
  }

  podeEntrarEmContato(): boolean {
    return !!this.usuario && !this.isProprioPerfil();
  }

  podeIrParaMeuPerfil(): boolean {
    return !!this.usuario && this.isProprioPerfil();
  }

  podeAvaliar(): boolean {
    return this.authService.isLoggedIn() && !!this.usuario?.id && !this.isProprioPerfil();
  }

  recarregarAvaliacoes(): void {
    if (this.usuario?.id) {
      this.carregarAvaliacoesUsuario(this.usuario.id);
    }
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
        nome: this.usuario.nome
      }
    });
  }

  irParaMeuPerfil(): void {
    this.router.navigate(["/profile"]);
  }

  private hidratarPerfilLocalSeForProprio(usuarioId: number): boolean {
    const idStorage = Number(localStorage.getItem("usuario_id") || "0");
    const idAuth = this.authService.getUsuarioAtual()?.id || 0;
    const idLogado = idAuth || idStorage;

    if (!idLogado || idLogado !== usuarioId) {
      return false;
    }

    const nome = localStorage.getItem("usuario_nome") || "Usuário";
    const email = localStorage.getItem("usuario_email") || "";
    const telefone = localStorage.getItem("usuario_telefone") || "";
    const endereco = localStorage.getItem("usuario_endereco") || "";

    this.usuario = {
      id: idLogado,
      nome,
      email,
      telefone,
      endereco
    };

    return true;
  }

  getCidadeExibicao(): string {
    const cidade = this.normalizarCidadeExibicao(this.usuario?.cidade);
    if (cidade) {
      return cidade;
    }

    return this.normalizarCidadeExibicao(this.extrairCidadeDoEndereco(this.usuario?.endereco));
  }

  private normalizarCidadeExibicao(valor?: string): string {
    const cidade = (valor ?? "").trim();
    if (!cidade) {
      return "";
    }

    if (/\d/.test(cidade)) {
      return "";
    }

    if (/^(rua|r\.|avenida|av\.|travessa|tv\.|estrada|rodovia|alameda|ladeira)\b/i.test(cidade)) {
      return "";
    }

    return cidade;
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

    const parteCidadeLabel = partes.find((parte) => /^cidade\s*:/i.test(parte));
    if (parteCidadeLabel) {
      return parteCidadeLabel.replace(/^cidade\s*:/i, "").trim();
    }

    return "";
  }
}
