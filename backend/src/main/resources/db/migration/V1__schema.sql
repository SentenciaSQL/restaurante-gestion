-- Esquema relacional de Fogón. Importes en numeric(14,2), cantidades en numeric(14,3),
-- costos unitarios en numeric(14,4). El redondeo de negocio es HALF_UP en la aplicación.

CREATE TABLE restaurants (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(160) NOT NULL,
    legal_name      VARCHAR(200),
    tax_id          VARCHAR(40),
    phone           VARCHAR(40),
    email           VARCHAR(160),
    address         VARCHAR(300),
    logo_url        VARCHAR(500),
    country_code    VARCHAR(2) NOT NULL DEFAULT 'DO',
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'DOP',
    timezone        VARCHAR(80) NOT NULL DEFAULT 'America/Santo_Domingo',
    locale          VARCHAR(20) NOT NULL DEFAULT 'es-DO',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE branches (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(160) NOT NULL,
    address         VARCHAR(300),
    phone           VARCHAR(40),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (restaurant_id, code)
);

CREATE TABLE permissions (
    id      BIGSERIAL PRIMARY KEY,
    code    VARCHAR(80) NOT NULL UNIQUE,
    name    VARCHAR(160) NOT NULL,
    module  VARCHAR(60) NOT NULL
);

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(40) NOT NULL UNIQUE,
    name        VARCHAR(80) NOT NULL,
    description VARCHAR(300)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    branch_id       BIGINT REFERENCES branches (id),
    email           VARCHAR(160) NOT NULL UNIQUE,
    password_hash   VARCHAR(120) NOT NULL,
    full_name       VARCHAR(160) NOT NULL,
    phone           VARCHAR(40),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    user_id         BIGINT REFERENCES users (id),
    action          VARCHAR(80) NOT NULL,
    entity_type     VARCHAR(80) NOT NULL,
    entity_id       VARCHAR(40),
    details         TEXT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tax_rates (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    name            VARCHAR(80) NOT NULL,
    rate            NUMERIC(8, 4) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_tax_rate_nonneg CHECK (rate >= 0)
);

CREATE TABLE payment_methods (
    id                  BIGSERIAL PRIMARY KEY,
    restaurant_id       BIGINT NOT NULL REFERENCES restaurants (id),
    code                VARCHAR(30) NOT NULL,
    name                VARCHAR(80) NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    affects_cash        BOOLEAN NOT NULL DEFAULT FALSE,
    requires_reference  BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order          INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (restaurant_id, code)
);

CREATE TABLE units (
    id                  BIGSERIAL PRIMARY KEY,
    restaurant_id       BIGINT NOT NULL REFERENCES restaurants (id),
    code                VARCHAR(20) NOT NULL,
    name                VARCHAR(80) NOT NULL,
    symbol              VARCHAR(12) NOT NULL,
    family              VARCHAR(20) NOT NULL,
    base_unit_id        BIGINT REFERENCES units (id),
    factor_to_base      NUMERIC(18, 6) NOT NULL DEFAULT 1,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (restaurant_id, code),
    CONSTRAINT ck_unit_factor CHECK (factor_to_base > 0),
    CONSTRAINT ck_unit_family CHECK (family IN ('COUNT', 'WEIGHT', 'VOLUME'))
);

CREATE TABLE kitchen_stations (
    id              BIGSERIAL PRIMARY KEY,
    branch_id       BIGINT NOT NULL REFERENCES branches (id),
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(80) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (branch_id, code)
);

CREATE TABLE dining_areas (
    id              BIGSERIAL PRIMARY KEY,
    branch_id       BIGINT NOT NULL REFERENCES branches (id),
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(80) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (branch_id, code)
);

CREATE TABLE dining_tables (
    id                  BIGSERIAL PRIMARY KEY,
    area_id             BIGINT NOT NULL REFERENCES dining_areas (id),
    number              VARCHAR(20) NOT NULL,
    capacity            INTEGER NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    assigned_waiter_id  BIGINT REFERENCES users (id),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    notes               VARCHAR(300),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (area_id, number),
    CONSTRAINT ck_table_capacity CHECK (capacity > 0),
    CONSTRAINT ck_table_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'CLEANING'))
);

