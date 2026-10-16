export interface Profile {
  id: number;
  fullName: string;
  email: string;
  restaurantId: number;
  restaurantName: string;
  currency: string;
  timezone: string;
  locale: string;
  branchId: number | null;
  roles: string[];
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  user: Profile;
}

export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface Branch {
  id: number;
  code: string;
  name: string;
  address: string | null;
  phone: string | null;
  active: boolean;
}

export interface Named {
  id: number;
  name: string;
  active?: boolean;
}

export interface TaxRate extends Named {
  rate: number;
}

export interface PaymentMethod extends Named {
  code: string;
  affectsCash: boolean;
  requiresReference: boolean;
}

export interface Unit extends Named {
  code: string;
  family: string;
  factorToBase: number;
}

export interface Station extends Named {
  branchId: number;
  code: string;
}

export interface Area extends Named {
  branchId: number;
  sortOrder: number;
}

export interface DiningTable {
  id: number;
  areaId: number;
  tableNumber: string;
  capacity: number;
  status: string;
  assignedWaiterId: number | null;
  active: boolean;
  notes: string | null;
}

export interface Product {
  id: number;
  categoryId: number;
  sku: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  productType: string;
  basePrice: number;
  taxRateId: number;
  prepMinutes: number;
  stationId: number | null;
  available: boolean;
  active: boolean;
  trackInventory: boolean;
}

export interface Variant {
  id?: number;
  productId?: number;
  name: string;
  priceDelta: number;
  active: boolean;
  sortOrder: number;
}

export interface Modifier extends Named {
  price: number;
}

export interface ProductDetail {
  product: Product;
  variants: Variant[];
  modifierIds: number[];
  recipe: RecipeLine[];
}

export interface RecipeLine {
  ingredientId: number;
  quantity: number;
  unitId: number;
}

export interface Ingredient {
  id?: number;
  sku: string;
  name: string;
  unitId: number;
  minStock: number;
  active: boolean;
}

export interface StockLevel {
  warehouseId: number;
  ingredientId: number;
  quantity: number;
  averageCost: number;
}

export interface StockMovement {
  id: number;
  warehouseId: number;
  ingredientId: number;
  movementType: string;
  direction: string;
  quantity: number;
  unitCost: number;
  reason: string | null;
  createdAt: string;
}

export interface Warehouse extends Named {
  branchId: number;
  code: string;
}

export interface Supplier {
  id?: number;
  name: string;
  taxId: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  active: boolean;
  notes: string | null;
}

export interface PurchaseOrder {
  id: number;
  version: number;
  branchId: number;
  supplierId: number;
  warehouseId: number;
  number: string;
  status: string;
  orderDate: string;
  expectedDate: string | null;
  totalAmount: number;
  paidAmount: number;
  notes: string | null;
}

export interface PurchaseLine {
  id: number;
  ingredientId: number;
  quantityOrdered: number;
  quantityReceived: number;
  unitCost: number;
  taxRate: number;
}

export interface PurchaseDetail {
  order: PurchaseOrder;
  lines: PurchaseLine[];
  receipts: { id: number; documentRef: string | null; receivedAt: string }[];
}

export interface ExpenseCategory extends Named {
  classification: string;
}

export interface Expense {
  id: number;
  branchId: number;
  categoryId: number;
  supplierId: number | null;
  description: string;
  expenseDate: string;
  amount: number;
  currencyCode: string;
  paymentMethodId: number | null;
  status: string;
  receiptRef: string | null;
  notes: string | null;
}

export interface CashSession {
  id: number;
  version: number;
  openingFloat: number;
  expectedCash: number | null;
  countedCash: number | null;
  difference: number | null;
  status: string;
  notes: string | null;
  openedAt: string;
}

export interface CashCurrent {
  open: boolean;
  session?: CashSession;
  expectedCash?: number;
  movements?: { id: number; movementType: string; direction: string; amount: number; description: string | null; createdAt: string }[];
}

export interface OrderSummary {
  id: number;
  version: number;
  orderNumber: string;
  orderType: string;
  preparationStatus: string;
  accountStatus: string;
  totalAmount: number;
  paidAmount: number;
  openedAt: string;
  tableLabel: string | null;
  customerName: string | null;
}

export interface OrderLine {
  id: number;
  productId: number;
  variantId: number | null;
  productName: string;
  variantName: string | null;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  status: string;
  notes: string | null;
  stationName: string | null;
  modifiers: { id: number; name: string; unitPrice: number }[];
}

export interface OrderView {
  id: number;
  version: number;
  branchId: number;
  orderNumber: string;
  orderType: string;
  tableId: number | null;
  tableLabel: string | null;
  waiterId: number | null;
  waiterName: string | null;
  customerName: string | null;
  customerPhone: string | null;
  deliveryAddress: string | null;
  guestCount: number | null;
  preparationStatus: string;
  accountStatus: string;
  grossAmount: number;
  discountAmount: number;
  netAmount: number;
  taxAmount: number;
  totalAmount: number;
  paidAmount: number;
  balance: number;
  tipAmount: number;
  notes: string | null;
  lines: OrderLine[];
  payments: { id: number; methodId: number; methodName: string; amount: number; tipAmount: number; status: string; reference: string | null }[];
  refunds: { id: number; paymentId: number; amount: number; reason: string }[];
  events: { type: string; message: string; createdAt: string }[];
}

export interface KitchenTicket {
  id: number;
  orderId: number;
  orderNumber: string;
  tableId: number | null;
  stationName: string;
  status: string;
  receivedAt: string;
  elapsedMinutes: number;
  delayed: boolean;
  items: { id: number; name: string; quantity: number; notes: string | null; status: string }[];
}

export interface Restaurant {
  id: number;
  name: string;
  legalName: string | null;
  taxId: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  logoUrl: string | null;
  countryCode: string;
  currencyCode: string;
  timezone: string;
  locale: string;
}

export interface UserView {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  branchId: number | null;
  active: boolean;
  roles: string[];
}

export interface RoleView {
  id: number;
  code: string;
  name: string;
  description: string;
  permissions: string[];
}

export interface Staff {
  id: number;
  fullName: string;
  roles: string[];
}

export interface Setting {
  id: number;
  settingKey: string;
  settingValue: string;
}

export interface Sequence {
  id: number;
  branchId: number;
  docType: string;
  prefix: string;
  nextNumber: number;
  padding: number;
}

export interface ReportTable {
  title: string;
  columns: string[];
  rows: string[][];
  note: string;
}

export interface IncomeStatement {
  from: string;
  to: string;
  currency: string;
  grossSales: number;
  discounts: number;
  returns: number;
  netSales: number;
  cogs: number;
  grossProfit: number;
  operatingExpenses: number;
  operatingResult: number;
  otherIncome: number;
  otherExpenses: number;
  netResult: number;
  taxCollected: number;
  methodology: string;
}

export interface Dashboard {
  salesToday: number;
  salesMonth: number;
  orders: number;
  averageTicket: number;
  tables: Record<string, number>;
  pendingKitchen: number;
  lowStock: { name: string; amount: number }[];
  expenses: number;
  grossProfit: number;
  netResult: number;
  collections: number;
  salesByDay: { day: string; orders: number; total: number }[];
  paymentMethods: { name: string; amount: number }[];
  topProducts: { name: string; amount: number }[];
  recent: { action: string; entity: string; details: string; at: string }[];
  currency: string;
  from: string;
  to: string;
}

export interface Notice {
  level: string;
  title: string;
  message: string;
}
