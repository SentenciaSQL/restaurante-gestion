import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format } from '../../core/format';
import { ACCOUNT, ORDER_TYPE, PREP, label } from '../../core/labels';
import { OrderSummary } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-orders',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './orders.html',
})
export class OrdersPage {
  readonly format = inject(Format);
  readonly session = inject(Session);
  private readonly api = inject(Api);
  readonly rows = signal<OrderSummary[]>([]);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    q: [''],
    accountStatus: [''],
    orderType: [''],
    page: [0],
  });
  label = label;
  types = ORDER_TYPE;
  prep = PREP;
  account = ACCOUNT;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const value = this.form.getRawValue();
    this.api.orders({ ...value, branchId: this.session.branchId(), size: 20 }).subscribe({
      next: (page) => {
        this.rows.set(page.items);
        this.total.set(page.total);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.session.toast('error', errorMessage(error));
      },
    });
  }

  turn(delta: number): void {
    const next = Math.max(0, this.form.controls.page.value + delta);
    this.form.controls.page.setValue(next);
    this.load();
  }
}
