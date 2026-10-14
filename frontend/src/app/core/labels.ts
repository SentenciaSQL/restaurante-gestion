export const ORDER_TYPE: Record<string, string> = {
  DINE_IN: 'En mesa',
  TAKEAWAY: 'Para llevar',
  DELIVERY: 'A domicilio',
};

export const PREP: Record<string, string> = {
  DRAFT: 'Borrador',
  PENDING_SEND: 'Pendiente de envío',
  SENT: 'Enviado a cocina',
  IN_PREPARATION: 'En preparación',
  READY: 'Listo',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
};

export const ACCOUNT: Record<string, string> = {
  OPEN: 'Abierta',
  CLOSED: 'Cerrada',
  CANCELLED: 'Cancelada',
};

export const LINE: Record<string, string> = {
  PENDING: 'Pendiente',
  SENT: 'Enviado',
  PREPARING: 'En preparación',
  READY: 'Listo',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
};

export const TABLE: Record<string, string> = {
  AVAILABLE: 'Disponible',
  OCCUPIED: 'Ocupada',
  RESERVED: 'Reservada',
  CLEANING: 'Pendiente de limpieza',
};

export const TICKET: Record<string, string> = {
  QUEUED: 'Recibida',
  IN_PROGRESS: 'En preparación',
  READY: 'Lista',
  DELIVERED: 'Entregada',
  CANCELLED: 'Cancelada',
};

export const EXPENSE: Record<string, string> = {
  PENDING: 'Pendiente de pago',
  PAID: 'Pagado',
  VOID: 'Anulado',
};

export const PURCHASE: Record<string, string> = {
  DRAFT: 'Borrador',
  ORDERED: 'Ordenada',
  PARTIAL: 'Recepción parcial',
  RECEIVED: 'Recibida',
  CANCELLED: 'Cancelada',
};

export const ROLE: Record<string, string> = {
  ADMIN: 'Administrador',
  MANAGER: 'Gerente',
  CASHIER: 'Cajero',
  WAITER: 'Mesero',
  COOK: 'Cocinero',
  INVENTORY: 'Encargado de inventario',
};

export function label(map: Record<string, string>, code: string | null | undefined): string {
  if (!code) return '—';
  return map[code] ?? code;
}
