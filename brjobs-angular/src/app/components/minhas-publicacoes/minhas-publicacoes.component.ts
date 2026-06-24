import { ChangeDetectorRef, Component, OnDestroy, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, RouterModule } from "@angular/router";
import { Subscription } from "rxjs";
import {
  PublicacaoServico,
  PublicacaoServicoService
} from "../../service/publicacao-servico.service";
import { AuthService } from "../../service/auth.service";
import { HighlightPlan, HighlightService } from "../../service/highlight.service";

@Component({
  selector: "app-minhas-publicacoes",
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: "./minhas-publicacoes.component.html",
  styleUrls: ["./minhas-publicacoes.component.css"]
})
export class MinhasPublicacoesComponent implements OnInit, OnDestroy {
  publicacoes: PublicacaoServico[] = [];
  planos: HighlightPlan[] = [];
  carregando = false;
  carregandoPlanos = false;
  erroCarregamento = "";
  erroPlanos = "";
  usuarioLogado = false;
  usuarioLogadoId: number | null = null;
  removendoPublicacaoId: number | null = null;
  destacandoPublicacaoId: number | null = null;
  publicacaoExpandidaId: number | null = null;
  paginaAtual = 0;
  readonly tamanhoPagina = 10;
  ultimaPagina = true;

  private authSubscription?: Subscription;

  constructor(
    private readonly publicacaoService: PublicacaoServicoService,
    private readonly highlightService: HighlightService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.usuarioLogado = this.authService.isLoggedIn();
    this.atualizarUsuarioLogadoId();

    this.authSubscription = this.authService.isLoggedIn$.subscribe((logged) => {
      this.usuarioLogado = logged;
      this.atualizarUsuarioLogadoId();
      if (logged) {
        this.carregarPlanos();
        this.carregarPublicacoes(true);
      }
      this.cdr.markForCheck();
    });

    if (this.usuarioLogado) {
      this.carregarPlanos();
      this.carregarPublicacoes(true);
    }
  }

  ngOnDestroy(): void {
    this.authSubscription?.unsubscribe();
  }

  carregarPublicacoes(resetPage = false): void {
    if (resetPage) {
      this.paginaAtual = 0;
    }

    if (!this.usuarioLogadoId) {
      this.erroCarregamento = "Usuário não identificado.";
      return;
    }

    this.carregando = true;
    this.erroCarregamento = "";

    this.publicacaoService.listarMinhas().subscribe({
      next: (lista) => {
        this.aplicarPaginacaoLocal(lista ?? []);
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.erroCarregamento = "Não foi possível carregar suas publicações.";
        this.publicacoes = [];
        this.ultimaPagina = true;
        this.carregando = false;
        this.cdr.markForCheck();
      }
    });
  }

  carregarPlanos(): void {
    this.carregandoPlanos = true;
    this.erroPlanos = "";

    this.highlightService.listPlans().subscribe({
      next: (planos) => {
        this.planos = planos ?? [];
        this.carregandoPlanos = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.planos = [];
        this.carregandoPlanos = false;
        this.erroPlanos = "Não foi possível carregar os planos de destaque.";
        this.cdr.markForCheck();
      }
    });
  }

  private aplicarPaginacaoLocal(lista: PublicacaoServico[]): void {
    const start = this.paginaAtual * this.tamanhoPagina;
    const end = start + this.tamanhoPagina;
    this.publicacoes = lista.slice(start, end);
    this.ultimaPagina = end >= lista.length;
  }

  proximaPagina(): void {
    if (!this.ultimaPagina) {
      this.paginaAtual++;
      this.carregarPublicacoes(false);
    }
  }

  paginaAnterior(): void {
    if (this.paginaAtual > 0) {
      this.paginaAtual--;
      this.carregarPublicacoes(false);
    }
  }

  podeRemoverPublicacao(publicacao: PublicacaoServico): boolean {
    return this.usuarioLogado && this.usuarioLogadoId != null && publicacao.usuarioId === this.usuarioLogadoId;
  }

  removerPublicacao(publicacao: PublicacaoServico): void {
    if (!this.podeRemoverPublicacao(publicacao)) {
      return;
    }

    const confirmacao = confirm(`Tem certeza que deseja remover a publicação "${publicacao.titulo}"?`);
    if (!confirmacao) {
      return;
    }

    this.removendoPublicacaoId = publicacao.id;

    this.publicacaoService.encerrar(publicacao.id).subscribe({
      next: () => {
        this.removendoPublicacaoId = null;
        this.carregarPublicacoes(this.publicacoes.length === 1 && this.paginaAtual > 0);
      },
      error: (err) => {
        this.removendoPublicacaoId = null;

        if (err?.status === 403) {
          alert("Você não tem permissão para remover esta publicação.");
        } else {
          alert("Erro ao remover a publicação. Tente novamente.");
        }
      }
    });
  }

  togglePlanos(publicacaoId: number): void {
    this.publicacaoExpandidaId = this.publicacaoExpandidaId === publicacaoId ? null : publicacaoId;
  }

  destacarPublicacao(publicacao: PublicacaoServico, plano: HighlightPlan): void {
    // Vai para o nosso checkout (Payment Element), sem redirecionar para a Stripe.
    this.router.navigate(["/destacar", publicacao.id], { queryParams: { plano: plano.id } });
  }

  isPublicacaoDestacada(publicacao: PublicacaoServico): boolean {
    return !!publicacao.isHighlighted
      && !!publicacao.highlightExpiresAt
      && new Date(publicacao.highlightExpiresAt).getTime() > Date.now();
  }

  formatarDataDestaque(valor?: string): string {
    if (!valor) {
      return "";
    }

    const data = new Date(valor);
    return Number.isNaN(data.getTime()) ? "" : data.toLocaleDateString("pt-BR");
  }

  private atualizarUsuarioLogadoId(): void {
    const usuario = this.authService.getUsuarioAtual();
    if (usuario?.id) {
      this.usuarioLogadoId = usuario.id;
    } else {
      const storedUserId = localStorage.getItem("usuario_id");
      this.usuarioLogadoId = storedUserId ? parseInt(storedUserId, 10) : null;
    }
  }
}
