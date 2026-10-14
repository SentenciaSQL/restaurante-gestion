import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format } from '../../core/format';
import { PURCHASE, label } from '../../core/labels';
import { Ingredient, PaymentMethod, PurchaseDetail, PurchaseOrder, Supplier, Warehouse } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-purchases',
  imports: [ReactiveFormsModule],
  templateUrl: './purchases.html',
})
export class PurchasesPage {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  readonly orders = signal<PurchaseOrder[]>([]);
  readonly suppliers = signal<Supplier[]>([]);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly ingredients = signal<Ingredient[]>([]);
  readonly methods = signal<PaymentMethod[]>([]);
  readonly detail = signal<PurchaseDetail | null>(null);
  readonly lines = signal<{ ingredientId: number; quantity: number; unitCost: number; taxRate: number }[]>([]);
  readonly receiveKey = signal(crypto.randomUUID());
  readonly form = inject(FormBuilder).nonNullable.group({
    supplierId: [null as number | null, Validators.required],
    warehouseId: [null as number | null, Validators.required],
    expectedDate: [''],
    notes: [''],
    ingredientId: [null as number | null],
    quantity: [1],
    unitCost: [0],
    taxRate: [0],
  });
  label = label;
  states = PURCHASE;

  constructor() { this.reload(); }

  reload(): void {
    const branch = this.session.branchId();
    if (!branch) return;
    this.api.purchases(branch).subscribe({ next: (rows) => this.orders.set(rows) });
    this.api.suppliers().subscribe({ next: (rows) => this.suppliers.set(rows) });
    this.api.warehouses(branch).subscribe({ next: (rows) => this.warehouses.set(rows) });
    this.api.ingredients().subscribe({ next: (rows) => this.ingredients.set(rows) });
    this.api.methods().subscribe({ next: (rows) => this.methods.set(rows.filter((row) => row.active)) });
  }

  supplierName(id: number): string { return this.suppliers().find((row) => row.id === id)?.name ?? String(id); }
  ingredientName(id: number): string { return this.ingredients().find((row) => row.id === id)?.name ?? String(id); }

  addLine(): void {
    const value = this.form.getRawValue();
    if (!value.ingredientId || value.quantity <= 0) return;
    this.lines.update((rows) => [...rows, { ingredientId: value.ingredientId!, quantity: value.quantity, unitCost: value.unitCost, taxRate: value.taxRate }]);
  }

  create(): void {
    const branch = this.session.branchId();
    const value = this.form.getRawValue();
    if (!branch || !value.supplierId || !value.warehouseId || !this.lines().length) return;
    this.api.createPurchase({ branchId: branch, supplierId: value.supplierId, warehouseId: value.warehouseId, expectedDate: value.expectedDate || null, notes: value.notes, lines: this.lines() }).subscribe({
      next: (detail) => { this.detail.set(detail); this.lines.set([]); this.session.toast('success', 'Orden creada en borrador.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  open(id: number): void { this.api.purchase(id).subscribe({ next: (row) => this.detail.set(row), error: (error: unknown) => this.session.toast('error', errorMessage(error)) }); }

  markOrdered(): void {
    const row = this.detail();
    if (!row) return;
    this.api.orderPurchase(row.order.id, row.order.version).subscribe({ next: () => this.open(row.order.id), error: (error: unknown) => this.session.toast('error', errorMessage(error)) });
  }

  receive(lineId: number, quantity: string, documentRef: string): void {
    const row = this.detail();
    const qty = Number(quantity);
    if (!row || !qty) return;
    this.api.receivePurchase(row.order.id, { version: row.order.version, documentRef, lines: [{ lineId, quantity: qty }] }, this.receiveKey()).subscribe({
      next: () => { this.receiveKey.set(crypto.randomUUID()); this.session.toast('success', 'Recepción aplicada al inventario.'); this.open(row.order.id); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  pay(amount: string, methodId: string): void {
    const row = this.detail();
    if (!row || !amount || !methodId) return;
    this.api.payPurchase(row.order.id, { version: row.order.version, amount: Number(amount), methodId: Number(methodId) }).subscribe({
      next: () => { this.session.toast('success', 'Pago al proveedor registrado en caja, sin duplicarlo como gasto.'); this.open(row.order.id); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  async cancel(): Promise<void> {
    const row = this.detail();
    if (!row) return;
    const reason = window.prompt('Motivo de la cancelación')?.trim();
    if (!reason) return;
    this.api.cancelPurchase(row.order.id, row.order.version, reason).subscribe({
      next: () => { this.session.toast('success', 'Orden cancelada.'); this.open(row.order.id); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }
}
