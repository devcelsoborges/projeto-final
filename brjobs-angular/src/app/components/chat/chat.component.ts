import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ChatService, ChatMessage, Conversa } from '../../service/chat.service';
import { AuthService } from '../../service/auth.service';
import { ChatUnreadService } from '../../service/chat-unread.service';
import { UxTelemetryService } from '../../service/ux-telemetry.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="chat-container">
      <div class="conversas-list">
        <h3>Conversas</h3>
        <div class="nao-lidas-badge" *ngIf="naoLidas > 0">{{ naoLidas }} novas</div>
        <div *ngIf="isLoadingConversas" class="loading-inline">Carregando conversas...</div>
        
        <div *ngIf="conversas.length === 0" class="no-conversas">
          Nenhuma conversa ativa
        </div>
        
        <div *ngFor="let conversa of conversas" 
             class="conversa-item"
             [class.ativa]="conversaSelecionada?.id === conversa.id"
             (click)="selecionarConversa(conversa)">
          <div class="conversa-header">
            <h5>{{ conversa.contatoNome }}</h5>
            <span class="data">{{ conversa.atualizadaEm | date:'short' }}</span>
          </div>
          <p *ngIf="conversa.ultimaMensagem" class="ultima-msg">
            {{ conversa.ultimaMensagem | slice:0:40 }}
          </p>
          <small *ngIf="conversa.naoLidas > 0" class="conversation-unread">{{ conversa.naoLidas }} não lida(s)</small>
        </div>
      </div>

      <div class="chat-area">
        <div *ngIf="!conversaSelecionada" class="select-conversa">
          Selecione uma conversa para começar
        </div>

        <div *ngIf="conversaSelecionada" class="conversa-chat">
          <div class="chat-header">
            <h4>Conversa com {{ conversaSelecionada.contatoNome }}</h4>
            <button class="btn-close" (click)="fecharConversa()">×</button>
          </div>

          <div class="mensagens">
            <div *ngIf="isLoadingMensagens" class="loading-inline">Atualizando mensagens...</div>
            <div *ngFor="let msg of mensagensConversa" 
                 class="mensagem"
                 [class.enviada]="msg.remetenteId === usuarioLogado">
              <div class="msg-content">
                <strong>{{ msg.remetenteName }}</strong>
                <p>{{ msg.conteudo }}</p>
                <small>{{ msg.criadoEm | date:'short' }}</small>
              </div>
            </div>
          </div>

          <div class="msg-input">
            <input type="text" 
                   [(ngModel)]="novaMsg"
                   (keyup.enter)="enviarMensagem()"
                   [maxlength]="maxMessageLength"
                   placeholder="Digite sua mensagem..."
                   class="form-control" />
            <button (click)="enviarMensagem()" class="btn btn-primary" [disabled]="isSending">Enviar</button>
          </div>
          <div class="input-meta">
            <small class="char-count">{{ (novaMsg || '').length }}/{{ maxMessageLength }}</small>
            <small class="error-text" *ngIf="mensagemErro">{{ mensagemErro }}</small>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .chat-container {
      display: flex;
      height: 600px;
      border: 1px solid var(--color-border);
      border-radius: 8px;
      overflow: hidden;
      background: var(--color-surface);
      box-shadow: 0 2px 8px color-mix(in srgb, var(--color-text) 10%, transparent);
    }
    .conversas-list {
      flex: 0 0 250px;
      border-right: 1px solid var(--color-border);
      overflow-y: auto;
      padding: 15px;
      background: var(--color-surface-muted);
    }
    .conversas-list h3 {
      margin: 0 0 15px;
      color: var(--color-text);
      font-size: 16px;
    }
    .nao-lidas-badge {
      display: inline-block;
      background: var(--color-danger);
      color: var(--color-on-danger, #fff);
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
      margin-bottom: 10px;
    }
    .conversa-item {
      padding: 10px;
      border-radius: 6px;
      cursor: pointer;
      margin-bottom: 5px;
      border-left: 3px solid transparent;
      background: var(--color-surface);
      transition: all 0.2s ease;
    }
    .conversa-item:hover {
      background: var(--color-surface-muted);
      border-left-color: var(--color-primary);
    }
    .conversa-item.ativa {
      background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface) 88%);
      border-left-color: var(--color-primary);
      box-shadow: 0 2px 4px color-mix(in srgb, var(--color-primary) 18%, transparent);
    }
    .conversa-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 5px;
    }
    .conversa-header h5 {
      margin: 0;
      font-size: 14px;
      color: var(--color-text);
      font-weight: 600;
    }
    .data {
      font-size: 11px;
      color: var(--color-text-muted);
    }
    .loading-inline {
      font-size: 12px;
      color: var(--color-text-muted);
      margin-bottom: 10px;
    }
    .ultima-msg {
      margin: 5px 0 0;
      font-size: 12px;
      color: var(--color-text-muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .conversation-unread {
      color: var(--color-primary);
      font-weight: 600;
      font-size: 11px;
    }
    .no-conversas {
      text-align: center;
      color: var(--color-text-muted);
      padding: 20px;
    }
    .chat-area {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    .select-conversa {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--color-text-muted);
    }
    .conversa-chat {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    .chat-header {
      padding: 15px;
      border-bottom: 1px solid var(--color-border);
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: var(--color-surface);
    }
    .chat-header h4 {
      margin: 0;
      color: var(--color-text);
    }
    .btn-close {
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: var(--color-text-muted);
    }
    .mensagens {
      flex: 1;
      overflow-y: auto;
      padding: 15px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      background: var(--color-surface-muted);
    }
    .mensagem {
      display: flex;
      margin-bottom: 10px;
    }
    .mensagem.enviada {
      justify-content: flex-end;
    }
    .msg-content {
      max-width: 70%;
      background: color-mix(in srgb, var(--color-success) 12%, var(--color-surface) 88%);
      color: var(--color-text);
      padding: 10px 12px;
      border-radius: 8px;
      border: 1px solid color-mix(in srgb, var(--color-success) 20%, var(--color-border) 80%);
    }
    .mensagem.enviada .msg-content {
      background: var(--color-primary);
      color: var(--color-on-primary);
      border: 1px solid var(--color-primary-dark, var(--color-primary));
    }
    .msg-content strong {
      display: block;
      font-size: 12px;
      margin-bottom: 4px;
      font-weight: 600;
    }
    .msg-content p {
      margin: 0;
      word-wrap: break-word;
      font-size: 13px;
      line-height: 1.4;
    }
    .msg-content small {
      display: block;
      font-size: 11px;
      margin-top: 4px;
      opacity: 0.8;
    }
    .msg-input {
      padding: 15px;
      border-top: 1px solid var(--color-border);
      display: flex;
      gap: 8px;
      background: var(--color-surface);
    }
    .input-meta {
      padding: 0 15px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      min-height: 20px;
    }
    .char-count {
      color: var(--color-text-muted);
      font-size: 12px;
    }
    .error-text {
      color: var(--color-danger);
      font-size: 12px;
      font-weight: 500;
      margin-left: 12px;
    }
    .form-control {
      flex: 1;
      padding: 10px;
      border: 1px solid var(--color-border);
      border-radius: 4px;
      font-family: inherit;
      background: var(--color-surface);
      color: var(--color-text);
    }
    .btn {
      padding: 10px 16px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 500;
    }
    .btn-primary {
      background: var(--color-primary);
      color: var(--color-on-primary);
    }
    .btn-primary:hover {
      background: var(--color-primary-light);
    }
  `]
})
export class ChatComponent implements OnInit, OnDestroy {
  conversas: Conversa[] = [];
  conversaSelecionada: Conversa | null = null;
  mensagensConversa: ChatMessage[] = [];
  novaMsg = '';
  naoLidas = 0;
  usuarioLogado = 0;
  isLoadingConversas = false;
  isLoadingMensagens = false;
  mensagemErro = '';
  isSending = false;
  private pollIntervalId: ReturnType<typeof setInterval> | null = null;
  readonly maxMessageLength = environment.chat.maxMessageLength;
  private readonly pollIntervalMs = environment.chat.pollIntervalMs;
  private destroy$ = new Subject<void>();

  constructor(
    private chatService: ChatService,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private chatUnreadService: ChatUnreadService,
    private telemetry: UxTelemetryService
  ) {}

  ngOnInit() {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    this.usuarioLogado = Number(localStorage.getItem('usuario_id') || '0');
    this.carregarConversas();
    this.chatUnreadService.refreshNow();

    this.chatUnreadService.unreadCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe((count) => {
        this.naoLidas = count;
      });

    this.route.queryParamMap
      .pipe(takeUntil(this.destroy$))
      .subscribe((params) => {
        const usuarioId = Number(params.get('usuarioId'));
        const nome = params.get('nome') || '';

        if (usuarioId && usuarioId !== this.usuarioLogado) {
          this.iniciarConversaDireta(usuarioId, nome);
        }
      });

    this.pollIntervalId = setInterval(() => {
      this.carregarConversas(true);
      if (this.contatoSelecionadoId) {
        this.carregarMensagens(this.contatoSelecionadoId, true);
      }
    }, this.pollIntervalMs);

    this.telemetry.logEvent('chat_screen_opened', {
      userState: 'auth'
    });
  }

  ngOnDestroy() {
    if (this.pollIntervalId) {
      clearInterval(this.pollIntervalId);
      this.pollIntervalId = null;
    }

    this.destroy$.next();
    this.destroy$.complete();
  }

  get contatoSelecionadoId(): number | null {
    return this.conversaSelecionada?.contatoId || null;
  }

  carregarConversas(silent = false) {
    if (!silent) {
      this.isLoadingConversas = true;
    }

    this.chatService.obterConversas()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (conversas) => {
          this.conversas = conversas;
          this.isLoadingConversas = false;

          if (this.conversaSelecionada) {
            const atualizada = this.conversas.find((c) => c.contatoId === this.conversaSelecionada?.contatoId);
            if (atualizada) {
              this.conversaSelecionada = atualizada;
            }
          }
        },
        error: (err) => {
          this.isLoadingConversas = false;
          this.handleHttpError(err, 'Não foi possível carregar suas conversas agora.');
        }
      });
  }

  selecionarConversa(conversa: Conversa) {
    this.mensagemErro = '';
    this.conversaSelecionada = conversa;
    this.carregarMensagens(conversa.contatoId);
  }

  iniciarConversaDireta(outroUsuarioId: number, nomeContato = '') {
    this.conversaSelecionada = {
      id: 0,
      contatoId: outroUsuarioId,
      contatoNome: nomeContato || 'Contato',
      ultimaMensagem: null,
      ultimaMensagemEm: null,
      ultimaMensagemRemetenteId: null,
      naoLidas: 0,
      atualizadaEm: new Date().toISOString()
    };

    this.carregarMensagens(outroUsuarioId);
  }

  carregarMensagens(outroUsuarioId: number, silent = false) {
    if (!silent) {
      this.isLoadingMensagens = true;
    }

    this.chatService.obterConversa(outroUsuarioId, 50)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (msgs) => {
          this.mensagensConversa = [...msgs].sort((a, b) => {
            return new Date(a.criadoEm).getTime() - new Date(b.criadoEm).getTime();
          });

          this.isLoadingMensagens = false;
          this.marcarConversaComoLida(outroUsuarioId);
        },
        error: (err) => {
          this.isLoadingMensagens = false;
          this.handleHttpError(err, 'Não foi possível carregar as mensagens desta conversa.');
        }
      });
  }

  enviarMensagem() {
    const mensagem = (this.novaMsg || '').trim();
    const outroUsuarioId = this.contatoSelecionadoId;

    if (!mensagem || !outroUsuarioId || this.isSending) return;

    if (mensagem.length > this.maxMessageLength) {
      this.mensagemErro = `A mensagem deve ter no máximo ${this.maxMessageLength} caracteres.`;
      return;
    }

    this.mensagemErro = '';
    this.isSending = true;

    this.telemetry.logEvent('chat_message_send_attempt', {
      destinatarioId: outroUsuarioId,
      messageLength: mensagem.length
    });

    this.chatService.enviarMensagem(outroUsuarioId, mensagem)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (msg) => {
          this.mensagensConversa.push(msg);
          this.novaMsg = '';
          this.mensagemErro = '';
          this.isSending = false;
          this.chatUnreadService.refreshNow();
          this.carregarConversas(true);
          this.telemetry.logEvent('chat_message_send_success', {
            destinatarioId: outroUsuarioId
          });
        },
        error: (err) => {
          if (err?.status === 429) {
            this.mensagemErro = typeof err.error === 'string'
              ? err.error
              : 'Você está enviando mensagens muito rápido. Aguarde alguns segundos.';
          } else if (err?.status === 400 && typeof err.error === 'string') {
            this.mensagemErro = err.error;
          } else if (err?.status === 403) {
            this.mensagemErro = 'Você não tem permissão para enviar mensagem nesta conversa.';
          } else {
            this.mensagemErro = 'Não foi possível enviar a mensagem agora. Tente novamente.';
          }

          this.isSending = false;
          this.telemetry.logEvent('chat_ui_send_error', {
            destinatarioId: outroUsuarioId,
            errorCode: err?.status || 0
          });

          this.handleHttpError(err, this.mensagemErro);
        }
      });
  }

  fecharConversa() {
    this.conversaSelecionada = null;
    this.mensagensConversa = [];
    this.mensagemErro = '';
  }

  private marcarConversaComoLida(outroUsuarioId: number) {
    this.chatService.marcarConversaComoLida(outroUsuarioId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.chatUnreadService.refreshNow();
          this.carregarConversas(true);
        },
        error: () => {
          // Não interrompe a UX do chat se marcar como lida falhar.
        }
      });
  }

  private handleHttpError(err: unknown, fallbackMessage: string) {
    const status = (err as { status?: number })?.status;

    if (status === 401) {
      this.authService.logout();
      this.router.navigate(['/login']);
      return;
    }

    if (status === 403) {
      this.mensagemErro = 'Você não tem permissão para acessar esta conversa.';
      return;
    }

    if (!this.mensagemErro) {
      this.mensagemErro = fallbackMessage;
    }
  }
}
