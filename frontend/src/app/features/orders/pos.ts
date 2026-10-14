import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format } from '../../core/format';
import { ACCOUNT, LINE, ORDER_TYPE, PREP, label } from '../../core/labels';
import { DiningTable, Modifier, OrderView, PaymentMethod, Product, Variant } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-pos',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './pos.html',
})
export class PosPage {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly order = signal<OrderView | null>(null);
  readonly products = signal<Product[]>([]);
  readonly modifiers = signal<Modifier[]>([]);
  readonly methods = signal<PaymentMethod[]>([]);
  readonly tables = signal<DiningTable[]>([]);
  readonly variants = signal<Variant[]>([]);
  readonly openOrders = signal<{ id: number; orderNumber: string }[]>([]);
  readonly search = signal('');
  readonly selected = signal<Product | null>(null);
  readonly chosenModifiers = signal<number[]>([]);
  readonly receipt = signal<Record<string, unknown> | null>(null);
  readonly payKey = signal(crypto.randomUUID());
  readonly refundKey = signal(crypto.randomUUID());
  label = label;
  types = ORDER_TYPE;
  prep = PREP;
  account = ACCOUNT;
  lineStatus = LINE;
  readonly start = inject(FormBuilder).nonNullable.group({
    orderType: ['DINE_IN', Validators.required],
    tableId: [null as number | null],
    customerName: [''],
    customerPhone: [''],
    deliveryAddress: [''],
    guestCount: [2],
  });
  readonly line = inject(FormBuilder).nonNullable.group({
    variantId: [null as number | null],
    quantity: [1, [Validators.required, Validators.min(0.001)]],
    notes: [''],
  });
  readonly pay = inject(FormBuilder).nonNullable.group({
    methodId: [null as number | null, Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    tipAmount: [0, Validators.min(0)],
    reference: [''],
  });
  readonly discount = inject(FormBuilder).nonNullable.group({
    discountType: ['AMOUNT', Validators.required],
    value: [0, Validators.min(0)],
    reason: ['', Validators.required],
  });
  readonly splitIds = signal<number[]>([]);

  constructor() {
    this.api.products().subscribe({ next: (rows) => this.products.set(rows.filter((row) => row.active && row.available)) });
    this.api.modifiers().subscribe({ next: (rows) => this.modifiers.set(rows.filter((row) => row.active)) });
    this.api.methods().subscribe({ next: (rows) => this.methods.set(rows.filter((row) => row.active)) });
    const branch = this.session.branchId();
    if (branch) {
      this.api.tables(branch).subscribe({ next: (rows) => this.tables.set(rows) });
    }
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) this.load(Number(id));
    });
  }

  filtered(): Product[] {
    const text = this.search().trim().toLowerCase();
    return this.products().filter((product) => !text || product.name.toLowerCase().includes(text) || product.sku.toLowerCase().includes(text));
  }

  load(id: number): void {
    this.api.order(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.pay.patchValue({ amount: Number(order.balance) });
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  create(): void {
    const branch = this.session.branchId();
    if (!branch) {
      this.session.toast('error', 'Seleccione una sucursal.');
      return;
    }
    const value = this.start.getRawValue();
    if (value.orderType === 'DINE_IN' && !value.tableId) {
      this.session.toast('error', 'Seleccione la mesa.');
      return;
    }
    this.api.createOrder({ branchId: branch, ...value }).subscribe({
      next: (order) => void this.router.navigate(['/pos', order.id]),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  pick(product: Product): void {
    this.selected.set(product);
    this.chosenModifiers.set([]);
    this.variants.set([]);
    this.line.reset({ variantId: null, quantity: 1, notes: '' });
    this.api.variants(product.id).subscribe({ next: (rows) => this.variants.set(rows.filter((row) => row.active)) });
  }

  toggleModifier(id: number): void {
    this.chosenModifiers.update((ids) => (ids.includes(id) ? ids.filter((item) => item !== id) : [...ids, id]));
  }

  addLine(): void {
    const current = this.order();
    const product = this.selected();
    if (!current || !product || this.line.invalid) return;
    const value = this.line.getRawValue();
    this.api.addLine(current.id, {
      version: current.version,
      productId: product.id,
      variantId: value.variantId,
      quantity: value.quantity,
      modifierIds: this.chosenModifiers(),
      notes: value.notes,
    }).subscribe({
      next: (order) => {
        this.order.set(order);
        this.selected.set(null);
        this.session.toast('success', 'Producto agregado.');
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  async cancelLine(lineId: number): Promise<void> {
    const current = this.order();
    if (!current) return;
    const ok = await this.session.confirm('Cancelar artículo', 'Si ya se descontó inventario, se repondrá. La cantidad enviada no se edita: cancele y vuelva a agregar.', 'Cancelar artículo');
    if (!ok) return;
    const reason = window.prompt('Motivo de la cancelación')?.trim();
    if (!reason) return;
    this.api.cancelLine(current.id, lineId, { version: current.version, reason }).subscribe({
      next: (order) => this.order.set(order),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  send(): void {
    const current = this.order();
    if (!current) return;
    this.api.sendKitchen(current.id, { version: current.version, lineIds: [] }).subscribe({
      next: (order) => {
        this.order.set(order);
        this.session.toast('success', 'Comanda enviada a cocina.');
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  deliver(): void {
    const current = this.order();
    if (!current) return;
    this.api.deliver(current.id, { version: current.version, lineIds: [] }).subscribe({
      next: (order) => this.order.set(order),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  applyDiscount(): void {
    const current = this.order();
    if (!current || this.discount.invalid) return;
    this.api.discount(current.id, { version: current.version, ...this.discount.getRawValue() }).subscribe({
      next: (order) => {
        this.order.set(order);
        this.session.toast('success', 'Descuento aplicado. El anterior queda en el historial.');
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  collect(): void {
    const current = this.order();
    if (!current || this.pay.invalid) return;
    const value = this.pay.getRawValue();
    this.api.pay(current.id, { version: current.version, ...value }, this.payKey()).subscribe({
      next: (order) => {
        this.order.set(order);
        this.payKey.set(crypto.randomUUID());
        this.pay.patchValue({ amount: Number(order.balance), reference: '' });
        this.session.toast('success', 'Cobro registrado.');
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  async voidPay(paymentId: number): Promise<void> {
    const current = this.order();
    if (!current) return;
    const reason = window.prompt('Motivo de la anulación')?.trim();
    if (!reason) return;
    this.api.voidPayment(current.id, paymentId, { version: current.version, reason }).subscribe({
      next: (order) => this.order.set(order),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  refund(paymentId: number): void {
    const current = this.order();
    if (!current) return;
    const amount = Number(window.prompt('Importe a devolver') ?? '');
    const reason = window.prompt('Motivo de la devolución')?.trim();
    if (!amount || !reason) return;
    const restock = window.confirm('¿Reingresar el costo de los artículos entregados al inventario?');
    this.api.refund(current.id, {
      version: current.version,
      paymentId,
      amount,
      reason,
      restock,
      lineIds: current.lines.filter((line) => line.status === 'DELIVERED').map((line) => line.id),
    }, this.refundKey()).subscribe({
      next: (order) => {
        this.order.set(order);
        this.refundKey.set(crypto.randomUUID());
        this.session.toast('success', 'Devolución registrada en el período actual.');
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  close(): void {
    const current = this.order();
    if (!current) return;
    this.api.closeOrder(current.id, current.version).subscribe({
      next: (order) => {
        this.order.set(order);
        this.session.toast('success', 'Cuenta cerrada. El ingreso queda reconocido en este momento.');
        this.showReceipt();
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  async cancelOrder(): Promise<void> {
    const current = this.order();
    if (!current) return;
    const reason = window.prompt('Motivo de la cancelación de la cuenta')?.trim();
    if (!reason) return;
    this.api.cancelOrder(current.id, { version: current.version, reason }).subscribe({
      next: (order) => this.order.set(order),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  transfer(tableId: string): void {
    const current = this.order();
    if (!current || !tableId) return;
    this.api.transfer(current.id, { version: current.version, tableId: Number(tableId) }).subscribe({
      next: (order) => this.order.set(order),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  toggleSplit(id: number): void {
    this.splitIds.update((ids) => (ids.includes(id) ? ids.filter((item) => item !== id) : [...ids, id]));
  }

  split(tableId: string): void {
    const current = this.order();
    if (!current || !this.splitIds().length) return;
    this.api.split(current.id, { version: current.version, lineIds: this.splitIds(), targetTableId: tableId ? Number(tableId) : null }).subscribe({
      next: (order) => {
        this.splitIds.set([]);
        this.order.set(order);
        this.session.toast('success', 'Cuenta dividida. La cuenta nueva quedó en la mesa o para llevar.');
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  prepareMerge(): void {
    const branch = this.session.branchId();
    if (!branch) return;
    this.api.orders({ branchId: branch, accountStatus: 'OPEN', page: 0, size: 50 }).subscribe({
      next: (page) => this.openOrders.set(page.items.filter((item) => item.id !== this.order()?.id).map((item) => ({ id: item.id, orderNumber: item.orderNumber }))),
    });
  }

  merge(sourceId: string): void {
    const current = this.order();
    if (!current || !sourceId) return;
    this.api.merge(current.id, { version: current.version, sourceOrderId: Number(sourceId) }).subscribe({
      next: (order) => this.order.set(order),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  showReceipt(): void {
    const current = this.order();
    if (!current) return;
    this.api.receipt(current.id).subscribe({
      next: (row) => this.receipt.set(row),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  downloadPdf(): void {
    const current = this.order();
    if (!current) return;
    this.api.receiptPdf(current.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${current.orderNumber}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  printReceipt(): void {
    window.print();
  }
}
