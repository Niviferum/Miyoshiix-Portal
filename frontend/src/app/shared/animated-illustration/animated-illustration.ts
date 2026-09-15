import { Component, inject, input } from '@angular/core';
import { MotionService } from '../../core/motion-service';

/**
 * Illustration animée décorative, centrée dans une zone enfoncée. Affiche l'image fixe
 * quand les animations sont désactivées (case du pied de page ou réglage du système).
 */
@Component({
  selector: 'app-animated-illustration',
  templateUrl: './animated-illustration.html',
  styleUrl: './animated-illustration.scss',
})
export class AnimatedIllustration {
  readonly animatedSrc = input.required<string>();
  readonly staticSrc = input.required<string>();
  /** Taille d'affichage en pixels CSS : un multiple entier de la grille du pixel art. */
  readonly width = input.required<number>();
  readonly height = input.required<number>();

  protected readonly motion = inject(MotionService);
}
