-- Trape E-commerce — Initial schema
create extension if not exists "uuid-ossp";
create extension if not exists pg_trgm;

create table users (
    id uuid primary key default uuid_generate_v4(),
    email text unique,
    phone text unique,
    password_hash text,
    full_name text,
    role text not null default 'CUSTOMER'
        check (role in ('CUSTOMER','ADMIN','SUPER_ADMIN')),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_identifier check (email is not null or phone is not null)
);

create table refresh_tokens (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash text not null unique,
    device_info text,
    expires_at timestamptz not null,
    revoked boolean not null default false,
    created_at timestamptz not null default now()
);
create index idx_refresh_tokens_user on refresh_tokens(user_id);

create table addresses (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references users(id) on delete cascade,
    label text,
    line1 text not null,
    line2 text,
    city text not null,
    state text not null,
    pincode text not null,
    phone text not null,
    is_default boolean not null default false,
    created_at timestamptz not null default now()
);
create index idx_addresses_user on addresses(user_id);

create table categories (
    id uuid primary key default uuid_generate_v4(),
    parent_id uuid references categories(id),
    name text not null,
    slug text not null unique,
    display_order int not null default 0,
    is_active boolean not null default true
);

create table products (
    id uuid primary key default uuid_generate_v4(),
    category_id uuid references categories(id),
    name text not null,
    slug text not null unique,
    description text,
    brand text default 'Trape',
    base_price numeric(10,2) not null,
    status text not null default 'DRAFT'
        check (status in ('DRAFT','ACTIVE','ARCHIVED')),
    search_vector tsvector,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_products_category on products(category_id);
create index idx_products_search on products using gin(search_vector);
create index idx_products_name_trgm on products using gin(name gin_trgm_ops);

create trigger trg_products_search_vector
before insert or update on products
for each row execute function
    tsvector_update_trigger(search_vector, 'pg_catalog.english', name, description);

create table product_images (
    id uuid primary key default uuid_generate_v4(),
    product_id uuid not null references products(id) on delete cascade,
    url text not null,
    alt_text text,
    display_order int not null default 0
);
create index idx_product_images_product on product_images(product_id);

create table product_variants (
    id uuid primary key default uuid_generate_v4(),
    product_id uuid not null references products(id) on delete cascade,
    sku text not null unique,
    size text,
    color text,
    price numeric(10,2) not null,
    mrp numeric(10,2),
    is_active boolean not null default true,
    created_at timestamptz not null default now()
);
create index idx_variants_product on product_variants(product_id);

create table inventory (
    id uuid primary key default uuid_generate_v4(),
    variant_id uuid not null references product_variants(id) on delete cascade,
    warehouse_id text not null default 'DEFAULT',
    quantity_available int not null default 0 check (quantity_available >= 0),
    quantity_reserved int not null default 0 check (quantity_reserved >= 0),
    updated_at timestamptz not null default now(),
    unique(variant_id, warehouse_id)
);

create table carts (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid references users(id) on delete cascade,
    guest_token text unique,
    status text not null default 'ACTIVE'
        check (status in ('ACTIVE','CONVERTED','ABANDONED')),
    updated_at timestamptz not null default now(),
    constraint chk_owner check (user_id is not null or guest_token is not null)
);

create table cart_items (
    id uuid primary key default uuid_generate_v4(),
    cart_id uuid not null references carts(id) on delete cascade,
    variant_id uuid not null references product_variants(id),
    quantity int not null check (quantity > 0),
    price_snapshot numeric(10,2) not null,
    added_at timestamptz not null default now(),
    unique(cart_id, variant_id)
);

create table coupons (
    id uuid primary key default uuid_generate_v4(),
    code text not null unique,
    discount_type text not null check (discount_type in ('PERCENT','FLAT')),
    discount_value numeric(10,2) not null,
    min_cart_value numeric(10,2) default 0,
    valid_from timestamptz not null,
    valid_until timestamptz not null,
    usage_limit int,
    usage_count int not null default 0,
    is_active boolean not null default true
);

create table orders (
    id uuid primary key default uuid_generate_v4(),
    order_number text not null unique,
    user_id uuid not null references users(id),
    shipping_address_id uuid not null references addresses(id),
    coupon_id uuid references coupons(id),
    subtotal numeric(10,2) not null,
    discount numeric(10,2) not null default 0,
    shipping_fee numeric(10,2) not null default 0,
    total numeric(10,2) not null,
    status text not null default 'CREATED'
        check (status in
          ('CREATED','PAYMENT_PENDING','PAID','PROCESSING','SHIPPED',
           'DELIVERED','CANCELLED','REFUNDED','FAILED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_orders_user on orders(user_id);
create index idx_orders_status on orders(status);

create table order_items (
    id uuid primary key default uuid_generate_v4(),
    order_id uuid not null references orders(id) on delete cascade,
    variant_id uuid not null references product_variants(id),
    product_name_snapshot text not null,
    variant_label_snapshot text,
    quantity int not null check (quantity > 0),
    unit_price numeric(10,2) not null
);
create index idx_order_items_order on order_items(order_id);

create table payments (
    id uuid primary key default uuid_generate_v4(),
    order_id uuid not null references orders(id),
    razorpay_order_id text unique,
    razorpay_payment_id text unique,
    razorpay_signature text,
    status text not null default 'INITIATED'
        check (status in ('INITIATED','CAPTURED','FAILED','REFUNDED')),
    amount numeric(10,2) not null,
    raw_payload jsonb,
    created_at timestamptz not null default now()
);
create index idx_payments_order on payments(order_id);

create table order_status_history (
    id uuid primary key default uuid_generate_v4(),
    order_id uuid not null references orders(id) on delete cascade,
    status text not null,
    note text,
    created_by uuid references users(id),
    created_at timestamptz not null default now()
);

create table reviews (
    id uuid primary key default uuid_generate_v4(),
    product_id uuid not null references products(id) on delete cascade,
    user_id uuid not null references users(id),
    rating int not null check (rating between 1 and 5),
    comment text,
    created_at timestamptz not null default now(),
    unique(product_id, user_id)
);

create table wishlist_items (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references users(id) on delete cascade,
    product_id uuid not null references products(id) on delete cascade,
    created_at timestamptz not null default now(),
    unique(user_id, product_id)
);
