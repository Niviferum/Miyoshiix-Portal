import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth-service';
import { AnimatedIllustration } from '../../shared/animated-illustration/animated-illustration';
import { SiteFooter } from '../../shared/site-footer/site-footer';
import { SkipLink } from '../../shared/skip-link/skip-link';
import { ThemeToggle } from '../../shared/theme-toggle/theme-toggle';
import { RetroWindow } from '../../shared/window/retro-window';

/** Messages affichés selon le code ?error= renvoyé par le back. */
const ERROR_MESSAGES: Record<string, string> = {
  not_allowed: "Ce compte Discord n'est pas autorisé sur le portail.",
  invalid_user: 'Discord a renvoyé un profil inexploitable.',
  login_failed: 'La connexion a échoué, réessaie.',
  unreachable: 'Le serveur ne répond pas.',
};

/** Page de connexion : bouton Discord et message d'erreur éventuel. */
@Component({
  selector: 'app-login-page',
  imports: [SiteFooter, SkipLink, ThemeToggle, RetroWindow, AnimatedIllustration],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);

  private readonly errorCode = inject(ActivatedRoute).snapshot.queryParamMap.get('error');

  // Seuls les codes connus produisent un message ; le paramètre brut n'est jamais affiché.
  protected readonly errorMessage = this.errorCode
    ? (ERROR_MESSAGES[this.errorCode] ?? ERROR_MESSAGES['login_failed'])
    : null;

  protected readonly redirecting = signal(false);

  protected login(): void {
    this.redirecting.set(true);
    this.auth.login();
  }
}
