// src/app/components/home/home.component.ts

import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';

type TipoPublicacao = 'Prestação' | 'Contratação';

interface Publicacao {
  id: number;
  titulo: string;
  autor: string;
  local: string;
  tipo: TipoPublicacao;
  categoria: string;
  valor: number; // valor do serviço em BRL
  publicadoEmDias: number;
}

interface OpcaoFiltro {
  valor: string;
  label: string;
}

interface Filtro {
  key: string;
  label: string;
  multi: boolean;
  opcoes: OpcaoFiltro[];
}

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:click)': 'fecharDropdowns()' },
})
export class HomeComponent {
  titulo = 'Encontre a Vaga Perfeita no BR-Jobs';

  // Dados simulados de publicações (Mock Data)
  vagas: Publicacao[] = [
    {
      id: 1,
      titulo: 'Diarista para limpeza residencial',
      autor: 'Maria Oliveira',
      local: 'São Paulo, SP',
      tipo: 'Contratação',
      categoria: 'Limpeza e Serviços Domésticos',
      valor: 150,
      publicadoEmDias: 2,
    },
    {
      id: 2,
      titulo: 'Eletricista para instalação elétrica',
      autor: 'João Pereira',
      local: 'Rio de Janeiro, RJ',
      tipo: 'Prestação',
      categoria: 'Elétrica',
      valor: 300,
      publicadoEmDias: 6,
    },
    {
      id: 3,
      titulo: 'Pedreiro para reforma de muro',
      autor: 'Construções Aliança',
      local: 'Belo Horizonte, MG',
      tipo: 'Contratação',
      categoria: 'Reformas e Construção',
      valor: 850,
      publicadoEmDias: 15,
    },
    {
      id: 4,
      titulo: 'Pintor profissional disponível',
      autor: 'Carlos Mendes',
      local: 'Curitiba, PR',
      tipo: 'Prestação',
      categoria: 'Pintura e Acabamento',
      valor: 500,
      publicadoEmDias: 25,
    },
  ];

  // Configuração declarativa dos filtros dinâmicos exibidos como "pills".
  filtros: Filtro[] = [
    {
      key: 'tipo',
      label: 'Tipo',
      multi: true,
      opcoes: [
        { valor: 'Prestação', label: 'Prestação' },
        { valor: 'Contratação', label: 'Contratação' },
      ],
    },
    {
      key: 'categoria',
      label: 'Categoria',
      multi: true,
      opcoes: [
        { valor: 'Reformas e Construção', label: 'Reformas e Construção' },
        { valor: 'Elétrica', label: 'Elétrica' },
        { valor: 'Hidráulica', label: 'Hidráulica' },
        { valor: 'Marcenaria e Móveis', label: 'Marcenaria e Móveis' },
        { valor: 'Pintura e Acabamento', label: 'Pintura e Acabamento' },
        { valor: 'Limpeza e Serviços Domésticos', label: 'Limpeza e Serviços Domésticos' },
        { valor: 'Marido de Aluguel', label: 'Marido de Aluguel' },
        { valor: 'Climatização', label: 'Climatização' },
        { valor: 'Assistência Técnica', label: 'Assistência Técnica' },
        { valor: 'Automotivo', label: 'Automotivo' },
        { valor: 'Eventos', label: 'Eventos' },
        { valor: 'Jardim e Exterior', label: 'Jardim e Exterior' },
        { valor: 'Segurança e Instalações', label: 'Segurança e Instalações' },
      ],
    },
    {
      key: 'valor',
      label: 'Valor',
      multi: false,
      opcoes: [
        { valor: '0-100', label: 'Até R$ 100' },
        { valor: '100-300', label: 'R$ 100 a R$ 300' },
        { valor: '300-600', label: 'R$ 300 a R$ 600' },
        { valor: '600-', label: 'Acima de R$ 600' },
      ],
    },
    {
      key: 'data',
      label: 'Data de publicação',
      multi: false,
      opcoes: [
        { valor: '1', label: 'Últimas 24 horas' },
        { valor: '7', label: 'Últimos 7 dias' },
        { valor: '30', label: 'Últimos 30 dias' },
      ],
    },
  ];

