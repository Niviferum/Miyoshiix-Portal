import { TestBed } from '@angular/core/testing';
import { ThemeToggle } from './theme-toggle';

describe('ThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.setAttribute('data-theme', 'light');
    TestBed.resetTestingModule();
  });

  it('annonce le thème cible dans son libellé, et bascule au clic', async () => {
    const fixture = TestBed.createComponent(ThemeToggle);
    await fixture.whenStable();
    const button = (fixture.nativeElement as HTMLElement).querySelector('button')!;

    expect(button.textContent?.trim()).toBe('Mode sombre');

    button.click();
    await fixture.whenStable();

    expect(button.textContent?.trim()).toBe('Mode clair');
  });
});
