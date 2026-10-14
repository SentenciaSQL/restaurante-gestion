import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Session } from './session';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(Session).token();
  if (!token || req.url.includes('/api/auth/login')) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
