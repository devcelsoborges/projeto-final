import { ChangeDetectorRef, Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { HttpClient } from "@angular/common/http";
import { catchError, of, switchMap } from "rxjs";
import { PublicacaoServico, PublicacaoServicoService } from "../../service/publicacao-servico.service";
import { AuthService } from "../../service/auth.service";

interface UsuarioPerfil {
  id: number;
  nome: string;
  email: string;
  telefone?: string;
  endereco?: string;
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
  imports: [CommonModule, RouterModule],
  templateUrl: "./publicacao-detalhe.component.html",
  styleUrls: ["./publicacao-detalhe.component.css"]
})
export class PublicacaoDetalheComponent implements OnInit {
  private readonly apiBase = "http://localhost:8080/api";

  carregando = true;
  erro = "";

  publicacao: PublicacaoServico | null = null;
  usuario: UsuarioPerfil | null = null;
  prestador: PrestadorPerfil | null = null;
  avaliacoes: AvaliacaoItem[] = [];
  stats: AvaliacaoStats = { media_avaliacao: 0, total_avaliacoes: 0 };

  constructor(
    private readonly route: ActivatedRoute,
    private readonly http: HttpClient,
    private readonly publicacaoService: PublicacaoServicoService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
    private readonly authService: AuthService
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
      .pipe(
        catchError(() => of(null))
      )
      .subscribe((usuario) => {
        this.usuario = usuario;
        this.cdr.markForCheck();

        if (!usuario) {
          this.carregando = false;
          this.cdr.markForCheck();
          return;
        }

        this.carregarDadosPrestador(usuarioId);
      });
  }

  private carregarDadosPrestador(usuarioId: number): void {
    this.http.get<PrestadorPerfil>(`${this.apiBase}/prestadores/usuario/${usuarioId}`)
      .pipe(catchError(() => of(null)))
      .subscribe((prestador) => {
        this.prestador = prestador;
        this.cdr.markForCheck();

        if (prestador?.id) {
          this.carregarAvaliacoes(prestador.id);
        } else {
          this.carregando = false;
          this.cdr.markForCheck();
        }
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
}
