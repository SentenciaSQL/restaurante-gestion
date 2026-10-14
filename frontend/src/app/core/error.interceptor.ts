import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Session } from './session';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(Session);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/login')) {
        session.clear();
        void router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};

export function errorMessage(error: unknown, fallback = 'No se pudo completar la operación.'): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as { message?: string; fields?: Record<string, string> } | null;
    if (body?.fields && Object.keys(body.fields).length > 0) {
      return Object.values(body.fields).join(' ');
    }
    if (body?.message) return body.message;
    if (error.status === 0) return 'No hay conexión con el servidor.';
  }
  return fallback;
}
