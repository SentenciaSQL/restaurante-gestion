import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format } from '../../core/format';
import { Ingredient, Modifier, Product, Station, TaxRate, Unit } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-menu',
  imports: [ReactiveFormsModule],
  templateUrl: './menu.html',
})
export class MenuPage {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  readonly categories = signal<{ id: number; name: string; active: boolean; sortOrder: number }[]>([]);
  readonly products = signal<Product[]>([]);
  readonly modifiers = signal<Modifier[]>([]);
  readonly taxes = signal<TaxRate[]>([]);
  readonly stations = signal<Station[]>([]);
  readonly ingredients = signal<Ingredient[]>([]);
  readonly units = signal<Unit[]>([]);
  readonly query = signal('');
  readonly categoryForm = inject(FormBuilder).nonNullable.group({ name: ['', Validators.required] });
  readonly modifierForm = inject(FormBuilder).nonNullable.group({
    id: [null as number | null],
    name: ['', Validators.required],
    price: [0, Validators.min(0)],
  });
  readonly productForm = inject(FormBuilder).nonNullable.group({
    id: [null as number | null],
    categoryId: [null as number | null, Validators.required],
    sku: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    imageUrl: [''],
    productType: ['FOOD', Validators.required],
    basePrice: [0, Validators.min(0)],
    taxRateId: [null as number | null, Validators.required],
    prepMinutes: [10, Validators.min(0)],
    stationId: [null as number | null],
    available: [true],
    active: [true],
    trackInventory: [true],
  });
  readonly variantName = signal('');
  readonly variantDelta = signal(0);
  readonly variants = signal<{ name: string; priceDelta: number; active: boolean; sortOrder: number }[]>([]);
  readonly modifierIds = signal<number[]>([]);
  readonly recipe = signal<{ ingredientId: number; quantity: number; unitId: number }[]>([]);

  constructor() {
    this.reload();
    this.api.taxes().subscribe({ next: (rows) => this.taxes.set(rows) });
    this.api.units().subscribe({ next: (rows) => this.units.set(rows) });
    this.api.ingredients().subscribe({ next: (rows) => this.ingredients.set(rows) });
    const branch = this.session.branchId();
    if (branch) this.api.stations(branch).subscribe({ next: (rows) => this.stations.set(rows) });
  }

  reload(): void {
    this.api.categories().subscribe({ next: (rows) => this.categories.set(rows) });
    this.api.products().subscribe({ next: (rows) => this.products.set(rows) });
    this.api.modifiers().subscribe({ next: (rows) => this.modifiers.set(rows) });
  }

  visible(): Product[] {
    const text = this.query().trim().toLowerCase();
    return this.products().filter((product) => !text || product.name.toLowerCase().includes(text) || product.sku.toLowerCase().includes(text));
  }

  categoryName(id: number): string {
    return this.categories().find((row) => row.id === id)?.name ?? '';
  }

  saveCategory(): void {
    if (this.categoryForm.invalid) return;
    this.api.saveCategory({ name: this.categoryForm.getRawValue().name, active: true, sortOrder: this.categories().length + 1 }).subscribe({
      next: () => {
        this.categoryForm.reset({ name: '' });
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  saveModifier(): void {
    if (this.modifierForm.invalid) return;
    const value = this.modifierForm.getRawValue();
    this.api.saveModifier({ id: value.id ?? undefined, name: value.name, price: value.price, active: true }).subscribe({
      next: () => {
        this.modifierForm.reset({ id: null, name: '', price: 0 });
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  edit(product: Product): void {
    this.api.product(product.id).subscribe({
      next: (detail) => {
        this.productForm.patchValue({
          ...detail.product,
          description: detail.product.description ?? '',
          imageUrl: detail.product.imageUrl ?? '',
        });
        this.variants.set(detail.variants.map((row) => ({ name: row.name, priceDelta: Number(row.priceDelta), active: row.active, sortOrder: row.sortOrder })));
        this.modifierIds.set(detail.modifierIds);
        this.recipe.set(detail.recipe.map((row) => ({ ingredientId: row.ingredientId, quantity: Number(row.quantity), unitId: row.unitId })));
      },
    });
  }

  addVariant(): void {
    if (!this.variantName().trim()) return;
    this.variants.update((rows) => [...rows, { name: this.variantName().trim(), priceDelta: this.variantDelta(), active: true, sortOrder: rows.length + 1 }]);
    this.variantName.set('');
    this.variantDelta.set(0);
  }

  toggleModifier(id: number): void {
    this.modifierIds.update((ids) => (ids.includes(id) ? ids.filter((item) => item !== id) : [...ids, id]));
  }

  addRecipe(ingredientId: string, quantity: string, unitId: string): void {
    if (!ingredientId || !unitId || !quantity) return;
    this.recipe.update((rows) => [...rows, { ingredientId: Number(ingredientId), quantity: Number(quantity), unitId: Number(unitId) }]);
  }

  saveProduct(): void {
    if (this.productForm.invalid) return;
    const value = this.productForm.getRawValue();
    this.api.saveProduct({
      ...value,
      id: value.id ?? undefined,
      categoryId: value.categoryId ?? undefined,
      taxRateId: value.taxRateId ?? undefined,
      stationId: value.stationId ?? undefined,
    }, this.variants(), this.modifierIds(), this.recipe()).subscribe({
      next: () => {
        this.session.toast('success', 'Producto guardado. Los pedidos ya creados conservan su precio.');
        this.productForm.reset({ id: null, categoryId: null, sku: '', name: '', description: '', imageUrl: '', productType: 'FOOD', basePrice: 0, taxRateId: null, prepMinutes: 10, stationId: null, available: true, active: true, trackInventory: true });
        this.variants.set([]);
        this.modifierIds.set([]);
        this.recipe.set([]);
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }
}
