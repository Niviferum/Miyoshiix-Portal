import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../core/auth-service';
import { PortalModule } from '../../core/portal-api';
import { RetroWindow } from '../../shared/window/retro-window';

/** Accueil du portail : salutation et fenêtre des modules. */
@Component({
  selector: 'app-home-page',
  imports: [RetroWindow],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  protected readonly auth = inject(AuthService);

  /** undefined pendant le chargement, null en cas d'erreur, sinon la liste. */
  protected readonly modules = toSignal(
    inject(HttpClient)
      .get<PortalModule[]>('/api/modules')
      .pipe(catchError(() => of(null))),
  );
}
