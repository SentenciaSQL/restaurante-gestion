import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Supplier } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-suppliers',
  imports: [ReactiveFormsModule],
  templateUrl: './suppliers.html',
})
export class SuppliersPage {
  readonly session = inject(Session);
  private readonly api = inject(Api);
  readonly rows = signal<Supplier[]>([]);
  readonly form = inject(FormBuilder).nonNullable.group({
    id: [null as number | null],
    name: ['', Validators.required],
    taxId: [''],
    phone: [''],
    email: [''],
    address: [''],
    notes: [''],
    active: [true],
  });

  constructor() { this.reload(); }
  reload(): void { this.api.suppliers().subscribe({ next: (rows) => this.rows.set(rows), error: (error: unknown) => this.session.toast('error', errorMessage(error)) }); }
  edit(row: Supplier): void {
    this.form.patchValue({ ...row, taxId: row.taxId ?? '', phone: row.phone ?? '', email: row.email ?? '', address: row.address ?? '', notes: row.notes ?? '' });
  }
  save(): void {
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.api.saveSupplier({ ...value, id: value.id ?? undefined, taxId: value.taxId || null, phone: value.phone || null, email: value.email || null, address: value.address || null, notes: value.notes || null, active: value.active, name: value.name }).subscribe({
      next: () => { this.form.reset({ id: null, name: '', taxId: '', phone: '', email: '', address: '', notes: '', active: true }); this.session.toast('success', 'Proveedor guardado.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }
}
