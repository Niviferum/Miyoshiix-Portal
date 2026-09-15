import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Me } from './portal-api';

/** Session de l'utilisateur : chargement du profil, connexion Discord, déconnexion. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** Profil connecté, ou null sans session. */
  readonly me = signal<Me | null>(null);

  /** Interroge /api/me. Renvoie null sur 401, relance les autres erreurs. */
  async loadMe(): Promise<Me | null> {
    try {
      const me = await firstValueFrom(this.http.get<Me>('/api/me'));
      this.me.set(me);
      return me;
    } catch (error) {
      this.me.set(null);
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return null;
      }
      throw error;
    }
  }

  /** Navigation complète vers le back, qui redirige vers Discord. */
  login(): void {
    window.location.href = '/oauth2/authorization/discord';
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/logout', null));
    this.me.set(null);
  }
}
