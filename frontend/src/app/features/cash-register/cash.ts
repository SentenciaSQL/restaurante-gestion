import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format } from '../../core/format';
import { CashCurrent, ReportTable } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-cash',
  imports: [ReactiveFormsModule],
  templateUrl: './cash.html',
})
export class CashPage {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  readonly current = signal<CashCurrent | null>(null);
  readonly history = signal<ReportTable | null>(null);
  readonly openForm = inject(FormBuilder).nonNullable.group({ openingFloat: [0, Validators.min(0)], notes: [''] });
  readonly moveForm = inject(FormBuilder).nonNullable.group({
    type: ['PAY_IN', Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    description: ['', Validators.required],
  });
  readonly closeForm = inject(FormBuilder).nonNullable.group({ countedCash: [0, Validators.min(0)], notes: [''] });

  constructor() { this.reload(); }

  reload(): void {
    const branch = this.session.branchId();
    if (!branch) return;
    this.api.cash(branch).subscribe({ next: (row) => this.current.set(row), error: (error: unknown) => this.session.toast('error', errorMessage(error)) });
    this.api.report('cash', branch, '2000-01-01', '2100-01-01').subscribe({ next: (row) => this.history.set(row) });
  }

  open(): void {
    const branch = this.session.branchId();
    if (!branch) return;
    const value = this.openForm.getRawValue();
    this.api.openCash(branch, value.openingFloat, value.notes).subscribe({
      next: () => { this.session.toast('success', 'Caja abierta.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  move(): void {
    const row = this.current()?.session;
    if (!row || this.moveForm.invalid) return;
    const value = this.moveForm.getRawValue();
    this.api.cashMove(row.id, { version: String(row.version), type: value.type, amount: value.amount.toFixed(2), description: value.description }).subscribe({
      next: () => { this.session.toast('success', 'Movimiento registrado.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  close(): void {
    const row = this.current()?.session;
    if (!row) return;
    const value = this.closeForm.getRawValue();
    this.api.closeCash(row.id, { version: String(row.version), countedCash: value.countedCash.toFixed(2), notes: value.notes }).subscribe({
      next: () => { this.session.toast('success', 'Caja cerrada. La diferencia queda en el historial.'); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }
}
