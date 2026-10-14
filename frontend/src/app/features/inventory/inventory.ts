import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format } from '../../core/format';
import { Ingredient, StockLevel, StockMovement, Unit, Warehouse } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-inventory',
  imports: [ReactiveFormsModule],
  templateUrl: './inventory.html',
})
export class InventoryPage {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  readonly ingredients = signal<Ingredient[]>([]);
  readonly stock = signal<StockLevel[]>([]);
  readonly movements = signal<StockMovement[]>([]);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly units = signal<Unit[]>([]);
  readonly query = signal('');
  readonly itemForm = inject(FormBuilder).nonNullable.group({
    id: [null as number | null],
    sku: ['', Validators.required],
    name: ['', Validators.required],
    unitId: [null as number | null, Validators.required],
    minStock: [0, Validators.min(0)],
    active: [true],
  });
  readonly moveForm = inject(FormBuilder).nonNullable.group({
    warehouseId: [null as number | null, Validators.required],
    ingredientId: [null as number | null, Validators.required],
    type: ['ADJUSTMENT', Validators.required],
    direction: ['IN', Validators.required],
    quantity: [1, Validators.min(0.001)],
    unitCost: [0, Validators.min(0)],
    reason: ['', Validators.required],
  });
  readonly transferForm = inject(FormBuilder).nonNullable.group({
    sourceWarehouseId: [null as number | null, Validators.required],
    targetWarehouseId: [null as number | null, Validators.required],
    ingredientId: [null as number | null, Validators.required],
    quantity: [1, Validators.min(0.001)],
    reason: ['', Validators.required],
  });

  constructor() {
    this.api.units().subscribe({ next: (rows) => this.units.set(rows) });
    this.reload();
  }

  reload(): void {
    this.api.ingredients().subscribe({ next: (rows) => this.ingredients.set(rows) });
    const branch = this.session.branchId();
    if (!branch) return;
    this.api.warehouses(branch).subscribe({ next: (rows) => this.warehouses.set(rows) });
    this.api.stock(branch).subscribe({ next: (rows) => this.stock.set(rows) });
    this.api.movements(branch).subscribe({ next: (rows) => this.movements.set(rows) });
  }

  nameOf(id: number): string {
    return this.ingredients().find((row) => row.id === id)?.name ?? String(id);
  }

  qtyOf(id: number): number {
    return this.stock().filter((row) => row.ingredientId === id).reduce((sum, row) => sum + Number(row.quantity), 0);
  }

  costOf(id: number): number {
    const row = this.stock().find((item) => item.ingredientId === id);
    return row ? Number(row.averageCost) : 0;
  }

  visible(): Ingredient[] {
    const text = this.query().trim().toLowerCase();
    return this.ingredients().filter((row) => !text || row.name.toLowerCase().includes(text) || row.sku.toLowerCase().includes(text));
  }

  valuation(): number {
    return this.stock().reduce((sum, row) => sum + Number(row.quantity) * Number(row.averageCost), 0);
  }

  saveItem(): void {
    if (this.itemForm.invalid) return;
    const value = this.itemForm.getRawValue();
    this.api.saveIngredient({ ...value, id: value.id ?? undefined, unitId: value.unitId!, minStock: value.minStock, active: value.active, sku: value.sku, name: value.name }).subscribe({
      next: () => {
        this.itemForm.reset({ id: null, sku: '', name: '', unitId: null, minStock: 0, active: true });
        this.session.toast('success', 'Ingrediente guardado.');
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  move(): void {
    const branch = this.session.branchId();
    if (!branch || this.moveForm.invalid) return;
    this.api.moveStock({ branchId: branch, ...this.moveForm.getRawValue() }).subscribe({
      next: () => {
        this.session.toast('success', 'Movimiento registrado.');
        this.moveForm.controls.reason.setValue('');
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  transfer(): void {
    const branch = this.session.branchId();
    if (!branch || this.transferForm.invalid) return;
    this.api.transferStock({ branchId: branch, ...this.transferForm.getRawValue() }).subscribe({
      next: () => {
        this.session.toast('success', 'Transferencia registrada.');
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }
}
