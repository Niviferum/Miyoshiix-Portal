import { DOCUMENT, Injectable, effect, inject, signal } from '@angular/core';

export const MOTION_STORAGE_KEY = 'miyoshix-animations';

/** Préférence d'animation du site : état courant, modification et mémorisation du choix. */
@Injectable({ providedIn: 'root' })
export class MotionService {
  private readonly document = inject(DOCUMENT);

  readonly animationsEnabled = signal<boolean>(this.initialValue());

  constructor() {
    effect(() =>
      this.document.documentElement.setAttribute('data-motion', this.animationsEnabled() ? 'full' : 'reduced'),
    );
  }

  setAnimationsEnabled(enabled: boolean): void {
    this.animationsEnabled.set(enabled);
    try {
      this.document.defaultView?.localStorage.setItem(MOTION_STORAGE_KEY, enabled ? 'on' : 'off');
    } catch {
      // Stockage indisponible : le choix vaut pour la session.
    }
  }

  /** Valeur posée par preferences-init.js, sinon choix mémorisé, sinon réglage du système. */
  private initialValue(): boolean {
    const fromDocument = this.document.documentElement.getAttribute('data-motion');
    if (fromDocument === 'full' || fromDocument === 'reduced') {
      return fromDocument === 'full';
    }
    try {
      const stored = this.document.defaultView?.localStorage.getItem(MOTION_STORAGE_KEY);
      if (stored === 'on' || stored === 'off') {
        return stored === 'on';
      }
    } catch {
      // Stockage indisponible : on se rabat sur le réglage du système.
    }
    return !this.document.defaultView?.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  }
}
