-- Datos maestros iniciales. No incluye ventas, cobros ni gastos: esas cifras nacen de operaciones reales.
-- El inventario de apertura queda registrado como movimiento OPENING auditable.

INSERT INTO restaurants (id, name, legal_name, tax_id, phone, email, address, country_code, currency_code, timezone, locale)
VALUES (1, 'Fogón', 'Fogón Restaurante S.R.L.', '101000001', '809-555-0100', 'hola@fogon.local',
        'Calle El Conde 120, Ciudad Colonial, Santo Domingo', 'DO', 'DOP', 'America/Santo_Domingo', 'es-DO');

INSERT INTO branches (id, restaurant_id, code, name, address, phone, active)
VALUES (1, 1, 'PRI', 'Sucursal principal', 'Calle El Conde 120, Ciudad Colonial, Santo Domingo', '809-555-0100', TRUE);

INSERT INTO permissions (id, code, name, module) VALUES
    (1, 'dashboard.view', 'Ver tablero', 'dashboard'),
    (2, 'users.manage', 'Administrar usuarios', 'users'),
    (3, 'settings.manage', 'Configurar identidad del restaurante', 'settings'),
    (4, 'branches.manage', 'Administrar sucursales', 'settings'),
    (5, 'branches.switch', 'Cambiar de sucursal', 'settings'),
    (6, 'tables.view', 'Ver mesas', 'tables'),
    (7, 'tables.manage', 'Administrar mesas y áreas', 'tables'),
    (8, 'tables.operate', 'Operar mesas', 'tables'),
    (9, 'menu.view', 'Ver menú', 'menu'),
    (10, 'menu.manage', 'Administrar menú', 'menu'),
    (11, 'orders.view', 'Ver pedidos', 'orders'),
    (12, 'orders.create', 'Crear pedidos', 'orders'),
    (13, 'orders.update', 'Modificar pedidos', 'orders'),
    (14, 'orders.cancel', 'Anular pedidos', 'orders'),
    (15, 'orders.discount', 'Autorizar descuentos', 'orders'),
    (16, 'orders.close', 'Cerrar cuentas', 'orders'),
    (17, 'kitchen.view', 'Ver cocina', 'kitchen'),
    (18, 'kitchen.update', 'Actualizar comandas', 'kitchen'),
    (19, 'inventory.view', 'Ver inventario', 'inventory'),
    (20, 'inventory.manage', 'Administrar inventario', 'inventory'),
    (21, 'inventory.adjust', 'Ajustar inventario y mermas', 'inventory'),
    (22, 'suppliers.manage', 'Administrar proveedores', 'purchases'),
    (23, 'purchases.view', 'Ver compras', 'purchases'),
    (24, 'purchases.manage', 'Administrar compras', 'purchases'),
    (25, 'purchases.receive', 'Recibir compras', 'purchases'),
    (26, 'expenses.view', 'Ver gastos', 'expenses'),
    (27, 'expenses.manage', 'Registrar gastos', 'expenses'),
    (28, 'expenses.pay', 'Pagar gastos', 'expenses'),
    (29, 'cash.view', 'Ver caja', 'cash'),
    (30, 'cash.operate', 'Operar caja', 'cash'),
    (31, 'cash.close', 'Cerrar caja', 'cash'),
    (32, 'payments.collect', 'Cobrar', 'payments'),
    (33, 'payments.refund', 'Reembolsar', 'payments'),
    (34, 'reports.view', 'Ver reportes operativos', 'reports'),
    (35, 'reports.financial', 'Ver estado de resultados', 'reports'),
    (36, 'receipts.view', 'Ver comprobantes', 'receipts'),
    (37, 'settings.operations', 'Configurar parámetros operativos', 'settings');

