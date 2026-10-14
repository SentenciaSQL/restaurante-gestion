import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Branch, ExpenseCategory, PaymentMethod, Restaurant, Sequence, Setting, Station, TaxRate, Unit } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './settings.html',
})
export class SettingsPage {
  readonly session = inject(Session);
  private readonly api = inject(Api);
  readonly tab = signal('restaurant');
  readonly restaurant = signal<Restaurant | null>(null);
  readonly branches = signal<Branch[]>([]);
  readonly taxes = signal<TaxRate[]>([]);
  readonly methods = signal<PaymentMethod[]>([]);
  readonly units = signal<Unit[]>([]);
  readonly stations = signal<Station[]>([]);
  readonly categories = signal<ExpenseCategory[]>([]);
  readonly parameters = signal<Setting[]>([]);
  readonly sequences = signal<Sequence[]>([]);
  readonly restaurantForm = inject(FormBuilder).nonNullable.group({
    name: ['', Validators.required],
    legalName: [''],
    taxId: [''],
    phone: [''],
    email: [''],
    address: [''],
    logoUrl: [''],
    countryCode: ['DO', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
    currencyCode: ['DOP', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    timezone: ['America/Santo_Domingo', Validators.required],
    locale: ['es-DO', Validators.required],
  });
  readonly branchForm = inject(FormBuilder).nonNullable.group({ code: ['', Validators.required], name: ['', Validators.required], address: [''], phone: [''], active: [true] });
  readonly taxForm = inject(FormBuilder).nonNullable.group({ name: ['', Validators.required], rate: [0, Validators.min(0)], active: [true] });
  readonly methodForm = inject(FormBuilder).nonNullable.group({ code: ['', Validators.required], name: ['', Validators.required], affectsCash: [false], requiresReference: [false], active: [true] });
  readonly unitForm = inject(FormBuilder).nonNullable.group({ code: ['', Validators.required], name: ['', Validators.required], family: ['COUNT', Validators.required], factorToBase: [1, Validators.min(0.000001)] });
  readonly stationForm = inject(FormBuilder).nonNullable.group({ code: ['', Validators.required], name: ['', Validators.required] });
  readonly categoryForm = inject(FormBuilder).nonNullable.group({ name: ['', Validators.required], classification: ['OPERATING', Validators.required] });
  readonly passwordForm = inject(FormBuilder).nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor() { this.reload(); }

  reload(): void {
    if (this.session.canAny('settings.manage', 'dashboard.view')) {
      this.api.restaurant().subscribe({ next: (row) => { this.restaurant.set(row); this.restaurantForm.patchValue({
        ...row,
        legalName: row.legalName ?? '',
        taxId: row.taxId ?? '',
        phone: row.phone ?? '',
        email: row.email ?? '',
        address: row.address ?? '',
        logoUrl: row.logoUrl ?? '',
      }); } });
    }
    this.api.branches().subscribe({ next: (rows) => this.branches.set(rows) });
    this.api.taxes().subscribe({ next: (rows) => this.taxes.set(rows) });
    this.api.methods().subscribe({ next: (rows) => this.methods.set(rows) });
    this.api.units().subscribe({ next: (rows) => this.units.set(rows) });
    this.api.expenseCategories().subscribe({ next: (rows) => this.categories.set(rows) });
    const branch = this.session.branchId();
    if (branch) {
      this.api.stations(branch).subscribe({ next: (rows) => this.stations.set(rows) });
      if (this.session.can('settings.manage')) this.api.sequences(branch).subscribe({ next: (rows) => this.sequences.set(rows) });
    }
    if (this.session.canAny('settings.manage', 'settings.operations')) {
      this.api.parameters().subscribe({ next: (rows) => this.parameters.set(rows), error: () => undefined });
    }
  }

  saveRestaurant(): void {
    const current = this.restaurant();
    if (!current || this.restaurantForm.invalid) return;
    this.api.saveRestaurant({ ...current, ...this.restaurantForm.getRawValue() }).subscribe({
      next: (row) => { this.restaurant.set(row); this.session.toast('success', 'Datos del restaurante actualizados.'); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  saveBranch(): void {
    if (this.branchForm.invalid) return;
    this.api.saveBranch({ ...this.branchForm.getRawValue(), active: true }).subscribe({
      next: () => { this.session.toast('success', 'Sucursal creada con almacén y numeración.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  saveTax(): void { this.post(this.taxForm.invalid, () => this.api.saveTax(this.taxForm.getRawValue())); }
  saveMethod(): void { this.post(this.methodForm.invalid, () => this.api.saveMethod(this.methodForm.getRawValue())); }
  saveUnit(): void { this.post(this.unitForm.invalid, () => this.api.saveUnit({ ...this.unitForm.getRawValue(), active: true })); }
  saveStation(): void {
    const branch = this.session.branchId();
    if (!branch || this.stationForm.invalid) return;
    this.api.saveStation({ ...this.stationForm.getRawValue(), branchId: branch, active: true }).subscribe({ next: () => this.reload(), error: (error: unknown) => this.session.toast('error', errorMessage(error)) });
  }
  saveCategory(): void { this.post(this.categoryForm.invalid, () => this.api.saveExpenseCategory({ ...this.categoryForm.getRawValue(), active: true })); }

  saveParameter(key: string, value: string): void {
    this.api.saveParameter(key, value).subscribe({
      next: () => this.session.toast('success', 'Parámetro actualizado.'),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  saveSequence(row: Sequence, prefix: string, padding: string): void {
    this.api.saveSequence({ ...row, prefix, padding: Number(padding) }).subscribe({
      next: () => { this.session.toast('success', 'Numeración actualizada.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;
    const value = this.passwordForm.getRawValue();
    this.api.changePassword(value.currentPassword, value.newPassword).subscribe({
      next: () => { this.passwordForm.reset(); this.session.toast('success', 'Contraseña actualizada.'); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  private post(invalid: boolean, call: () => { subscribe: (o: { next: () => void; error: (e: unknown) => void }) => void }): void {
    if (invalid) return;
    call().subscribe({ next: () => { this.session.toast('success', 'Guardado.'); this.reload(); }, error: (error: unknown) => this.session.toast('error', errorMessage(error)) });
  }
}
