import { TestBed } from '@angular/core/testing';
import { MOTION_STORAGE_KEY, MotionService } from './motion-service';

describe('MotionService', () => {
  const root = document.documentElement;

  beforeEach(() => {
    localStorage.clear();
    root.removeAttribute('data-motion');
    TestBed.resetTestingModule();
  });

  it('reprend la préférence déjà posée sur <html>', () => {
    root.setAttribute('data-motion', 'reduced');

    expect(TestBed.inject(MotionService).animationsEnabled()).toBe(false);
  });

  it('reprend le choix mémorisé en l’absence de préférence posée', () => {
    localStorage.setItem(MOTION_STORAGE_KEY, 'off');

    expect(TestBed.inject(MotionService).animationsEnabled()).toBe(false);
  });

  it('désactive les animations, applique la préférence sur <html> et la mémorise', () => {
    root.setAttribute('data-motion', 'full');
    const service = TestBed.inject(MotionService);

    service.setAnimationsEnabled(false);
    TestBed.tick();

    expect(root.getAttribute('data-motion')).toBe('reduced');
    expect(localStorage.getItem(MOTION_STORAGE_KEY)).toBe('off');
  });
});
