import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { Api } from '../../core/api';
import { Format, localToday, shiftDate } from '../../core/format';
import { Dashboard } from '../../core/models';
import { Session } from '../../core/session';
import { errorMessage } from '../../core/error.interceptor';

@Component({
  selector: 'app-dashboard',
  imports: [ReactiveFormsModule],
  templateUrl: './dashboard.html',
})
export class DashboardPage {
  readonly format = inject(Format);
  readonly session = inject(Session);
  private readonly api = inject(Api);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly data = signal<Dashboard | null>(null);
  readonly salesCanvas = viewChild<ElementRef<HTMLCanvasElement>>('sales');
  readonly payCanvas = viewChild<ElementRef<HTMLCanvasElement>>('pays');
  private salesChart?: Chart;
  private payChart?: Chart;
  readonly form = inject(FormBuilder).nonNullable.group({
    preset: ['month'],
    from: [''],
    to: [''],
  });

  constructor() {
    this.applyPreset('month');
    this.form.controls.preset.valueChanges.subscribe((preset) => {
      if (preset !== 'custom') this.applyPreset(preset);
    });
  }

  load(): void {
    const branch = this.session.branchId();
    if (branch == null && !this.session.can('branches.switch')) {
      this.loading.set(false);
      this.error.set('Seleccione una sucursal.');
      return;
    }
    this.loading.set(true);
    const value = this.form.getRawValue();
    this.api.dashboard(branch, value.from, value.to).subscribe({
      next: (row) => {
        this.data.set(row);
        this.loading.set(false);
        this.error.set('');
        queueMicrotask(() => this.draw(row));
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  private applyPreset(preset: string): void {
    const zone = this.session.profile()?.timezone || 'America/Santo_Domingo';
    const today = localToday(zone);
    let from = today;
    if (preset === 'week') from = shiftDate(today, -6);
    if (preset === 'month') from = today.slice(0, 8) + '01';
    if (preset === 'quarter') {
      const month = Number(today.slice(5, 7));
      const start = month - ((month - 1) % 3);
      from = `${today.slice(0, 5)}${String(start).padStart(2, '0')}-01`;
    }
    if (preset === 'year') from = today.slice(0, 4) + '-01-01';
    this.form.patchValue({ from, to: today }, { emitEvent: false });
    this.load();
  }

  private draw(row: Dashboard): void {
    const sales = this.salesCanvas()?.nativeElement;
    const pays = this.payCanvas()?.nativeElement;
    this.salesChart?.destroy();
    this.payChart?.destroy();
    if (sales) {
      const config: ChartConfiguration = {
        type: 'bar',
        data: {
          labels: row.salesByDay.map((item) => item.day),
          datasets: [{ label: 'Ventas con impuestos', data: row.salesByDay.map((item) => Number(item.total)), backgroundColor: '#8a4b2f' }],
        },
        options: { responsive: true, plugins: { legend: { display: false } } },
      };
      this.salesChart = new Chart(sales, config);
    }
    if (pays && row.paymentMethods.length) {
      this.payChart = new Chart(pays, {
        type: 'doughnut',
        data: {
          labels: row.paymentMethods.map((item) => item.name),
          datasets: [{ data: row.paymentMethods.map((item) => Number(item.amount)) }],
        },
        options: { responsive: true },
      });
    }
  }
}
