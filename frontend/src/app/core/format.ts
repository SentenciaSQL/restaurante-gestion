import { Injectable, inject } from '@angular/core';
import { Session } from './session';

@Injectable({ providedIn: 'root' })
export class Format {
  private readonly session = inject(Session);

  money(value: number | string | null | undefined): string {
    const profile = this.session.profile();
    return new Intl.NumberFormat(profile?.locale || 'es-DO', {
      style: 'currency',
      currency: profile?.currency || 'DOP',
    }).format(Number(value ?? 0));
  }

  qty(value: number | string | null | undefined): string {
    return new Intl.NumberFormat(this.session.profile()?.locale || 'es-DO', {
      maximumFractionDigits: 3,
    }).format(Number(value ?? 0));
  }

  when(value: string | null | undefined): string {
    if (!value) return '—';
    return new Intl.DateTimeFormat(this.session.profile()?.locale || 'es-DO', {
      dateStyle: 'short',
      timeStyle: 'short',
      timeZone: this.session.profile()?.timezone || 'America/Santo_Domingo',
    }).format(new Date(value));
  }

  day(value: string | null | undefined): string {
    if (!value) return '—';
    const [year, month, date] = value.split('-').map(Number);
    return new Intl.DateTimeFormat(this.session.profile()?.locale || 'es-DO', {
      dateStyle: 'medium',
      timeZone: 'UTC',
    }).format(new Date(Date.UTC(year, (month || 1) - 1, date || 1)));
  }
}

export function localToday(timeZone: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
}

export function shiftDate(iso: string, days: number): string {
  const [year, month, date] = iso.split('-').map(Number);
  const next = new Date(Date.UTC(year, (month || 1) - 1, date || 1));
  next.setUTCDate(next.getUTCDate() + days);
  return next.toISOString().slice(0, 10);
}
