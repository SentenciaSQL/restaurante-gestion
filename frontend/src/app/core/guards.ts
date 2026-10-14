import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Session } from './session';

export const authGuard: CanActivateFn = () => {
  const session = inject(Session);
  if (session.signedIn()) return true;
  return inject(Router).createUrlTree(['/login']);
};

export const guestGuard: CanActivateFn = () => {
  const session = inject(Session);
  if (!session.signedIn()) return true;
  return inject(Router).createUrlTree([session.home()]);
};

export function perm(...codes: string[]): CanActivateFn {
  return () => {
    const session = inject(Session);
    if (session.canAny(...codes)) return true;
    return inject(Router).createUrlTree([session.home()]);
  };
}
