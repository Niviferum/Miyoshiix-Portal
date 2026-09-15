import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth-service';

/** Laisse passer si une session existe, sinon renvoie vers /login. */
export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  try {
    const me = await auth.loadMe();
    return me ? true : router.parseUrl('/login');
  } catch {
    return router.parseUrl('/login?error=unreachable');
  }
};
