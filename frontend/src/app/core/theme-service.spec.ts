import { TestBed } from '@angular/core/testing';
import { THEME_STORAGE_KEY, ThemeService } from './theme-service';

describe('ThemeService', () => {
  const root = document.documentElement;

  beforeEach(() => {
    localStorage.clear();
    root.removeAttribute('data-theme');
    TestBed.resetTestingModule();
  });

  it('reprend le thème déjà posé sur <html>', () => {
    root.setAttribute('data-theme', 'dark');

    expect(TestBed.inject(ThemeService).theme()).toBe('dark');
  });

  it('reprend le choix mémorisé en l’absence de thème posé', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'dark');

    expect(TestBed.inject(ThemeService).theme()).toBe('dark');
  });

  it('bascule, applique le thème sur <html> et le mémorise', () => {
    root.setAttribute('data-theme', 'light');
    const service = TestBed.inject(ThemeService);

    service.toggle();
    TestBed.tick();

    expect(service.theme()).toBe('dark');
    expect(root.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
  });

  it('ignore une valeur mémorisée inconnue', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'rose');

    expect(['light', 'dark']).toContain(TestBed.inject(ThemeService).theme());
  });
});
