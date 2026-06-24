import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AccountService } from '../../service/account.service';

@Component({
  selector: 'app-confirmar-email',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './confirmar-email.component.html',
  styleUrls: ['./confirmar-email.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmarEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly accountService = inject(AccountService);
  private readonly cdr = inject(ChangeDetectorRef);

  status: 'loading' | 'success' | 'error' = 'loading';
  mensagem = 'Confirmando seu cadastro...';

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.status = 'error';
      this.mensagem = 'Link de confirmação inválido.';
      return;
    }

    this.accountService.confirmarEmail(token).subscribe({
      next: (res) => {
        this.status = 'success';
        this.mensagem = res.message || 'Cadastro confirmado com sucesso!';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.status = 'error';
        this.mensagem = err?.error?.message || 'Link de confirmação inválido ou expirado.';
        this.cdr.markForCheck();
      },
    });
  }

  irParaLogin(): void {
    this.router.navigate(['/login']);
  }
}