  // Estado dos campos de busca livre.
  termo = signal('');
  localizacao = signal('');

  // Seleções de cada filtro, indexadas pela key do filtro.
  selecoes = signal<Record<string, string[]>>({
    tipo: [],
    categoria: [],
    valor: [],
    data: [],
  });

  // Filtro cujo dropdown está aberto (null = todos fechados).
  dropdownAberto = signal<string | null>(null);

  // Lista filtrada de publicações, recalculada de forma reativa.
  vagasFiltradas = computed<Publicacao[]>(() => {
    const termo = this.termo().trim().toLowerCase();
    const local = this.localizacao().trim().toLowerCase();
    const sel = this.selecoes();

    return this.vagas.filter((pub) => {
      if (termo && !`${pub.titulo} ${pub.autor}`.toLowerCase().includes(termo)) {
        return false;
      }
      if (local && !pub.local.toLowerCase().includes(local)) {
        return false;
      }
      if (sel['tipo'].length && !sel['tipo'].includes(pub.tipo)) {
        return false;
      }
      if (sel['categoria'].length && !sel['categoria'].includes(pub.categoria)) {
        return false;
      }
      if (sel['valor'].length && !this.dentroDaFaixaValor(pub.valor, sel['valor'][0])) {
        return false;
      }
      if (sel['data'].length && pub.publicadoEmDias > Number(sel['data'][0])) {
        return false;
      }
      return true;
    });
  });

  // Quantidade total de filtros ativos (busca + seleções), usado no botão "Limpar".
  totalFiltrosAtivos = computed(() => {
    const selecionados = Object.values(this.selecoes()).reduce((soma, arr) => soma + arr.length, 0);
    const busca = (this.termo().trim() ? 1 : 0) + (this.localizacao().trim() ? 1 : 0);
    return selecionados + busca;
  });

  atualizarTermo(evento: Event): void {
    this.termo.set((evento.target as HTMLInputElement).value);
  }

  atualizarLocalizacao(evento: Event): void {
    this.localizacao.set((evento.target as HTMLInputElement).value);
  }

  alternarDropdown(key: string, evento: Event): void {
    evento.stopPropagation();
    this.dropdownAberto.update((atual) => (atual === key ? null : key));
  }

  fecharDropdowns(): void {
    this.dropdownAberto.set(null);
  }

  alternarOpcao(filtroKey: string, valor: string, multi: boolean): void {
    this.selecoes.update((estado) => {
      const atual = estado[filtroKey] ?? [];
      let novo: string[];
      if (multi) {
        novo = atual.includes(valor) ? atual.filter((v) => v !== valor) : [...atual, valor];
      } else {
        // Seleção única: clicar de novo na mesma opção desmarca.
        novo = atual.includes(valor) ? [] : [valor];
      }
      return { ...estado, [filtroKey]: novo };
    });
  }

  estaSelecionado(filtroKey: string, valor: string): boolean {
    return (this.selecoes()[filtroKey] ?? []).includes(valor);
  }

  contagem(filtroKey: string): number {
    return (this.selecoes()[filtroKey] ?? []).length;
  }

  limparFiltros(): void {
    this.termo.set('');
    this.localizacao.set('');
    this.selecoes.set({ tipo: [], categoria: [], valor: [], data: [] });
    this.dropdownAberto.set(null);
  }

  formatarValor(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      maximumFractionDigits: 0,
    }).format(valor);
  }

  formatarPublicacao(dias: number): string {
    if (dias <= 0) {
      return 'Publicada hoje';
    }
    if (dias === 1) {
      return 'Publicada há 1 dia';
    }
    return `Publicada há ${dias} dias`;
  }

  private dentroDaFaixaValor(valor: number, faixa: string): boolean {
    const [minStr, maxStr] = faixa.split('-');
    const min = Number(minStr) || 0;
    const max = maxStr ? Number(maxStr) : Infinity;
    return valor >= min && valor <= max;
  }
}
