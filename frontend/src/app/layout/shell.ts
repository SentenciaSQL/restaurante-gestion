import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Api } from '../core/api';
import { Branch, Notice } from '../core/models';
import { Session } from '../core/session';

interface NavItem {
  path: string;
  label: string;
  permission: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
})
export class Shell {
  readonly session = inject(Session);
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  readonly mobile = signal(false);
  readonly notesOpen = signal(false);
  readonly branches = signal<Branch[]>([]);
  readonly notes = signal<Notice[]>([]);
  readonly nav: NavItem[] = [
    { path: '/tablero', label: 'Tablero', permission: 'dashboard.view' },
    { path: '/mesas', label: 'Mesas', permission: 'tables.view' },
    { path: '/pos', label: 'Punto de venta', permission: 'orders.create' },
    { path: '/pedidos', label: 'Pedidos', permission: 'orders.view' },
    { path: '/cocina', label: 'Cocina', permission: 'kitchen.view' },
    { path: '/menu', label: 'Menú', permission: 'menu.view' },
    { path: '/inventario', label: 'Inventario', permission: 'inventory.view' },
    { path: '/compras', label: 'Compras', permission: 'purchases.view' },
    { path: '/proveedores', label: 'Proveedores', permission: 'suppliers.manage' },
    { path: '/gastos', label: 'Gastos', permission: 'expenses.view' },
    { path: '/caja', label: 'Caja', permission: 'cash.view' },
    { path: '/reportes', label: 'Reportes', permission: 'reports.view' },
    { path: '/usuarios', label: 'Usuarios', permission: 'users.manage' },
    { path: '/configuracion', label: 'Configuración', permission: 'settings.operations' },
  ];

  constructor() {
    this.api.branches().subscribe({
      next: (rows) => {
        const active = rows.filter((row) => row.active);
        this.branches.set(active);
        if (!this.session.branchId() && active.length > 0) {
          this.session.selectBranch(active[0].id);
        }
        this.loadNotes();
      },
    });
  }

  visible(item: NavItem): boolean {
    if (item.permission === 'settings.operations') {
      return this.session.canAny('settings.operations', 'settings.manage');
    }
    return this.session.can(item.permission);
  }

  loadNotes(): void {
    if (!this.session.can('dashboard.view')) return;
    this.api.notifications(this.session.branchId()).subscribe({ next: (rows) => this.notes.set(rows) });
  }

  branchChange(value: string): void {
    this.session.selectBranch(value ? Number(value) : null);
    this.loadNotes();
  }

  logout(): void {
    this.session.clear();
    void this.router.navigateByUrl('/login');
  }
}
