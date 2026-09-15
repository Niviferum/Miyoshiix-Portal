import { DOCUMENT, Injectable, effect, inject, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

export const THEME_STORAGE_KEY = 'miyoshix-theme';

/** Couleur de la barre du navigateur mobile pour chaque thème (fond du bureau). */
const THEME_COLORS: Record<Theme, string> = {
  light: '#60d394',
  dark: '#2d3047',
};

/** Thème clair ou sombre : état courant, bascule et mémorisation du choix. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  readonly theme = signal<Theme>(this.initialTheme());

  constructor() {
    effect(() => this.apply(this.theme()));
  }

  toggle(): void {
    this.theme.update((current) => (current === 'dark' ? 'light' : 'dark'));
    this.store(this.theme());
  }

  /** Thème déjà posé par preferences-init.js, sinon choix mémorisé, sinon réglage du système. */
  private initialTheme(): Theme {
    const fromDocument = this.document.documentElement.getAttribute('data-theme');
    if (fromDocument === 'light' || fromDocument === 'dark') {
      return fromDocument;
    }
    const stored = this.read();
    if (stored) {
      return stored;
    }
    const prefersDark = this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)').matches;
    return prefersDark ? 'dark' : 'light';
  }

  private apply(theme: Theme): void {
    this.document.documentElement.setAttribute('data-theme', theme);
    this.document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLORS[theme]);
  }

  private read(): Theme | null {
    try {
      const value = this.document.defaultView?.localStorage.getItem(THEME_STORAGE_KEY);
      return value === 'light' || value === 'dark' ? value : null;
    } catch {
      return null;
    }
  }

  private store(theme: Theme): void {
    try {
      this.document.defaultView?.localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Stockage indisponible (navigation privée, cookies bloqués) : le choix vaut pour la session.
    }
  }
}
