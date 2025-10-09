import { Component, HostListener } from '@angular/core';

@Component({
  selector: 'app-accessibility',
  standalone: true,
  templateUrl: './accessibility.component.html',
  styleUrls: ['./accessibility.component.css']
})
export class AccessibilityComponent {
  private fontSizeStep = 0.1;
  private defaultFontSize = 1; // 1rem
  private maxFontSize = 1.6;
  private minFontSize = 0.8;
  isDarkTheme = false;
  isReading = false;
  private synth = window.speechSynthesis;
  private utterance?: SpeechSynthesisUtterance;

  constructor() {}

  // ======== 🔠 TAMANHO DA FONTE ========
  aumentarFonte() {
    const html = document.documentElement;
    const current = parseFloat(getComputedStyle(html).fontSize);
    const newSize = Math.min(current * (1 + this.fontSizeStep), 16 * this.maxFontSize);
    html.style.fontSize = `${newSize}px`;
  }

  diminuirFonte() {
    const html = document.documentElement;
    const current = parseFloat(getComputedStyle(html).fontSize);
    const newSize = Math.max(current * (1 - this.fontSizeStep), 16 * this.minFontSize);
    html.style.fontSize = `${newSize}px`;
  }

  resetarFonte() {
    document.documentElement.style.fontSize = `${16 * this.defaultFontSize}px`;
  }

  // ======== 🌗 CONTRASTE / TEMA ESCURO ========
  alternarContraste() {
    this.isDarkTheme = !this.isDarkTheme;
    document.body.classList.toggle('dark-theme', this.isDarkTheme);
  }

  // ======== 🔊 LEITURA DE TEXTO ========
  lerTexto() {
    if (this.isReading) {
      this.pararLeitura();
      return;
    }

    const texto = document.body.innerText;
    if (!texto.trim()) return;

    this.utterance = new SpeechSynthesisUtterance(texto);
    this.utterance.lang = 'pt-BR';
    this.utterance.rate = 1.0;
    this.utterance.pitch = 1.0;

    this.isReading = true;
    this.synth.speak(this.utterance);

    this.utterance.onend = () => {
      this.isReading = false;
    };
  }

  pararLeitura() {
    if (this.synth.speaking) {
      this.synth.cancel();
      this.isReading = false;
    }
  }

  // ======== ⌨️ ATALHOS DE TECLADO ========
  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if (event.altKey && event.key === '+') {
      event.preventDefault();
      this.aumentarFonte();
    } else if (event.altKey && event.key === '-') {
      event.preventDefault();
      this.diminuirFonte();
    } else if (event.altKey && event.key === '0') {
      event.preventDefault();
      this.resetarFonte();
    } else if (event.altKey && event.key.toLowerCase() === 'c') {
      event.preventDefault();
      this.alternarContraste();
    } else if (event.altKey && event.key.toLowerCase() === 'l') {
      event.preventDefault();
      this.lerTexto();
    }
  }
}
