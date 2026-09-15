import { TestBed } from '@angular/core/testing';
import { ThemeToggle } from './theme-toggle';

describe('ThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.setAttribute('data-theme', 'light');
    TestBed.resetTestingModule();
  });

  const visibleImage = (element: HTMLElement) =>
    [...element.querySelectorAll('img')].filter((img) => !img.hidden);

  it('nomme le bouton par l’alt de la seule icône visible, et bascule au clic', async () => {
    const fixture = TestBed.createComponent(ThemeToggle);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(visibleImage(element).map((img) => img.alt)).toEqual(['Mode sombre']);

    element.querySelector('button')!.click();
    await fixture.whenStable();

    expect(visibleImage(element).map((img) => img.alt)).toEqual(['Mode clair']);
  });
});