CREATE TABLE table_reservations (
    id              BIGSERIAL PRIMARY KEY,
    table_id        BIGINT NOT NULL REFERENCES dining_tables (id),
    customer_name   VARCHAR(160) NOT NULL,
    phone           VARCHAR(40),
    party_size      INTEGER NOT NULL,
    reserved_from   TIMESTAMPTZ NOT NULL,
    reserved_until  TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes           VARCHAR(300),
    created_by      BIGINT REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_reservation_party CHECK (party_size > 0),
    CONSTRAINT ck_reservation_window CHECK (reserved_until > reserved_from),
    CONSTRAINT ck_reservation_status CHECK (status IN ('PENDING', 'SEATED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'))
);

CREATE TABLE table_occupancy (
    id          BIGSERIAL PRIMARY KEY,
    table_id    BIGINT NOT NULL REFERENCES dining_tables (id),
    order_id    BIGINT,
    waiter_id   BIGINT REFERENCES users (id),
    status      VARCHAR(20) NOT NULL,
    opened_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at   TIMESTAMPTZ,
    notes       VARCHAR(300)
);

CREATE TABLE menu_categories (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    name            VARCHAR(120) NOT NULL,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    station_id      BIGINT REFERENCES kitchen_stations (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id                  BIGSERIAL PRIMARY KEY,
    restaurant_id       BIGINT NOT NULL REFERENCES restaurants (id),
    category_id         BIGINT NOT NULL REFERENCES menu_categories (id),
    sku                 VARCHAR(40) NOT NULL,
    name                VARCHAR(160) NOT NULL,
    description         VARCHAR(600),
    image_url           VARCHAR(500),
    product_type        VARCHAR(20) NOT NULL,
    base_price          NUMERIC(14, 2) NOT NULL,
    tax_rate_id         BIGINT NOT NULL REFERENCES tax_rates (id),
    prep_minutes        INTEGER NOT NULL DEFAULT 10,
    station_id          BIGINT REFERENCES kitchen_stations (id),
    available           BOOLEAN NOT NULL DEFAULT TRUE,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    track_inventory     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (restaurant_id, sku),
    CONSTRAINT ck_product_price CHECK (base_price >= 0),
    CONSTRAINT ck_product_prep CHECK (prep_minutes >= 0),
    CONSTRAINT ck_product_type CHECK (product_type IN ('FOOD', 'DRINK'))
);

CREATE TABLE product_variants (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products (id),
    name            VARCHAR(80) NOT NULL,
    price_delta     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE modifiers (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    name            VARCHAR(80) NOT NULL,
    price           NUMERIC(14, 2) NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_modifier_price CHECK (price >= 0)
);

CREATE TABLE product_modifiers (
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    modifier_id BIGINT NOT NULL REFERENCES modifiers (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, modifier_id)
);

CREATE TABLE ingredients (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    sku             VARCHAR(40) NOT NULL,
    name            VARCHAR(160) NOT NULL,
    unit_id         BIGINT NOT NULL REFERENCES units (id),
    min_stock       NUMERIC(14, 3) NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (restaurant_id, sku),
    CONSTRAINT ck_ingredient_min CHECK (min_stock >= 0)
);

CREATE TABLE recipes (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products (id),
    variant_id  BIGINT REFERENCES product_variants (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_recipe_product ON recipes (product_id) WHERE variant_id IS NULL;
CREATE UNIQUE INDEX ux_recipe_variant ON recipes (product_id, variant_id) WHERE variant_id IS NOT NULL;

CREATE TABLE recipe_lines (
    id              BIGSERIAL PRIMARY KEY,
    recipe_id       BIGINT NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    ingredient_id   BIGINT NOT NULL REFERENCES ingredients (id),
    quantity        NUMERIC(14, 3) NOT NULL,
    unit_id         BIGINT NOT NULL REFERENCES units (id),
    CONSTRAINT ck_recipe_qty CHECK (quantity > 0)
);

CREATE TABLE warehouses (
    id          BIGSERIAL PRIMARY KEY,
    branch_id   BIGINT NOT NULL REFERENCES branches (id),
    code        VARCHAR(20) NOT NULL,
    name        VARCHAR(120) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (branch_id, code)
);

CREATE TABLE stock_levels (
    id              BIGSERIAL PRIMARY KEY,
    warehouse_id    BIGINT NOT NULL REFERENCES warehouses (id),
    ingredient_id   BIGINT NOT NULL REFERENCES ingredients (id),
    quantity        NUMERIC(14, 3) NOT NULL DEFAULT 0,
    average_cost    NUMERIC(14, 4) NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (warehouse_id, ingredient_id),
    CONSTRAINT ck_stock_cost CHECK (average_cost >= 0)
);

CREATE TABLE stock_movements (
    id                  BIGSERIAL PRIMARY KEY,
    warehouse_id        BIGINT NOT NULL REFERENCES warehouses (id),
    ingredient_id       BIGINT NOT NULL REFERENCES ingredients (id),
    movement_type       VARCHAR(30) NOT NULL,
    direction           VARCHAR(3) NOT NULL,
    quantity            NUMERIC(14, 3) NOT NULL,
    unit_cost           NUMERIC(14, 4) NOT NULL DEFAULT 0,
    reference_type      VARCHAR(40),
    reference_id        VARCHAR(40),
    reason              VARCHAR(300),
    user_id             BIGINT REFERENCES users (id),
    idempotency_key     VARCHAR(80),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_movement_qty CHECK (quantity > 0),
    CONSTRAINT ck_movement_cost CHECK (unit_cost >= 0),
    CONSTRAINT ck_movement_dir CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_movement_type CHECK (movement_type IN (
        'OPENING', 'PURCHASE', 'SALE', 'SALE_REVERSAL', 'ADJUSTMENT', 'WASTE', 'SUPPLIER_RETURN', 'TRANSFER_IN', 'TRANSFER_OUT'
    ))
);

CREATE UNIQUE INDEX ux_stock_movement_idem ON stock_movements (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE suppliers (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    name            VARCHAR(160) NOT NULL,
    tax_id          VARCHAR(40),
    phone           VARCHAR(40),
    email           VARCHAR(160),
    address         VARCHAR(300),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    notes           VARCHAR(400),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE purchase_orders (
    id              BIGSERIAL PRIMARY KEY,
    branch_id       BIGINT NOT NULL REFERENCES branches (id),
    supplier_id     BIGINT NOT NULL REFERENCES suppliers (id),
    warehouse_id    BIGINT NOT NULL REFERENCES warehouses (id),
    number          VARCHAR(40) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    order_date      DATE NOT NULL,
    expected_date   DATE,
    notes           VARCHAR(400),
    subtotal        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(14, 2) NOT NULL DEFAULT 0,
    paid_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_by      BIGINT REFERENCES users (id),
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (branch_id, number),
    CONSTRAINT ck_po_status CHECK (status IN ('DRAFT', 'ORDERED', 'PARTIAL', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_po_paid CHECK (paid_amount >= 0)
);

CREATE TABLE purchase_order_lines (
    id                  BIGSERIAL PRIMARY KEY,
    purchase_order_id   BIGINT NOT NULL REFERENCES purchase_orders (id) ON DELETE CASCADE,
    ingredient_id       BIGINT NOT NULL REFERENCES ingredients (id),
    quantity_ordered    NUMERIC(14, 3) NOT NULL,
    quantity_received   NUMERIC(14, 3) NOT NULL DEFAULT 0,
    unit_cost           NUMERIC(14, 4) NOT NULL,
    tax_rate            NUMERIC(8, 4) NOT NULL DEFAULT 0,
    CONSTRAINT ck_po_line_qty CHECK (quantity_ordered > 0),
    CONSTRAINT ck_po_line_recv CHECK (quantity_received >= 0 AND quantity_received <= quantity_ordered),
    CONSTRAINT ck_po_line_cost CHECK (unit_cost >= 0),
    CONSTRAINT ck_po_line_tax CHECK (tax_rate >= 0)
);

CREATE TABLE goods_receipts (
    id                  BIGSERIAL PRIMARY KEY,
    purchase_order_id   BIGINT NOT NULL REFERENCES purchase_orders (id),
    warehouse_id        BIGINT NOT NULL REFERENCES warehouses (id),
    document_ref        VARCHAR(80),
    notes               VARCHAR(400),
    received_by         BIGINT REFERENCES users (id),
    idempotency_key     VARCHAR(80),
    received_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_receipt_idem ON goods_receipts (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE goods_receipt_lines (
    id                      BIGSERIAL PRIMARY KEY,
    receipt_id              BIGINT NOT NULL REFERENCES goods_receipts (id) ON DELETE CASCADE,
    purchase_order_line_id  BIGINT NOT NULL REFERENCES purchase_order_lines (id),
    quantity                NUMERIC(14, 3) NOT NULL,
    unit_cost               NUMERIC(14, 4) NOT NULL,
    CONSTRAINT ck_receipt_qty CHECK (quantity > 0),
    CONSTRAINT ck_receipt_cost CHECK (unit_cost >= 0)
);

CREATE TABLE expense_categories (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    name            VARCHAR(120) NOT NULL,
    classification  VARCHAR(20) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_expense_class CHECK (classification IN ('OPERATING', 'OTHER_EXPENSE', 'OTHER_INCOME'))
);

CREATE TABLE expenses (
    id                  BIGSERIAL PRIMARY KEY,
    branch_id           BIGINT NOT NULL REFERENCES branches (id),
    category_id         BIGINT NOT NULL REFERENCES expense_categories (id),
    supplier_id         BIGINT REFERENCES suppliers (id),
    description         VARCHAR(300) NOT NULL,
    expense_date        DATE NOT NULL,
    amount              NUMERIC(14, 2) NOT NULL,
    currency_code       VARCHAR(3) NOT NULL,
    payment_method_id   BIGINT REFERENCES payment_methods (id),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    receipt_ref         VARCHAR(80),
    notes               VARCHAR(400),
    created_by          BIGINT REFERENCES users (id),
    paid_by             BIGINT REFERENCES users (id),
    paid_at             TIMESTAMPTZ,
    cash_movement_id    BIGINT,
    void_reason         VARCHAR(300),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_expense_amount CHECK (amount > 0),
    CONSTRAINT ck_expense_status CHECK (status IN ('PENDING', 'PAID', 'VOID'))
);

CREATE TABLE cash_sessions (
    id              BIGSERIAL PRIMARY KEY,
    branch_id       BIGINT NOT NULL REFERENCES branches (id),
    opened_by       BIGINT NOT NULL REFERENCES users (id),
    closed_by       BIGINT REFERENCES users (id),
    opened_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at       TIMESTAMPTZ,
    opening_float   NUMERIC(14, 2) NOT NULL,
    expected_cash   NUMERIC(14, 2),
    counted_cash    NUMERIC(14, 2),
    difference      NUMERIC(14, 2),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    notes           VARCHAR(400),
    close_notes     VARCHAR(400),
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_float CHECK (opening_float >= 0),
    CONSTRAINT ck_cash_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE UNIQUE INDEX ux_cash_session_open ON cash_sessions (branch_id) WHERE status = 'OPEN';

CREATE TABLE cash_movements (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL REFERENCES cash_sessions (id),
    movement_type       VARCHAR(30) NOT NULL,
    direction           VARCHAR(3) NOT NULL,
    amount              NUMERIC(14, 2) NOT NULL,
    affects_cash        BOOLEAN NOT NULL DEFAULT FALSE,
    payment_method_id   BIGINT REFERENCES payment_methods (id),
    reference_type      VARCHAR(40),
    reference_id        VARCHAR(40),
    description         VARCHAR(300),
    user_id             BIGINT REFERENCES users (id),
    idempotency_key     VARCHAR(80),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_move_amount CHECK (amount >= 0),
    CONSTRAINT ck_cash_move_dir CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_cash_move_type CHECK (movement_type IN (
        'OPENING', 'SALE', 'REFUND', 'PAY_IN', 'PAY_OUT', 'EXPENSE', 'SUPPLIER_PAYMENT', 'TIP', 'ADJUSTMENT'
    ))
);

CREATE UNIQUE INDEX ux_cash_move_idem ON cash_movements (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE sales_orders (
    id                  BIGSERIAL PRIMARY KEY,
    branch_id           BIGINT NOT NULL REFERENCES branches (id),
    order_number        VARCHAR(40) NOT NULL,
    order_type          VARCHAR(20) NOT NULL,
    table_id            BIGINT REFERENCES dining_tables (id),
    waiter_id           BIGINT REFERENCES users (id),
    customer_name       VARCHAR(160),
    customer_phone      VARCHAR(40),
    delivery_address    VARCHAR(300),
    guest_count         INTEGER,
    preparation_status  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    account_status      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    prices_include_tax  BOOLEAN NOT NULL DEFAULT FALSE,
    inventory_trigger   VARCHAR(20) NOT NULL DEFAULT 'ON_SEND',
    gross_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    net_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    paid_amount         NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tip_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    header_discount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes               VARCHAR(400),
    cancel_reason       VARCHAR(300),
    opened_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at           TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    created_by          BIGINT REFERENCES users (id),
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (branch_id, order_number),
    CONSTRAINT ck_order_type CHECK (order_type IN ('DINE_IN', 'TAKEAWAY', 'DELIVERY')),
    CONSTRAINT ck_order_prep CHECK (preparation_status IN (
        'DRAFT', 'PENDING_SEND', 'SENT', 'IN_PREPARATION', 'READY', 'DELIVERED', 'CANCELLED'
    )),
    CONSTRAINT ck_order_account CHECK (account_status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_order_trigger CHECK (inventory_trigger IN ('ON_SEND', 'ON_CLOSE')),
    CONSTRAINT ck_order_guests CHECK (guest_count IS NULL OR guest_count > 0),
    CONSTRAINT ck_order_money CHECK (
        gross_amount >= 0 AND discount_amount >= 0 AND net_amount >= 0 AND tax_amount >= 0
        AND total_amount >= 0 AND paid_amount >= 0 AND tip_amount >= 0 AND header_discount >= 0
    )
);

CREATE UNIQUE INDEX ux_open_order_table ON sales_orders (table_id)
    WHERE account_status = 'OPEN' AND table_id IS NOT NULL;

ALTER TABLE table_occupancy
    ADD CONSTRAINT fk_occupancy_order FOREIGN KEY (order_id) REFERENCES sales_orders (id);

CREATE TABLE sales_order_lines (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES sales_orders (id),
    product_id          BIGINT REFERENCES products (id),
    variant_id          BIGINT REFERENCES product_variants (id),
    product_name        VARCHAR(160) NOT NULL,
    variant_name        VARCHAR(80),
    unit_price          NUMERIC(14, 2) NOT NULL,
    modifier_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    quantity            NUMERIC(14, 3) NOT NULL,
    entered_amount      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    line_discount       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    gross_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    net_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_rate            NUMERIC(8, 4) NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    cost_amount         NUMERIC(14, 4) NOT NULL DEFAULT 0,
    inventory_deducted  BOOLEAN NOT NULL DEFAULT FALSE,
    restocked           BOOLEAN NOT NULL DEFAULT FALSE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes               VARCHAR(300),
    station_id          BIGINT REFERENCES kitchen_stations (id),
    station_name        VARCHAR(80),
    cancel_reason       VARCHAR(300),
    sent_at             TIMESTAMPTZ,
    ready_at            TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_line_qty CHECK (quantity > 0),
    CONSTRAINT ck_line_status CHECK (status IN ('PENDING', 'SENT', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_line_tax CHECK (tax_rate >= 0)
);

CREATE TABLE sales_order_line_modifiers (
    id              BIGSERIAL PRIMARY KEY,
    order_line_id   BIGINT NOT NULL REFERENCES sales_order_lines (id) ON DELETE CASCADE,
    modifier_id     BIGINT REFERENCES modifiers (id),
    name            VARCHAR(80) NOT NULL,
    unit_price      NUMERIC(14, 2) NOT NULL,
    CONSTRAINT ck_olm_price CHECK (unit_price >= 0)
);

CREATE TABLE sales_order_discounts (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES sales_orders (id),
    discount_type   VARCHAR(20) NOT NULL,
    value           NUMERIC(14, 4) NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL,
    reason          VARCHAR(300) NOT NULL,
    authorized_by   BIGINT REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_discount_type CHECK (discount_type IN ('PERCENT', 'AMOUNT')),
    CONSTRAINT ck_discount_amount CHECK (amount >= 0 AND value >= 0)
);

CREATE TABLE sales_order_events (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES sales_orders (id),
    event_type  VARCHAR(40) NOT NULL,
    message     VARCHAR(400) NOT NULL,
    user_id     BIGINT REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE kitchen_tickets (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES sales_orders (id),
    branch_id       BIGINT NOT NULL REFERENCES branches (id),
    station_id      BIGINT REFERENCES kitchen_stations (id),
    station_name    VARCHAR(80) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at      TIMESTAMPTZ,
    ready_at        TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ticket_status CHECK (status IN ('QUEUED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED'))
);

CREATE TABLE kitchen_ticket_lines (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL REFERENCES kitchen_tickets (id) ON DELETE CASCADE,
    order_line_id   BIGINT NOT NULL REFERENCES sales_order_lines (id),
    quantity        NUMERIC(14, 3) NOT NULL,
    product_name    VARCHAR(160) NOT NULL,
    notes           VARCHAR(300),
    status          VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    CONSTRAINT ck_ktl_status CHECK (status IN ('QUEUED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED'))
);

CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES sales_orders (id),
    session_id          BIGINT REFERENCES cash_sessions (id),
    method_id           BIGINT NOT NULL REFERENCES payment_methods (id),
    amount              NUMERIC(14, 2) NOT NULL,
    tip_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    reference           VARCHAR(80),
    status              VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    idempotency_key     VARCHAR(80),
    paid_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id             BIGINT REFERENCES users (id),
    void_reason         VARCHAR(300),
    voided_at           TIMESTAMPTZ,
    voided_by           BIGINT REFERENCES users (id),
    cash_movement_id    BIGINT REFERENCES cash_movements (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_tip CHECK (tip_amount >= 0),
    CONSTRAINT ck_payment_status CHECK (status IN ('COMPLETED', 'VOIDED'))
);

CREATE UNIQUE INDEX ux_payment_idem ON payments (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE refunds (
    id                  BIGSERIAL PRIMARY KEY,
    payment_id          BIGINT NOT NULL REFERENCES payments (id),
    order_id            BIGINT NOT NULL REFERENCES sales_orders (id),
    amount              NUMERIC(14, 2) NOT NULL,
    net_amount          NUMERIC(14, 2) NOT NULL,
    tax_amount          NUMERIC(14, 2) NOT NULL,
    cost_amount         NUMERIC(14, 4) NOT NULL DEFAULT 0,
    restock             BOOLEAN NOT NULL DEFAULT FALSE,
    reason              VARCHAR(300) NOT NULL,
    authorized_by       BIGINT REFERENCES users (id),
    idempotency_key     VARCHAR(80),
    cash_movement_id    BIGINT REFERENCES cash_movements (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_refund_amount CHECK (amount > 0 AND net_amount >= 0 AND tax_amount >= 0 AND cost_amount >= 0)
);

CREATE UNIQUE INDEX ux_refund_idem ON refunds (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE receipts (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES sales_orders (id),
    number      VARCHAR(40) NOT NULL UNIQUE,
    snapshot    TEXT NOT NULL,
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document_sequences (
    id              BIGSERIAL PRIMARY KEY,
    branch_id       BIGINT NOT NULL REFERENCES branches (id),
    doc_type        VARCHAR(30) NOT NULL,
    prefix          VARCHAR(12) NOT NULL,
    next_number     BIGINT NOT NULL DEFAULT 1,
    padding         INTEGER NOT NULL DEFAULT 6,
    UNIQUE (branch_id, doc_type),
    CONSTRAINT ck_seq_next CHECK (next_number > 0),
    CONSTRAINT ck_seq_pad CHECK (padding BETWEEN 1 AND 12)
);

CREATE TABLE app_settings (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    setting_key     VARCHAR(80) NOT NULL,
    setting_value   VARCHAR(500) NOT NULL,
    UNIQUE (restaurant_id, setting_key)
);

CREATE INDEX ix_users_restaurant ON users (restaurant_id);
CREATE INDEX ix_audit_restaurant_created ON audit_logs (restaurant_id, created_at DESC);
CREATE INDEX ix_tables_area ON dining_tables (area_id);
CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_orders_branch_closed ON sales_orders (branch_id, closed_at);
CREATE INDEX ix_orders_branch_status ON sales_orders (branch_id, account_status);
CREATE INDEX ix_lines_order ON sales_order_lines (order_id);
CREATE INDEX ix_payments_order ON payments (order_id);
CREATE INDEX ix_payments_paid_at ON payments (paid_at);
CREATE INDEX ix_refunds_created ON refunds (created_at);
CREATE INDEX ix_stock_movements_ingredient ON stock_movements (ingredient_id, created_at DESC);
CREATE INDEX ix_expenses_branch_date ON expenses (branch_id, expense_date);
CREATE INDEX ix_tickets_branch_status ON kitchen_tickets (branch_id, status);
CREATE INDEX ix_cash_movements_session ON cash_movements (session_id);
