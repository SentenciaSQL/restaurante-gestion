import { Injectable, computed, signal } from '@angular/core';
import { Profile } from './models';

export interface Toast {
  id: string;
  level: 'success' | 'error' | 'info';
  message: string;
}

export interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel: string;
  resolve: (value: boolean) => void;
}

const NAV = [
  { path: '/tablero', permission: 'dashboard.view' },
  { path: '/mesas', permission: 'tables.view' },
  { path: '/pos', permission: 'orders.create' },
  { path: '/pedidos', permission: 'orders.view' },
  { path: '/cocina', permission: 'kitchen.view' },
  { path: '/menu', permission: 'menu.view' },
  { path: '/inventario', permission: 'inventory.view' },
  { path: '/compras', permission: 'purchases.view' },
  { path: '/proveedores', permission: 'suppliers.manage' },
  { path: '/gastos', permission: 'expenses.view' },
  { path: '/caja', permission: 'cash.view' },
  { path: '/reportes', permission: 'reports.view' },
  { path: '/usuarios', permission: 'users.manage' },
  { path: '/configuracion', permission: 'settings.operations' },
];

@Injectable({ providedIn: 'root' })
export class Session {
  readonly token = signal<string | null>(sessionStorage.getItem('fogon.token'));
  readonly profile = signal<Profile | null>(readProfile());
  readonly branchId = signal<number | null>(readBranch());
  readonly theme = signal<'fogon' | 'fogon-dark'>(readTheme());
  readonly sidebarOpen = signal(localStorage.getItem('fogon.sidebar') !== 'closed');
  readonly toasts = signal<Toast[]>([]);
  readonly confirmRequest = signal<ConfirmRequest | null>(null);
  readonly signedIn = computed(() => !!this.token() && !!this.profile());

  constructor() {
    document.documentElement.dataset['theme'] = this.theme();
  }

  can(permission: string): boolean {
    return this.profile()?.permissions.includes(permission) ?? false;
  }

  canAny(...permissions: string[]): boolean {
    return permissions.some((permission) => this.can(permission));
  }

  home(): string {
    return NAV.find((item) => this.can(item.permission))?.path ?? '/login';
  }

  remember(token: string, profile: Profile): void {
    sessionStorage.setItem('fogon.token', token);
    sessionStorage.setItem('fogon.profile', JSON.stringify(profile));
    this.token.set(token);
    this.profile.set(profile);
    if (profile.branchId && !this.branchId()) {
      this.selectBranch(profile.branchId);
    }
  }

  selectBranch(id: number | null): void {
    this.branchId.set(id);
    if (id == null) sessionStorage.removeItem('fogon.branch');
    else sessionStorage.setItem('fogon.branch', String(id));
  }

  toggleTheme(): void {
    const next = this.theme() === 'fogon' ? 'fogon-dark' : 'fogon';
    this.theme.set(next);
    localStorage.setItem('fogon.theme', next);
    document.documentElement.dataset['theme'] = next;
  }

  toggleSidebar(): void {
    const next = !this.sidebarOpen();
    this.sidebarOpen.set(next);
    localStorage.setItem('fogon.sidebar', next ? 'open' : 'closed');
  }

  clear(): void {
    sessionStorage.removeItem('fogon.token');
    sessionStorage.removeItem('fogon.profile');
    this.token.set(null);
    this.profile.set(null);
  }

  toast(level: Toast['level'], message: string): void {
    const id = crypto.randomUUID();
    this.toasts.update((items) => [...items, { id, level, message }]);
    window.setTimeout(() => this.dismiss(id), 5200);
  }

  dismiss(id: string): void {
    this.toasts.update((items) => items.filter((item) => item.id !== id));
  }

  confirm(title: string, message: string, confirmLabel = 'Confirmar'): Promise<boolean> {
    return new Promise((resolve) => this.confirmRequest.set({ title, message, confirmLabel, resolve }));
  }

  answer(value: boolean): void {
    const current = this.confirmRequest();
    current?.resolve(value);
    this.confirmRequest.set(null);
  }
}

function readProfile(): Profile | null {
  const raw = sessionStorage.getItem('fogon.profile');
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Profile;
  } catch {
    return null;
  }
}

function readBranch(): number | null {
  const raw = sessionStorage.getItem('fogon.branch');
  return raw ? Number(raw) : null;
}

function readTheme(): 'fogon' | 'fogon-dark' {
  return localStorage.getItem('fogon.theme') === 'fogon-dark' ? 'fogon-dark' : 'fogon';
}
