import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth-service';
import { SiteFooter } from '../../shared/site-footer/site-footer';
import { SkipLink } from '../../shared/skip-link/skip-link';
import { ThemeToggle } from '../../shared/theme-toggle/theme-toggle';

/** Mise en page des écrans connectés : barre du haut et contenu de la route enfant. */
@Component({
  selector: 'app-shell-layout',
  imports: [RouterLink, RouterOutlet, SiteFooter, SkipLink, ThemeToggle],
  templateUrl: './shell-layout.html',
  styleUrl: './shell-layout.scss',
})
export class ShellLayout {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loggingOut = signal(false);
  protected readonly logoutFailed = signal(false);

  protected async logout(): Promise<void> {
    this.loggingOut.set(true);
    this.logoutFailed.set(false);
    try {
      await this.auth.logout();
      await this.router.navigateByUrl('/login');
    } catch {
      this.logoutFailed.set(true);
    } finally {
      this.loggingOut.set(false);
    }
  }
}
