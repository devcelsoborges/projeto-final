import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatMessage, Conversa } from '../../service/chat.service';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="chat-container">
      <div class="conversas-list">
        <h3>Conversas</h3>
        <div class="nao-lidas-badge" *ngIf="naoLidas > 0">{{ naoLidas }} novas</div>
        
        <div *ngIf="conversas.length === 0" class="no-conversas">
          Nenhuma conversa ativa
        </div>
        
        <div *ngFor="let conversa of conversas" 
             class="conversa-item"
             [class.ativa]="conversaSelecionada?.id === conversa.id"
             (click)="selecionarConversa(conversa)">
          <div class="conversa-header">
            <h5>Conversa #{{ conversa.id }}</h5>
            <span class="data">{{ conversa.dataAtualizacao | date:'short' }}</span>
          </div>
          <p *ngIf="conversa.ultimaMensagem" class="ultima-msg">
            {{ conversa.ultimaMensagem.conteudo | slice:0:40 }}...
          </p>
        </div>
      </div>

      <div class="chat-area">
        <div *ngIf="!conversaSelecionada" class="select-conversa">
          Selecione uma conversa para começar
        </div>

        <div *ngIf="conversaSelecionada" class="conversa-chat">
          <div class="chat-header">
            <h4>{{ nomeContatoDireto ? 'Conversa com ' + nomeContatoDireto : 'Conversa' }}</h4>
            <button class="btn-close" (click)="fecharConversa()">×</button>
          </div>

          <div class="mensagens">
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
                   placeholder="Digite sua mensagem..."
                   class="form-control" />
            <button (click)="enviarMensagem()" class="btn btn-primary">Enviar</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .chat-container {
      display: flex;
      height: 600px;
      border: 1px solid #ddd;
      border-radius: 8px;
      overflow: hidden;
    }
    .conversas-list {
      flex: 0 0 250px;
      border-right: 1px solid #ddd;
      overflow-y: auto;
      padding: 15px;
    }
    .conversas-list h3 {
      margin: 0 0 15px;
    }
    .nao-lidas-badge {
      display: inline-block;
      background: #ff6b6b;
      color: white;
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
      margin-bottom: 10px;
    }
    .conversa-item {
      padding: 10px;
      border-radius: 4px;
      cursor: pointer;
      margin-bottom: 5px;
      border-left: 3px solid transparent;
    }
    .conversa-item:hover {
      background: #f5f5f5;
    }
    .conversa-item.ativa {
      background: #e3f2fd;
      border-left-color: #2196F3;
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
    }
    .data {
      font-size: 11px;
      color: #999;
    }
    .ultima-msg {
      margin: 5px 0 0;
      font-size: 12px;
      color: #666;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .no-conversas {
      text-align: center;
      color: #999;
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
      color: #999;
    }
    .conversa-chat {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    .chat-header {
      padding: 15px;
      border-bottom: 1px solid #ddd;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .chat-header h4 {
      margin: 0;
    }
    .btn-close {
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #999;
    }
    .mensagens {
      flex: 1;
      overflow-y: auto;
      padding: 15px;
      display: flex;
      flex-direction: column;
      gap: 10px;
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
      background: #f5f5f5;
      padding: 10px 12px;
      border-radius: 8px;
    }
    .mensagem.enviada .msg-content {
      background: #2196F3;
      color: white;
    }
    .msg-content strong {
      display: block;
      font-size: 12px;
      margin-bottom: 4px;
    }
    .msg-content p {
      margin: 0;
      word-wrap: break-word;
    }
    .msg-content small {
      display: block;
      font-size: 11px;
      margin-top: 4px;
      opacity: 0.7;
    }
    .msg-input {
      padding: 15px;
      border-top: 1px solid #ddd;
      display: flex;
      gap: 8px;
    }
    .form-control {
      flex: 1;
      padding: 10px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-family: inherit;
    }
    .btn {
      padding: 10px 16px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 500;
    }
    .btn-primary {
      background: #2196F3;
      color: white;
    }
    .btn-primary:hover {
      background: #1976D2;
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
  contatoDiretoId: number | null = null;
  nomeContatoDireto = '';
  private destroy$ = new Subject<void>();

  constructor(
    private chatService: ChatService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.usuarioLogado = Number(localStorage.getItem('usuario_id') || '0');
    this.carregarConversas();
    this.atualizarNaoLidas();

    this.route.queryParamMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const usuarioId = Number(params.get('usuarioId'));
        const nome = params.get('nome') || '';

        if (usuarioId && usuarioId !== this.usuarioLogado) {
          this.nomeContatoDireto = nome;
          this.iniciarConversaDireta(usuarioId);
        }
      });

    // Atualizar nao-lidas a cada 10 segundos
    setInterval(() => this.atualizarNaoLidas(), 10000);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  carregarConversas() {
    this.chatService.obterConversas()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (conversas) => {
          this.conversas = conversas;
        },
        error: (err) => console.error('Erro ao carregar conversas:', err)
      });
  }

  selecionarConversa(conversa: Conversa) {
    this.contatoDiretoId = null;
    this.nomeContatoDireto = '';
    this.conversaSelecionada = conversa;
    const outroUsuarioId = conversa.usuario1Id === this.usuarioLogado ? conversa.usuario2Id : conversa.usuario1Id;
    this.carregarMensagens(outroUsuarioId);
  }

  iniciarConversaDireta(outroUsuarioId: number) {
    this.contatoDiretoId = outroUsuarioId;
    this.conversaSelecionada = {
      id: 0,
      usuario1Id: this.usuarioLogado,
      usuario2Id: outroUsuarioId,
      ultimaMensagem: null,
      dataAtualizacao: new Date().toISOString()
    };
    this.carregarMensagens(outroUsuarioId);
  }

  carregarMensagens(outroUsuarioId: number) {
    this.chatService.obterConversa(outroUsuarioId, 50)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (msgs) => {
          this.mensagensConversa = msgs;
          // Marcar como lidas (implementar se necessário)
        },
        error: (err) => console.error('Erro ao carregar mensagens:', err)
      });
  }

  enviarMensagem() {
    if (!this.novaMsg.trim() || !this.conversaSelecionada) return;

    const outroUsuarioId = this.contatoDiretoId
      ? this.contatoDiretoId
      : (this.conversaSelecionada.usuario1Id === this.usuarioLogado
        ? this.conversaSelecionada.usuario2Id
        : this.conversaSelecionada.usuario1Id);

    this.chatService.enviarMensagem(outroUsuarioId, this.novaMsg)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (msg) => {
          this.mensagensConversa.push(msg);
          this.novaMsg = '';
        },
        error: (err) => console.error('Erro ao enviar mensagem:', err)
      });
  }

  fecharConversa() {
    this.conversaSelecionada = null;
    this.mensagensConversa = [];
    this.contatoDiretoId = null;
    this.nomeContatoDireto = '';
  }

  atualizarNaoLidas() {
    this.chatService.contarNaoLidas()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (count) => {
          this.naoLidas = count;
        },
        error: (err) => console.error('Erro ao contar não-lidas:', err)
      });
  }
}
