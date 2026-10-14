import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Area, DiningTable, Staff } from '../../core/models';
import { Session } from '../../core/session';
import { TABLE, label } from '../../core/labels';
import { Format } from '../../core/format';

@Component({
  selector: 'app-tables',
  imports: [ReactiveFormsModule],
  templateUrl: './tables.html',
})
export class TablesPage {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  readonly areas = signal<Area[]>([]);
  readonly tables = signal<DiningTable[]>([]);
  readonly staff = signal<Staff[]>([]);
  readonly history = signal<{ status: string; openedAt: string; closedAt: string | null; orderId: number | null }[]>([]);
  readonly historyOpen = signal(false);
  readonly loading = signal(true);
  readonly query = signal('');
  readonly areaForm = inject(FormBuilder).nonNullable.group({ name: ['', Validators.required] });
  readonly tableForm = inject(FormBuilder).nonNullable.group({
    id: [null as number | null],
    areaId: [null as number | null, Validators.required],
    tableNumber: ['', Validators.required],
    capacity: [4, [Validators.required, Validators.min(1)]],
    notes: [''],
  });
  readonly reserveForm = inject(FormBuilder).nonNullable.group({
    tableId: [0],
    customerName: ['', Validators.required],
    phone: [''],
    partySize: [2, Validators.min(1)],
    from: ['', Validators.required],
    until: ['', Validators.required],
    notes: [''],
  });
  label = label;
  tableStatus = TABLE;

  constructor() {
    this.reload();
    if (this.session.can('tables.operate')) {
      this.api.staff().subscribe({ next: (rows) => this.staff.set(rows.filter((row) => row.roles.includes('WAITER') || row.roles.includes('ADMIN') || row.roles.includes('MANAGER'))) });
    }
  }

  reload(): void {
    const branch = this.session.branchId();
    if (!branch) {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.api.areas(branch).subscribe({
      next: (areas) => this.areas.set(areas),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
    this.api.tables(branch).subscribe({
      next: (tables) => {
        this.tables.set(tables);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.session.toast('error', errorMessage(error));
      },
    });
  }

  visible(): DiningTable[] {
    const text = this.query().trim().toLowerCase();
    return this.tables().filter((table) => !text || table.tableNumber.toLowerCase().includes(text) || this.areaName(table.areaId).toLowerCase().includes(text));
  }

  areaName(id: number): string {
    return this.areas().find((area) => area.id === id)?.name ?? 'Área';
  }

  waiterName(id: number | null): string {
    if (!id) return 'Sin asignar';
    return this.staff().find((person) => person.id === id)?.fullName ?? 'Asignado';
  }

  saveArea(): void {
    const branch = this.session.branchId();
    if (!branch || this.areaForm.invalid) return;
    this.api.saveArea({ branchId: branch, name: this.areaForm.getRawValue().name, sortOrder: this.areas().length + 1, active: true }).subscribe({
      next: () => {
        this.areaForm.reset({ name: '' });
        this.session.toast('success', 'Área guardada.');
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  edit(table: DiningTable): void {
    this.tableForm.patchValue({ ...table, notes: table.notes ?? '' });
  }

  saveTable(): void {
    if (this.tableForm.invalid) return;
    const value = this.tableForm.getRawValue();
    this.api.saveTable({ ...value, id: value.id ?? undefined, areaId: value.areaId ?? undefined, active: true, status: 'AVAILABLE' }).subscribe({
      next: () => {
        this.tableForm.reset({ id: null, areaId: null, tableNumber: '', capacity: 4, notes: '' });
        this.session.toast('success', 'Mesa guardada.');
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  assign(table: DiningTable, waiterId: string): void {
    if (!waiterId) return;
    this.api.assignWaiter(table.id, Number(waiterId)).subscribe({
      next: () => this.reload(),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  async release(table: DiningTable): Promise<void> {
    const ok = await this.session.confirm('Liberar mesa', `La mesa ${table.tableNumber} quedará disponible.`, 'Liberar');
    if (!ok) return;
    this.api.releaseTable(table.id).subscribe({
      next: () => {
        this.session.toast('success', 'Mesa liberada.');
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  openAccount(table: DiningTable): void {
    const branch = this.session.branchId();
    if (!branch) return;
    this.api.createOrder({ branchId: branch, orderType: 'DINE_IN', tableId: table.id, guestCount: table.capacity }).subscribe({
      next: (order) => void this.router.navigate(['/pos', order.id]),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  openHistory(table: DiningTable): void {
    this.api.tableHistory(table.id).subscribe({
      next: (rows) => {
        this.history.set(rows);
        this.historyOpen.set(true);
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  saveReserve(): void {
    if (this.reserveForm.invalid) return;
    const value = this.reserveForm.getRawValue();
    this.api.reserve(value.tableId, {
      customerName: value.customerName,
      phone: value.phone,
      partySize: String(value.partySize),
      from: new Date(value.from).toISOString(),
      until: new Date(value.until).toISOString(),
      notes: value.notes,
    }).subscribe({
      next: () => {
        this.reserveForm.reset({ tableId: 0, customerName: '', phone: '', partySize: 2, from: '', until: '', notes: '' });
        this.session.toast('success', 'Reserva registrada.');
        this.reload();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }
}
