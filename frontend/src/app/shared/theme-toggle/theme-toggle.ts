import { Component, inject } from '@angular/core';
import { ThemeService } from '../../core/theme-service';

/** Bouton de bascule entre thème clair et thème sombre. */
@Component({
  selector: 'app-theme-toggle',
  templateUrl: './theme-toggle.html',
})
export class ThemeToggle {
  protected readonly themes = inject(ThemeService);
}
