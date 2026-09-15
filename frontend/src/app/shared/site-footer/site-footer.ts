import { Component, inject } from '@angular/core';
import { MotionService } from '../../core/motion-service';

/** Pied de page discret : réglage global des animations. */
@Component({
  selector: 'app-site-footer',
  templateUrl: './site-footer.html',
  styleUrl: './site-footer.scss',
})
export class SiteFooter {
  protected readonly motion = inject(MotionService);

  protected onChange(event: Event): void {
    const disableAnimations = (event.target as HTMLInputElement).checked;
    this.motion.setAnimationsEnabled(!disableAnimations);
  }
}
