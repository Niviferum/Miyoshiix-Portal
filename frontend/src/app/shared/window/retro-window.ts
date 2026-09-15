import { Component, input } from '@angular/core';

let nextId = 0;

/**
 * Fenêtre rétro : barre de titre et contenu projeté. Le titre est un vrai titre HTML
 * (h1 ou h2) qui nomme la section pour les technologies d'assistance.
 */
@Component({
  selector: 'app-retro-window',
  templateUrl: './retro-window.html',
  styleUrl: './retro-window.scss',
})
export class RetroWindow {
  readonly title = input.required<string>();
  readonly headingLevel = input<1 | 2>(2);

  protected readonly titleId = `window-title-${nextId++}`;
}
