import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Area,
  Branch,
  CashCurrent,
  CashSession,
  Dashboard,
  DiningTable,
  Expense,
  ExpenseCategory,
  IncomeStatement,
  Ingredient,
  KitchenTicket,
  LoginResponse,
  Modifier,
  Notice,
  OrderSummary,
  OrderView,
  Page,
  PaymentMethod,
  Product,
  ProductDetail,
  PurchaseDetail,
  PurchaseOrder,
  RecipeLine,
  ReportTable,
  Restaurant,
  RoleView,
  Sequence,
  Setting,
  Staff,
  Station,
  StockLevel,
  StockMovement,
  Supplier,
  TaxRate,
  Unit,
  UserView,
  Variant,
  Warehouse,
} from './models';

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/auth/login`, { email, password });
  }

  me(): Observable<LoginResponse['user']> {
    return this.http.get<LoginResponse['user']>(`${this.base}/auth/me`);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.base}/auth/password`, { currentPassword, newPassword });
  }

  users(q: string, page: number): Observable<Page<UserView>> {
    return this.http.get<Page<UserView>>(`${this.base}/users`, { params: this.params({ q, page, size: 20 }) });
  }

  saveUser(id: number | null, body: object): Observable<UserView> {
    return id == null
      ? this.http.post<UserView>(`${this.base}/users`, body)
      : this.http.put<UserView>(`${this.base}/users/${id}`, body);
  }

  setUserActive(id: number, active: boolean): Observable<UserView> {
    return this.http.patch<UserView>(`${this.base}/users/${id}/active`, { active });
  }

  roles(): Observable<RoleView[]> {
    return this.http.get<RoleView[]>(`${this.base}/roles`);
  }

  staff(): Observable<Staff[]> {
    return this.http.get<Staff[]>(`${this.base}/staff`);
  }

  branches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(`${this.base}/branches`);
  }

  saveBranch(branch: Partial<Branch>): Observable<Branch> {
    return branch.id
      ? this.http.put<Branch>(`${this.base}/branches/${branch.id}`, branch)
      : this.http.post<Branch>(`${this.base}/branches`, branch);
  }

  restaurant(): Observable<Restaurant> {
    return this.http.get<Restaurant>(`${this.base}/settings/restaurant`);
  }

  saveRestaurant(body: Restaurant): Observable<Restaurant> {
    return this.http.put<Restaurant>(`${this.base}/settings/restaurant`, body);
  }

  taxes(): Observable<TaxRate[]> {
    return this.http.get<TaxRate[]>(`${this.base}/settings/taxes`);
  }

  saveTax(body: Partial<TaxRate>): Observable<TaxRate> {
    return this.http.post<TaxRate>(`${this.base}/settings/taxes`, body);
  }

  methods(): Observable<PaymentMethod[]> {
    return this.http.get<PaymentMethod[]>(`${this.base}/settings/payment-methods`);
  }

  saveMethod(body: Partial<PaymentMethod>): Observable<PaymentMethod> {
    return this.http.post<PaymentMethod>(`${this.base}/settings/payment-methods`, body);
  }

  units(): Observable<Unit[]> {
    return this.http.get<Unit[]>(`${this.base}/settings/units`);
  }

  saveUnit(body: Partial<Unit>): Observable<Unit> {
    return this.http.post<Unit>(`${this.base}/settings/units`, body);
  }

  stations(branchId: number): Observable<Station[]> {
    return this.http.get<Station[]>(`${this.base}/settings/stations`, { params: { branchId } });
  }

  saveStation(body: Partial<Station>): Observable<Station> {
    return this.http.post<Station>(`${this.base}/settings/stations`, body);
  }

  parameters(): Observable<Setting[]> {
    return this.http.get<Setting[]>(`${this.base}/settings/parameters`);
  }

  saveParameter(key: string, value: string): Observable<Setting> {
    return this.http.put<Setting>(`${this.base}/settings/parameters`, { key, value });
  }

  sequences(branchId: number): Observable<Sequence[]> {
    return this.http.get<Sequence[]>(`${this.base}/settings/sequences`, { params: { branchId } });
  }

  saveSequence(body: Sequence): Observable<Sequence> {
    return this.http.put<Sequence>(`${this.base}/settings/sequences`, body);
  }

  expenseCategories(): Observable<ExpenseCategory[]> {
    return this.http.get<ExpenseCategory[]>(`${this.base}/settings/expense-categories`);
  }

  saveExpenseCategory(body: Partial<ExpenseCategory>): Observable<ExpenseCategory> {
    return this.http.post<ExpenseCategory>(`${this.base}/settings/expense-categories`, body);
  }

  areas(branchId: number): Observable<Area[]> {
    return this.http.get<Area[]>(`${this.base}/areas`, { params: { branchId } });
  }

  saveArea(body: Partial<Area>): Observable<Area> {
    return this.http.post<Area>(`${this.base}/areas`, body);
  }

  tables(branchId: number): Observable<DiningTable[]> {
    return this.http.get<DiningTable[]>(`${this.base}/tables`, { params: { branchId } });
  }

  saveTable(body: Partial<DiningTable>): Observable<DiningTable> {
    return this.http.post<DiningTable>(`${this.base}/tables`, body);
  }

  assignWaiter(id: number, waiterId: number): Observable<DiningTable> {
    return this.http.post<DiningTable>(`${this.base}/tables/${id}/assign`, { waiterId });
  }

  releaseTable(id: number): Observable<DiningTable> {
    return this.http.post<DiningTable>(`${this.base}/tables/${id}/release`, {});
  }

  reserve(id: number, body: object): Observable<unknown> {
    return this.http.post(`${this.base}/tables/${id}/reservations`, body);
  }

  tableHistory(id: number): Observable<{ status: string; openedAt: string; closedAt: string | null; orderId: number | null }[]> {
    return this.http.get(`${this.base}/tables/${id}/history`) as Observable<{ status: string; openedAt: string; closedAt: string | null; orderId: number | null }[]>;
  }

  categories(): Observable<{ id: number; name: string; active: boolean; sortOrder: number }[]> {
    return this.http.get(`${this.base}/menu/categories`) as Observable<{ id: number; name: string; active: boolean; sortOrder: number }[]>;
  }

  saveCategory(body: object): Observable<unknown> {
    return this.http.post(`${this.base}/menu/categories`, body);
  }

  products(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.base}/menu/products`);
  }

  product(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.base}/menu/products/${id}`);
  }

  saveProduct(product: Partial<Product>, variants: object[], modifierIds: number[], recipe: RecipeLine[]): Observable<ProductDetail> {
    return this.http.post<ProductDetail>(`${this.base}/menu/products`, { product, variants, modifierIds, recipe });
  }

  modifiers(): Observable<Modifier[]> {
    return this.http.get<Modifier[]>(`${this.base}/menu/modifiers`);
  }

  saveModifier(body: Partial<Modifier>): Observable<Modifier> {
    return this.http.post<Modifier>(`${this.base}/menu/modifiers`, body);
  }

  variants(productId: number): Observable<Variant[]> {
    return this.http.get<Variant[]>(`${this.base}/menu/products/${productId}/variants`);
  }

  orders(query: Record<string, string | number | null | undefined>): Observable<Page<OrderSummary>> {
    return this.http.get<Page<OrderSummary>>(`${this.base}/orders`, { params: this.params(query) });
  }

  order(id: number): Observable<OrderView> {
    return this.http.get<OrderView>(`${this.base}/orders/${id}`);
  }

  createOrder(body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders`, body);
  }

  addLine(id: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/lines`, body);
  }

  updateLine(id: number, lineId: number, body: object): Observable<OrderView> {
    return this.http.put<OrderView>(`${this.base}/orders/${id}/lines/${lineId}`, body);
  }

  cancelLine(id: number, lineId: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/lines/${lineId}/cancel`, body);
  }

  discount(id: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/discount`, body);
  }

  sendKitchen(id: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/send`, body);
  }

  deliver(id: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/deliver`, body);
  }

  pay(id: number, body: object, key: string): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/payments`, body, { headers: this.key(key) });
  }

  voidPayment(id: number, paymentId: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/payments/${paymentId}/void`, body);
  }

  refund(id: number, body: object, key: string): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/refunds`, body, { headers: this.key(key) });
  }

  closeOrder(id: number, version: number): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/close`, { version, reason: 'Cierre de cuenta' });
  }

  cancelOrder(id: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/cancel`, body);
  }

  transfer(id: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/transfer`, body);
  }

  split(id: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/split`, body);
  }

  merge(id: number, body: object): Observable<OrderView> {
    return this.http.post<OrderView>(`${this.base}/orders/${id}/merge`, body);
  }

  receipt(id: number): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.base}/orders/${id}/receipt`);
  }

  receiptPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/orders/${id}/receipt.pdf`, { responseType: 'blob' });
  }

  tickets(branchId: number, history: boolean): Observable<KitchenTicket[]> {
    return this.http.get<KitchenTicket[]>(`${this.base}/kitchen/tickets`, { params: { branchId, history } });
  }

  advanceTicket(id: number, status?: string): Observable<KitchenTicket> {
    return this.http.post<KitchenTicket>(`${this.base}/kitchen/tickets/${id}/advance`, status ? { status } : {});
  }

  ingredients(): Observable<Ingredient[]> {
    return this.http.get<Ingredient[]>(`${this.base}/inventory/ingredients`);
  }

  saveIngredient(body: Ingredient): Observable<Ingredient> {
    return this.http.post<Ingredient>(`${this.base}/inventory/ingredients`, body);
  }

  stock(branchId: number): Observable<StockLevel[]> {
    return this.http.get<StockLevel[]>(`${this.base}/inventory/stock`, { params: { branchId } });
  }

  movements(branchId: number): Observable<StockMovement[]> {
    return this.http.get<StockMovement[]>(`${this.base}/inventory/movements`, { params: { branchId } });
  }

  moveStock(body: object): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${this.base}/inventory/movements`, body);
  }

  transferStock(body: object): Observable<void> {
    return this.http.post<void>(`${this.base}/inventory/transfers`, body);
  }

  warehouses(branchId: number): Observable<Warehouse[]> {
    return this.http.get<Warehouse[]>(`${this.base}/inventory/warehouses`, { params: { branchId } });
  }

  suppliers(): Observable<Supplier[]> {
    return this.http.get<Supplier[]>(`${this.base}/suppliers`);
  }

  saveSupplier(body: Supplier): Observable<Supplier> {
    return this.http.post<Supplier>(`${this.base}/suppliers`, body);
  }

  purchases(branchId: number): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(`${this.base}/purchases`, { params: { branchId } });
  }

  purchase(id: number): Observable<PurchaseDetail> {
    return this.http.get<PurchaseDetail>(`${this.base}/purchases/${id}`);
  }

  createPurchase(body: object): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(`${this.base}/purchases`, body);
  }

  orderPurchase(id: number, version: number): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.base}/purchases/${id}/order`, { version });
  }

  receivePurchase(id: number, body: object, key: string): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(`${this.base}/purchases/${id}/receive`, body, { headers: this.key(key) });
  }

  payPurchase(id: number, body: object): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.base}/purchases/${id}/pay`, body);
  }

  cancelPurchase(id: number, version: number, reason: string): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.base}/purchases/${id}/cancel`, { version: String(version), reason });
  }

  expenses(query: Record<string, string | number | null | undefined>): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.base}/expenses`, { params: this.params(query) });
  }

  createExpense(expense: object, payNow: boolean): Observable<Expense> {
    return this.http.post<Expense>(`${this.base}/expenses`, { expense, payNow });
  }

  payExpense(id: number, methodId: number): Observable<Expense> {
    return this.http.post<Expense>(`${this.base}/expenses/${id}/pay`, { methodId });
  }

  voidExpense(id: number, reason: string): Observable<Expense> {
    return this.http.post<Expense>(`${this.base}/expenses/${id}/void`, { reason });
  }

  cash(branchId: number): Observable<CashCurrent> {
    return this.http.get<CashCurrent>(`${this.base}/cash/current`, { params: { branchId } });
  }

  openCash(branchId: number, openingFloat: number, notes: string): Observable<CashSession> {
    return this.http.post<CashSession>(`${this.base}/cash/open`, {
      branchId: String(branchId),
      openingFloat: openingFloat.toFixed(2),
      notes,
    });
  }

  cashMove(id: number, body: object): Observable<unknown> {
    return this.http.post(`${this.base}/cash/${id}/movements`, body);
  }

  closeCash(id: number, body: object): Observable<CashSession> {
    return this.http.post<CashSession>(`${this.base}/cash/${id}/close`, body);
  }

  dashboard(branchId: number | null, from: string, to: string): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.base}/dashboard`, { params: this.params({ branchId, from, to }) });
  }

  income(branchId: number | null, from: string, to: string): Observable<IncomeStatement> {
    return this.http.get<IncomeStatement>(`${this.base}/reports/income-statement`, { params: this.params({ branchId, from, to }) });
  }

  report(type: string, branchId: number | null, from: string, to: string): Observable<ReportTable> {
    return this.http.get<ReportTable>(`${this.base}/reports/${type}`, { params: this.params({ branchId, from, to }) });
  }

  exportReport(type: string, branchId: number | null, from: string, to: string, format: 'csv' | 'pdf' = 'csv'): Observable<Blob> {
    return this.http.get(`${this.base}/reports/${type}/export`, {
      params: this.params({ branchId, from, to, format }),
      responseType: 'blob',
    });
  }

  notifications(branchId: number | null): Observable<Notice[]> {
    return this.http.get<Notice[]>(`${this.base}/notifications`, { params: this.params({ branchId }) });
  }

  private key(value: string): HttpHeaders {
    return new HttpHeaders({ 'Idempotency-Key': value });
  }

  private params(query: Record<string, string | number | boolean | null | undefined>): HttpParams {
    let params = new HttpParams();
    for (const [name, value] of Object.entries(query)) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(name, String(value));
      }
    }
    return params;
  }
}