INSERT INTO roles (id, code, name, description) VALUES
    (1, 'ADMIN', 'Administrador', 'Acceso total, incluida la identidad fiscal del restaurante y la asignación del rol administrador'),
    (2, 'MANAGER', 'Gerente', 'Operación, personal, reportes financieros y parámetros. No cambia la identidad fiscal ni otorga el rol administrador'),
    (3, 'CASHIER', 'Cajero', 'Caja, cobros, cuentas y gastos pagados desde el turno'),
    (4, 'WAITER', 'Mesero', 'Mesas, pedidos y envío a cocina'),
    (5, 'COOK', 'Cocinero', 'Comandas y estado de preparación'),
    (6, 'INVENTORY', 'Encargado de inventario', 'Existencias, recetas, proveedores y compras');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

INSERT INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE code <> 'settings.manage';

INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE code IN (
    'dashboard.view', 'tables.view', 'tables.operate', 'menu.view', 'orders.view', 'orders.create', 'orders.update',
    'orders.close', 'expenses.view', 'expenses.pay', 'cash.view', 'cash.operate', 'cash.close',
    'payments.collect', 'payments.refund', 'reports.view', 'receipts.view'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT 4, id FROM permissions WHERE code IN (
    'dashboard.view', 'tables.view', 'tables.operate', 'menu.view', 'orders.view', 'orders.create', 'orders.update', 'receipts.view'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT 5, id FROM permissions WHERE code IN ('dashboard.view', 'menu.view', 'kitchen.view', 'kitchen.update');

INSERT INTO role_permissions (role_id, permission_id)
SELECT 6, id FROM permissions WHERE code IN (
    'dashboard.view', 'menu.view', 'inventory.view', 'inventory.manage', 'inventory.adjust',
    'suppliers.manage', 'purchases.view', 'purchases.manage', 'purchases.receive', 'reports.view'
);

INSERT INTO tax_rates (id, restaurant_id, name, rate, active) VALUES
    (1, 1, 'ITBIS 18%', 0.1800, TRUE),
    (2, 1, 'Exento', 0.0000, TRUE);

INSERT INTO payment_methods (id, restaurant_id, code, name, active, affects_cash, requires_reference, sort_order) VALUES
    (1, 1, 'CASH', 'Efectivo', TRUE, TRUE, FALSE, 1),
    (2, 1, 'CARD', 'Tarjeta', TRUE, FALSE, TRUE, 2),
    (3, 1, 'TRANSFER', 'Transferencia', TRUE, FALSE, TRUE, 3),
    (4, 1, 'DIGITAL', 'Pago digital', TRUE, FALSE, TRUE, 4),
    (5, 1, 'OTHER', 'Otro', TRUE, FALSE, FALSE, 5);

INSERT INTO units (id, restaurant_id, code, name, symbol, family, base_unit_id, factor_to_base, active) VALUES
    (1, 1, 'UNIT', 'Unidad', 'ud', 'COUNT', NULL, 1, TRUE),
    (2, 1, 'G', 'Gramo', 'g', 'WEIGHT', NULL, 1, TRUE),
    (6, 1, 'ML', 'Mililitro', 'ml', 'VOLUME', NULL, 1, TRUE);

INSERT INTO units (id, restaurant_id, code, name, symbol, family, base_unit_id, factor_to_base, active) VALUES
    (3, 1, 'KG', 'Kilogramo', 'kg', 'WEIGHT', 2, 1000, TRUE),
    (4, 1, 'LB', 'Libra', 'lb', 'WEIGHT', 2, 453.592370, TRUE),
    (5, 1, 'OZ', 'Onza', 'oz', 'WEIGHT', 2, 28.349523, TRUE),
    (7, 1, 'L', 'Litro', 'L', 'VOLUME', 6, 1000, TRUE);

INSERT INTO kitchen_stations (id, branch_id, code, name, active, sort_order) VALUES
    (1, 1, 'CALIENTE', 'Cocina caliente', TRUE, 1),
    (2, 1, 'FRIA', 'Cocina fría', TRUE, 2),
    (3, 1, 'BARRA', 'Barra', TRUE, 3);

INSERT INTO dining_areas (id, branch_id, code, name, active, sort_order) VALUES
    (1, 1, 'SALON', 'Salón', TRUE, 1),
    (2, 1, 'TERRAZA', 'Terraza', TRUE, 2),
    (3, 1, 'BAR', 'Bar', TRUE, 3);

INSERT INTO dining_tables (id, area_id, number, capacity, status, active) VALUES
    (1, 1, '1', 4, 'AVAILABLE', TRUE),
    (2, 1, '2', 4, 'AVAILABLE', TRUE),
    (3, 1, '3', 4, 'AVAILABLE', TRUE),
    (4, 1, '4', 2, 'AVAILABLE', TRUE),
    (5, 1, '5', 6, 'AVAILABLE', TRUE),
    (6, 1, '6', 4, 'AVAILABLE', TRUE),
    (7, 2, '7', 4, 'AVAILABLE', TRUE),
    (8, 2, '8', 4, 'AVAILABLE', TRUE),
    (9, 2, '9', 2, 'AVAILABLE', TRUE),
    (10, 2, '10', 4, 'AVAILABLE', TRUE),
    (11, 3, '11', 2, 'AVAILABLE', TRUE),
    (12, 3, '12', 2, 'AVAILABLE', TRUE),
    (13, 3, '13', 2, 'AVAILABLE', TRUE),
    (14, 3, '14', 4, 'AVAILABLE', TRUE);

INSERT INTO menu_categories (id, restaurant_id, name, sort_order, active, station_id) VALUES
    (1, 1, 'Entradas', 1, TRUE, 1),
    (2, 1, 'Platos fuertes', 2, TRUE, 1),
    (3, 1, 'Acompañantes', 3, TRUE, 1),
    (4, 1, 'Ensaladas', 4, TRUE, 2),
    (5, 1, 'Bebidas', 5, TRUE, 3),
    (6, 1, 'Postres', 6, TRUE, 2);

INSERT INTO products (id, restaurant_id, category_id, sku, name, description, product_type, base_price, tax_rate_id, prep_minutes, station_id, available, active, track_inventory) VALUES
    (1, 1, 1, 'YANI', 'Yaniqueque', 'Frito de harina, sal y aceite', 'FOOD', 150.00, 1, 8, 1, TRUE, TRUE, TRUE),
    (2, 1, 1, 'TOST', 'Tostones', 'Plátano verde dos veces frito', 'FOOD', 180.00, 1, 10, 1, TRUE, TRUE, TRUE),
    (3, 1, 2, 'POLLO', 'Pollo guisado', 'Pollo en salsa criolla', 'FOOD', 450.00, 1, 20, 1, TRUE, TRUE, TRUE),
    (4, 1, 2, 'PESC', 'Pescado frito', 'Pescado entero frito', 'FOOD', 650.00, 1, 18, 1, TRUE, TRUE, TRUE),
    (5, 1, 2, 'CHURR', 'Churrasco', 'Corte de res a la plancha', 'FOOD', 890.00, 1, 18, 1, TRUE, TRUE, TRUE),
    (6, 1, 2, 'MOFO', 'Mofongo', 'Plátano majado con ajo', 'FOOD', 420.00, 1, 15, 1, TRUE, TRUE, TRUE),
    (7, 1, 3, 'ARROZ', 'Arroz blanco', 'Porción de arroz', 'FOOD', 120.00, 1, 8, 1, TRUE, TRUE, TRUE),
    (8, 1, 3, 'HAB', 'Habichuelas', 'Habichuelas guisadas', 'FOOD', 120.00, 1, 8, 1, TRUE, TRUE, TRUE),
    (9, 1, 4, 'ENS', 'Ensalada verde', 'Lechuga y tomate', 'FOOD', 160.00, 1, 6, 2, TRUE, TRUE, TRUE),
    (10, 1, 5, 'MORIR', 'Morir soñando', 'Jugo de chinola con leche', 'DRINK', 180.00, 1, 5, 3, TRUE, TRUE, TRUE),
    (11, 1, 5, 'JUGO', 'Jugo de chinola', 'Jugo natural', 'DRINK', 140.00, 1, 5, 3, TRUE, TRUE, TRUE),
    (12, 1, 5, 'CERV', 'Cerveza', 'Botella', 'DRINK', 200.00, 1, 1, 3, TRUE, TRUE, TRUE),
    (13, 1, 5, 'AGUA', 'Agua', 'Botella de agua', 'DRINK', 80.00, 2, 1, 3, TRUE, TRUE, TRUE),
    (14, 1, 6, 'FLAN', 'Flan', 'Flan de huevo', 'FOOD', 160.00, 1, 5, 2, TRUE, TRUE, TRUE),
    (15, 1, 5, 'CAFE', 'Café', 'Café solo', 'DRINK', 70.00, 1, 4, 3, TRUE, TRUE, TRUE);

INSERT INTO product_variants (id, product_id, name, price_delta, active, sort_order) VALUES
    (1, 10, '12 oz', 0.00, TRUE, 1),
    (2, 10, '16 oz', 50.00, TRUE, 2),
    (3, 11, '12 oz', 0.00, TRUE, 1),
    (4, 11, '16 oz', 40.00, TRUE, 2);

INSERT INTO modifiers (id, restaurant_id, name, price, active) VALUES
    (1, 1, 'Extra queso', 80.00, TRUE),
    (2, 1, 'Sin cebolla', 0.00, TRUE),
    (3, 1, 'Picante', 0.00, TRUE),
    (4, 1, 'Aderezo', 25.00, TRUE);

INSERT INTO product_modifiers (product_id, modifier_id) VALUES
    (3, 1), (3, 2), (3, 3),
    (4, 1), (4, 2), (4, 3),
    (5, 1), (5, 2), (5, 3),
    (6, 1), (6, 2), (6, 3),
    (9, 4);

INSERT INTO ingredients (id, restaurant_id, sku, name, unit_id, min_stock, active) VALUES
    (1, 1, 'HAR-TRG', 'Harina de trigo', 3, 2.000, TRUE),
    (2, 1, 'ACE-VEG', 'Aceite vegetal', 7, 2.000, TRUE),
    (3, 1, 'SAL', 'Sal', 3, 0.500, TRUE),
    (4, 1, 'PLA-VER', 'Plátano verde', 4, 5.000, TRUE),
    (5, 1, 'POLLO', 'Pollo', 4, 8.000, TRUE),
    (6, 1, 'PESC', 'Pescado', 4, 5.000, TRUE),
    (7, 1, 'RES', 'Carne de res', 4, 6.000, TRUE),
    (8, 1, 'AJO', 'Ajo', 3, 0.300, TRUE),
    (9, 1, 'CEB', 'Cebolla', 3, 2.000, TRUE),
    (10, 1, 'PIM', 'Pimiento', 3, 1.000, TRUE),
    (11, 1, 'ARROZ', 'Arroz', 4, 8.000, TRUE),
    (12, 1, 'HAB', 'Habichuelas', 4, 4.000, TRUE),
    (13, 1, 'LECH', 'Lechuga', 1, 4.000, TRUE),
    (14, 1, 'TOM', 'Tomate', 4, 3.000, TRUE),
    (15, 1, 'CHI', 'Chinola', 1, 8.000, TRUE),
    (16, 1, 'LEC-EVA', 'Leche evaporada', 1, 6.000, TRUE),
    (17, 1, 'LEC-CON', 'Leche condensada', 1, 6.000, TRUE),
    (18, 1, 'AZU', 'Azúcar', 4, 3.000, TRUE),
    (19, 1, 'CAFE', 'Café molido', 4, 1.000, TRUE),
    (20, 1, 'CERV', 'Cerveza', 1, 12.000, TRUE),
    (21, 1, 'AGUA', 'Agua embotellada', 1, 12.000, TRUE),
    (22, 1, 'HUEVO', 'Huevo', 1, 12.000, TRUE),
    (23, 1, 'QUESO', 'Queso', 4, 2.000, TRUE);

INSERT INTO recipes (id, product_id, variant_id) VALUES
    (1, 1, NULL), (2, 2, NULL), (3, 3, NULL), (4, 4, NULL), (5, 5, NULL),
    (6, 6, NULL), (7, 7, NULL), (8, 8, NULL), (9, 9, NULL),
    (10, 10, 1), (11, 10, 2), (12, 11, 3), (13, 11, 4),
    (14, 12, NULL), (15, 13, NULL), (16, 14, NULL), (17, 15, NULL);

INSERT INTO recipe_lines (recipe_id, ingredient_id, quantity, unit_id) VALUES
    (1, 1, 0.080, 3), (1, 2, 20.000, 6), (1, 3, 0.002, 3),
    (2, 4, 0.500, 4), (2, 2, 30.000, 6), (2, 3, 0.002, 3),
    (3, 5, 0.700, 4), (3, 9, 0.050, 3), (3, 8, 0.010, 3), (3, 10, 0.040, 3), (3, 3, 0.003, 3),
    (4, 6, 0.800, 4), (4, 2, 40.000, 6), (4, 3, 0.003, 3),
    (5, 7, 0.700, 4), (5, 3, 0.003, 3),
    (6, 4, 0.800, 4), (6, 8, 0.015, 3), (6, 2, 20.000, 6), (6, 3, 0.003, 3),
    (7, 11, 0.250, 4), (7, 3, 0.002, 3),
    (8, 12, 0.200, 4), (8, 9, 0.030, 3), (8, 3, 0.002, 3),
    (9, 13, 0.300, 1), (9, 14, 0.200, 4),
    (10, 16, 0.250, 1), (10, 17, 0.150, 1), (10, 15, 1.000, 1),
    (11, 16, 0.350, 1), (11, 17, 0.200, 1), (11, 15, 1.500, 1),
    (12, 15, 2.000, 1), (12, 18, 0.050, 4),
    (13, 15, 3.000, 1), (13, 18, 0.070, 4),
    (14, 20, 1.000, 1),
    (15, 21, 1.000, 1),
    (16, 22, 2.000, 1), (16, 16, 0.500, 1), (16, 18, 0.150, 4),
    (17, 19, 0.015, 4), (17, 18, 0.020, 4);

INSERT INTO warehouses (id, branch_id, code, name, active) VALUES
    (1, 1, 'MAIN', 'Almacén principal', TRUE);

INSERT INTO stock_levels (warehouse_id, ingredient_id, quantity, average_cost) VALUES
    (1, 1, 10.000, 55.0000),
    (1, 2, 8.000, 180.0000),
    (1, 3, 2.000, 25.0000),
    (1, 4, 30.000, 25.0000),
    (1, 5, 25.000, 85.0000),
    (1, 6, 15.000, 160.0000),
    (1, 7, 20.000, 220.0000),
    (1, 8, 1.000, 180.0000),
    (1, 9, 5.000, 60.0000),
    (1, 10, 3.000, 70.0000),
    (1, 11, 40.000, 35.0000),
    (1, 12, 15.000, 70.0000),
    (1, 13, 12.000, 40.0000),
    (1, 14, 10.000, 45.0000),
    (1, 15, 30.000, 20.0000),
    (1, 16, 24.000, 75.0000),
    (1, 17, 18.000, 90.0000),
    (1, 18, 12.000, 40.0000),
    (1, 19, 3.000, 280.0000),
    (1, 20, 48.000, 90.0000),
    (1, 21, 36.000, 20.0000),
    (1, 22, 60.000, 12.0000),
    (1, 23, 5.000, 220.0000);

INSERT INTO stock_movements (warehouse_id, ingredient_id, movement_type, direction, quantity, unit_cost, reference_type, reference_id, reason, idempotency_key)
SELECT 1, ingredient_id, 'OPENING', 'IN', quantity, average_cost, 'OPENING', 'seed', 'Conteo inicial de apertura', 'seed-opening-' || ingredient_id
FROM stock_levels;

INSERT INTO expense_categories (id, restaurant_id, name, classification, active) VALUES
    (1, 1, 'Alquiler', 'OPERATING', TRUE),
    (2, 1, 'Nómina', 'OPERATING', TRUE),
    (3, 1, 'Servicios', 'OPERATING', TRUE),
    (4, 1, 'Mantenimiento', 'OPERATING', TRUE),
    (5, 1, 'Marketing', 'OPERATING', TRUE),
    (6, 1, 'Comisiones bancarias', 'OTHER_EXPENSE', TRUE),
    (7, 1, 'Otros ingresos', 'OTHER_INCOME', TRUE),
    (8, 1, 'Otros gastos', 'OTHER_EXPENSE', TRUE);

INSERT INTO document_sequences (branch_id, doc_type, prefix, next_number, padding) VALUES
    (1, 'ORDER', 'PED-', 1, 6),
    (1, 'RECEIPT', 'REC-', 1, 6),
    (1, 'PURCHASE', 'OC-', 1, 6);

INSERT INTO app_settings (restaurant_id, setting_key, setting_value) VALUES
    (1, 'tax.prices_include_tax', 'false'),
    (1, 'inventory.deduct_trigger', 'ON_SEND'),
    (1, 'inventory.allow_negative', 'false'),
    (1, 'cash.require_session', 'true'),
    (1, 'billing.auto_tip', 'false'),
    (1, 'billing.tip_percent', '10'),
    (1, 'kitchen.delay_factor', '1.25');

SELECT setval(pg_get_serial_sequence('restaurants', 'id'), (SELECT MAX(id) FROM restaurants));
SELECT setval(pg_get_serial_sequence('branches', 'id'), (SELECT MAX(id) FROM branches));
SELECT setval(pg_get_serial_sequence('permissions', 'id'), (SELECT MAX(id) FROM permissions));
SELECT setval(pg_get_serial_sequence('roles', 'id'), (SELECT MAX(id) FROM roles));
SELECT setval(pg_get_serial_sequence('tax_rates', 'id'), (SELECT MAX(id) FROM tax_rates));
SELECT setval(pg_get_serial_sequence('payment_methods', 'id'), (SELECT MAX(id) FROM payment_methods));
SELECT setval(pg_get_serial_sequence('units', 'id'), (SELECT MAX(id) FROM units));
SELECT setval(pg_get_serial_sequence('kitchen_stations', 'id'), (SELECT MAX(id) FROM kitchen_stations));
SELECT setval(pg_get_serial_sequence('dining_areas', 'id'), (SELECT MAX(id) FROM dining_areas));
SELECT setval(pg_get_serial_sequence('dining_tables', 'id'), (SELECT MAX(id) FROM dining_tables));
SELECT setval(pg_get_serial_sequence('menu_categories', 'id'), (SELECT MAX(id) FROM menu_categories));
SELECT setval(pg_get_serial_sequence('products', 'id'), (SELECT MAX(id) FROM products));
SELECT setval(pg_get_serial_sequence('product_variants', 'id'), (SELECT MAX(id) FROM product_variants));
SELECT setval(pg_get_serial_sequence('modifiers', 'id'), (SELECT MAX(id) FROM modifiers));
SELECT setval(pg_get_serial_sequence('ingredients', 'id'), (SELECT MAX(id) FROM ingredients));
SELECT setval(pg_get_serial_sequence('recipes', 'id'), (SELECT MAX(id) FROM recipes));
SELECT setval(pg_get_serial_sequence('warehouses', 'id'), (SELECT MAX(id) FROM warehouses));
SELECT setval(pg_get_serial_sequence('expense_categories', 'id'), (SELECT MAX(id) FROM expense_categories));
