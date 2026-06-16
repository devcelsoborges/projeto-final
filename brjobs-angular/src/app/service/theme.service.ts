import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ThemeMode = 'system' | 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private static readonly STORAGE_KEY = 'theme-preference';

  private readonly themeModeSubject = new BehaviorSubject<ThemeMode>(this.readInitialThemeMode());
  public readonly themeMode$: Observable<ThemeMode> = this.themeModeSubject.asObservable();

  private readonly darkModeSubject = new BehaviorSubject<boolean>(false);
  public readonly darkMode$: Observable<boolean> = this.darkModeSubject.asObservable();

  private mediaQueryList: MediaQueryList | null = null;

  constructor() {
    this.initializeThemeListener();
    this.applyMode(this.themeModeSubject.value, false);
  }

  getThemeMode(): ThemeMode {
    return this.themeModeSubject.value;
  }

  toggleDarkMode(): void {
    const current = this.resolveEffectiveTheme(this.themeModeSubject.value);
    this.setThemeMode(current === 'dark' ? 'light' : 'dark');
  }

  setDarkMode(isDark: boolean): void {
    this.setThemeMode(isDark ? 'dark' : 'light');
  }

  setThemeMode(mode: ThemeMode): void {
    this.applyMode(mode, true);
  }

  private applyMode(mode: ThemeMode, persist: boolean): void {
    this.themeModeSubject.next(mode);

    const effectiveTheme = this.resolveEffectiveTheme(mode);
    const isDark = effectiveTheme === 'dark';
    this.darkModeSubject.next(isDark);

    const root = document.documentElement;
    root.setAttribute('data-theme', effectiveTheme);
    root.classList.toggle('dark-theme', isDark);

    if (persist) {
      localStorage.setItem(ThemeService.STORAGE_KEY, mode);
    }
  }

  isDarkModeActive(): boolean {
    return this.darkModeSubject.value;
  }

  private readInitialThemeMode(): ThemeMode {
    const savedTheme = localStorage.getItem(ThemeService.STORAGE_KEY);
    if (savedTheme === 'light' || savedTheme === 'dark' || savedTheme === 'system') {
      return savedTheme;
    }
    return 'system';
  }

  private resolveEffectiveTheme(mode: ThemeMode): 'light' | 'dark' {
    if (mode === 'light' || mode === 'dark') {
      return mode;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  private initializeThemeListener(): void {
    this.mediaQueryList = window.matchMedia('(prefers-color-scheme: dark)');
    this.mediaQueryList.addEventListener('change', () => {
      if (this.themeModeSubject.value === 'system') {
        this.applyMode('system', false);
      }
    });
  }
}
