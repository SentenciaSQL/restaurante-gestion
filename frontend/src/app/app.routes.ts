import { Routes } from '@angular/router';
import { authGuard, guestGuard, perm } from './core/guards';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./features/auth/login').then((m) => m.LoginPage) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tablero' },
      { path: 'tablero', canActivate: [perm('dashboard.view')], loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.DashboardPage) },
      { path: 'mesas', canActivate: [perm('tables.view')], loadComponent: () => import('./features/tables/tables').then((m) => m.TablesPage) },
      { path: 'pos', canActivate: [perm('orders.create', 'orders.view')], loadComponent: () => import('./features/orders/pos').then((m) => m.PosPage) },
      { path: 'pos/:id', canActivate: [perm('orders.create', 'orders.view')], loadComponent: () => import('./features/orders/pos').then((m) => m.PosPage) },
      { path: 'pedidos', canActivate: [perm('orders.view')], loadComponent: () => import('./features/orders/orders').then((m) => m.OrdersPage) },
      { path: 'cocina', canActivate: [perm('kitchen.view')], loadComponent: () => import('./features/kitchen/kitchen').then((m) => m.KitchenPage) },
      { path: 'menu', canActivate: [perm('menu.view')], loadComponent: () => import('./features/menu/menu').then((m) => m.MenuPage) },
      { path: 'inventario', canActivate: [perm('inventory.view')], loadComponent: () => import('./features/inventory/inventory').then((m) => m.InventoryPage) },
      { path: 'compras', canActivate: [perm('purchases.view')], loadComponent: () => import('./features/purchases/purchases').then((m) => m.PurchasesPage) },
      { path: 'proveedores', canActivate: [perm('suppliers.manage', 'purchases.view')], loadComponent: () => import('./features/suppliers/suppliers').then((m) => m.SuppliersPage) },
      { path: 'gastos', canActivate: [perm('expenses.view')], loadComponent: () => import('./features/expenses/expenses').then((m) => m.ExpensesPage) },
      { path: 'caja', canActivate: [perm('cash.view')], loadComponent: () => import('./features/cash-register/cash').then((m) => m.CashPage) },
      { path: 'reportes', canActivate: [perm('reports.view')], loadComponent: () => import('./features/reports/reports').then((m) => m.ReportsPage) },
      { path: 'usuarios', canActivate: [perm('users.manage')], loadComponent: () => import('./features/users/users').then((m) => m.UsersPage) },
      { path: 'configuracion', canActivate: [perm('settings.manage', 'settings.operations', 'dashboard.view')], loadComponent: () => import('./features/settings/settings').then((m) => m.SettingsPage) },
    ],
  },
  { path: '**', redirectTo: 'tablero' },
];
