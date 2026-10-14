import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format, localToday, shiftDate } from '../../core/format';
import { IncomeStatement, ReportTable } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-reports',
  imports: [ReactiveFormsModule],
  templateUrl: './reports.html',
})
export class ReportsPage {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  readonly table = signal<ReportTable | null>(null);
  readonly income = signal<IncomeStatement | null>(null);
  readonly loading = signal(false);
  readonly types = [
    ['sales-day', 'Ventas por día'],
    ['sales-product', 'Ventas por producto'],
    ['sales-category', 'Ventas por categoría'],
    ['sales-waiter', 'Ventas por mesero'],
    ['sales-cashier', 'Cobros por cajero'],
    ['sales-table', 'Ventas por mesa'],
    ['sales-payment', 'Cobros por método'],
    ['discounts', 'Descuentos'],
    ['cancellations', 'Cancelaciones'],
    ['prep-times', 'Tiempos de preparación'],
    ['occupancy', 'Ocupación de mesas'],
    ['stock', 'Existencias'],
    ['movements', 'Movimientos de inventario'],
    ['waste', 'Mermas'],
    ['purchases', 'Compras por proveedor'],
    ['margins', 'Costos y márgenes'],
    ['expenses', 'Gastos por categoría'],
    ['cash', 'Cierres de caja'],
  ];
  readonly form = inject(FormBuilder).nonNullable.group({
    type: ['sales-day'],
    preset: ['month'],
    from: [''],
    to: [''],
  });

  constructor() {
    this.apply('month');
    this.form.controls.preset.valueChanges.subscribe((preset) => { if (preset !== 'custom') this.apply(preset); });
  }

  apply(preset: string): void {
    const zone = this.session.profile()?.timezone || 'America/Santo_Domingo';
    const today = localToday(zone);
    let from = today;
    if (preset === 'week') from = shiftDate(today, -6);
    if (preset === 'month') from = today.slice(0, 8) + '01';
    if (preset === 'quarter') {
      const month = Number(today.slice(5, 7));
      from = `${today.slice(0, 5)}${String(month - ((month - 1) % 3)).padStart(2, '0')}-01`;
    }
    if (preset === 'year') from = `${today.slice(0, 4)}-01-01`;
    this.form.patchValue({ from, to: today }, { emitEvent: false });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const value = this.form.getRawValue();
    const branch = this.session.branchId();
    if (value.type === 'income-statement') {
      this.api.income(branch, value.from, value.to).subscribe({
        next: (row) => { this.income.set(row); this.table.set(null); this.loading.set(false); },
        error: (error: unknown) => { this.loading.set(false); this.session.toast('error', errorMessage(error)); },
      });
      return;
    }
    this.api.report(value.type, branch, value.from, value.to).subscribe({
      next: (row) => { this.table.set(row); this.income.set(null); this.loading.set(false); },
      error: (error: unknown) => { this.loading.set(false); this.session.toast('error', errorMessage(error)); },
    });
  }

  exportCsv(): void { this.export('csv'); }
  exportPdf(): void { this.export('pdf'); }

  private export(format: 'csv' | 'pdf'): void {
    const value = this.form.getRawValue();
    this.api.exportReport(value.type, this.session.branchId(), value.from, value.to, format).subscribe({
      next: (blob) => this.download(blob, `${value.type}.${format}`),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  printPdf(): void {
    window.print();
  }

  private download(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    URL.revokeObjectURL(url);
  }
}
