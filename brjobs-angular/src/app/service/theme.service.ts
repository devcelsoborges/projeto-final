import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private darkModeSubject = new BehaviorSubject<boolean>(this.isDarkMode());
  public darkMode$: Observable<boolean> = this.darkModeSubject.asObservable();

  constructor() {
    // Aplicar tema ao inicializar
    this.applyTheme(this.isDarkMode());
  }

  /**
   * Verifica se o dark mode está ativo
   */
  private isDarkMode(): boolean {
    // Verificar localStorage
    const savedTheme = localStorage.getItem('theme-preference');
    if (savedTheme) {
      return savedTheme === 'dark';
    }

    // Verificar preferência do sistema
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  /**
   * Toggle do tema escuro
   */
  toggleDarkMode(): void {
    const isDark = !this.darkModeSubject.value;
    this.setDarkMode(isDark);
  }

  /**
   * Define o tema escuro
   */
  setDarkMode(isDark: boolean): void {
    this.darkModeSubject.next(isDark);
    this.applyTheme(isDark);
    localStorage.setItem('theme-preference', isDark ? 'dark' : 'light');
  }

  /**
   * Aplica o tema ao documento
   */
  private applyTheme(isDark: boolean): void {
    const html = document.documentElement;
    if (isDark) {
      html.classList.add('dark-theme');
    } else {
      html.classList.remove('dark-theme');
    }
  }

  /**
   * Retorna o estado atual do tema
   */
  isDarkModeActive(): boolean {
    return this.darkModeSubject.value;
  }
}
