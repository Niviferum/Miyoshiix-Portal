import { Component, DOCUMENT, inject } from '@angular/core';

/**
 * Lien d'évitement vers #contenu. Le focus est déplacé en JavaScript : avec
 * <base href="/">, un simple lien vers une ancre renverrait à la racine du site.
 */
@Component({
  selector: 'app-skip-link',
  template: `<a class="skip-link" href="#contenu" (click)="skip($event)">Aller au contenu</a>`,
})
export class SkipLink {
  private readonly document = inject(DOCUMENT);

  protected skip(event: Event): void {
    event.preventDefault();
    this.document.getElementById('contenu')?.focus();
  }
}
