import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format, localToday } from '../../core/format';
import { EXPENSE, label } from '../../core/labels';
import { Expense, ExpenseCategory, PaymentMethod, Supplier } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-expenses',
  imports: [ReactiveFormsModule],
  templateUrl: './expenses.html',
})
export class ExpensesPage {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  readonly rows = signal<Expense[]>([]);
  readonly categories = signal<ExpenseCategory[]>([]);
  readonly suppliers = signal<Supplier[]>([]);
  readonly methods = signal<PaymentMethod[]>([]);
  readonly status = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    categoryId: [null as number | null, Validators.required],
    supplierId: [null as number | null],
    description: ['', Validators.required],
    expenseDate: [localToday('America/Santo_Domingo'), Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    paymentMethodId: [null as number | null],
    receiptRef: [''],
    notes: [''],
    payNow: [false],
  });
  label = label;
  states = EXPENSE;

  constructor() {
    this.api.expenseCategories().subscribe({ next: (rows) => this.categories.set(rows) });
    this.api.suppliers().subscribe({ next: (rows) => this.suppliers.set(rows) });
    this.api.methods().subscribe({ next: (rows) => this.methods.set(rows.filter((row) => row.active)) });
    this.reload();
  }

  reload(): void {
    this.api.expenses({ branchId: this.session.branchId(), status: this.status() }).subscribe({
      next: (rows) => this.rows.set(rows),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  categoryName(id: number): string { return this.categories().find((row) => row.id === id)?.name ?? ''; }

  save(): void {
    const branch = this.session.branchId();
    if (!branch || this.form.invalid) return;
    const value = this.form.getRawValue();
    if (value.payNow && !value.paymentMethodId) {
      this.session.toast('error', 'Elija el método si va a pagar ahora.');
      return;
    }
    this.api.createExpense({
      branchId: branch,
      categoryId: value.categoryId,
      supplierId: value.supplierId,
      description: value.description,
      expenseDate: value.expenseDate,
      amount: value.amount,
      currencyCode: this.session.profile()?.currency || 'DOP',
      paymentMethodId: value.paymentMethodId,
      receiptRef: value.receiptRef,
      notes: value.notes,
    }, value.payNow).subscribe({
      next: () => { this.session.toast('success', value.payNow ? 'Gasto pagado y registrado una sola vez.' : 'Gasto pendiente de pago.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  pay(row: Expense, methodId: string): void {
    if (!methodId) return;
    this.api.payExpense(row.id, Number(methodId)).subscribe({
      next: () => { this.session.toast('success', 'Pago registrado.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  async voidExpense(row: Expense): Promise<void> {
    const reason = window.prompt('Motivo de la anulación')?.trim();
    if (!reason) return;
    this.api.voidExpense(row.id, reason).subscribe({
      next: () => { this.session.toast('success', 'Gasto anulado. El historial se conserva.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }
}
