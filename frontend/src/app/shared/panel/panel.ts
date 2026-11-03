import { Component, input } from '@angular/core';

let nextId = 0;

/**
 * Panneau : en-tête et contenu projeté. Le titre est un vrai titre HTML (h1 ou h2)
 * qui nomme la section pour les technologies d'assistance. L'étiquette codifiée
 * facultative (« MOD-01 ») est décorative.
 */
@Component({
  selector: 'app-panel',
  templateUrl: './panel.html',
  styleUrl: './panel.scss',
})
export class Panel {
  readonly title = input.required<string>();
  readonly headingLevel = input<1 | 2>(2);
  readonly code = input<string>();

  protected readonly titleId = `panel-title-${nextId++}`;
}
