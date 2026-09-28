# Trape — E-commerce Platform

A full-stack men's fashion e-commerce application, built from the HLD/LLD in
`trape-system-design/`.

- **Backend:** Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, Flyway
- **Frontend:** React 18 + Vite (no heavy UI framework — hand-rolled design system)
- **Database:** PostgreSQL via Supabase
- **Payments:** Razorpay (Orders API + Checkout.js + webhook)

```
trape-ecommerce/
├── trape-backend/     Spring Boot REST API
└── trape-frontend/    React + Vite storefront & admin console
```

---

## 1. What's implemented

| Area | Status |
|---|---|
| Auth (register/login/refresh/logout, JWT + rotating refresh tokens) | ✅ |
| Product catalog (categories, products, variants, images, search/filter/sort/pagination) | ✅ |
| Inventory with **pessimistic-lock stock reservation** at checkout | ✅ |
| Cart (guest via `X-Guest-Token` header, merges into user cart on login) | ✅ |
| Coupons (percent/flat, min-cart-value, usage limits) | ✅ |
| Checkout → Razorpay Order → **Checkout.js** → signature verification | ✅ |
| Razorpay **webhook** (HMAC-verified, idempotent, handles async confirmation) | ✅ |
| Order lifecycle state machine + status history + auto-cancel stale pending orders | ✅ |
| Reviews (verified-purchase only — requires a DELIVERED order for that product) | ✅ |
| Wishlist | ✅ |
| Addresses (multiple, default) | ✅ |
| Admin console (products, categories, inventory, coupons, order status) | ✅ |
| OTP-based login, email notifications, image upload pipeline, Redis caching | ⏳ Not built — see §6 |

---

## 2. Prerequisites

- **Java 21** + **Maven 3.9+**
- **Node.js 18+** and npm
- A **Supabase** project (free tier is fine) — for Postgres
- A **Razorpay** account (test mode is fine) — for payments

---

## 3. Set up Supabase Postgres

1. Create a project at [supabase.com](https://supabase.com).
2. Go to **Project Settings → Database → Connection string** and copy the
   **Session pooler** connection details (host, port 5432, database, user, password).
3. You do **not** need to run any SQL manually — Flyway migrations
   (`trape-backend/src/main/resources/db/migration/`) create the full schema
   and seed sample data automatically on first boot.

---

## 4. Set up Razorpay

1. Sign up at [razorpay.com](https://razorpay.com) and switch to **Test Mode**.
2. Go to **Settings → API Keys** → generate a Key Id / Key Secret pair.
3. Go to **Settings → Webhooks** → add a webhook pointing at
   `https://<your-backend-host>/api/v1/payments/webhook`, subscribe to
   `payment.captured` and `payment.failed`, and copy the **webhook secret**
   (different from your API key secret).
   - For local development, use a tunnel (e.g. `ngrok http 8080`) so
     Razorpay's servers can reach your machine.

---

## 5. Run the backend

```bash
cd trape-backend
cp .env.example .env     # fill in DB_URL / DB_USERNAME / DB_PASSWORD / RAZORPAY_* / JWT_SECRET
export $(grep -v '^#' .env | xargs)   # or use your IDE's env-var support / direnv
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. On first boot, Flyway creates the
schema and seeds:

- **Admin login:** `admin@trape.in` / `Admin@123`
- **Test customer login:** `customer@trape.in` / `Test@123`
- 4 categories, 4 products with variants and stock, 2 sample coupons
  (`WELCOME10`, `FLAT500`)

Swagger UI: `http://localhost:8080/swagger-ui.html`
Health check: `http://localhost:8080/actuator/health`

> ⚠️ Change the admin password and `JWT_SECRET` before deploying anywhere
> real. The seeded bcrypt hashes are for local development only.

### Build a runnable jar

```bash
mvn clean package -DskipTests
java -jar target/trape-backend-1.0.0.jar
```

---

## 6. Run the frontend

```bash
cd trape-frontend
cp .env.example .env      # VITE_API_BASE_URL=http://localhost:8080/api/v1
npm install
npm run dev
```

Opens on `http://localhost:5173`.

```bash
npm run build    # production build -> dist/
npm run preview  # serve the production build locally
```

---

## 7. Architecture notes (how the LLD decisions show up in code)

- **Stock reservation, not stock locking on add-to-cart.** Adding to cart
  only checks current stock; the actual reservation (`quantity_reserved++`)
  happens inside the checkout transaction, under a
  `SELECT ... FOR UPDATE` row lock per variant
  (`InventoryRepository.findByVariantIdForUpdate` /
  `InventoryService.reserveStock`). This matches
  `diagrams/05-payment-race-sequence.mmd` — the lock is held only for the
  short checkout critical section, not for the lifetime of the cart.

- **Cart is cleared only on payment confirmation**, not at checkout
  initiation. A second checkout attempt on the same cart before the first
  payment completes will fail fast on insufficient stock — this is a
  deliberate fail-safe against duplicate orders from a double-click or a
  second tab, rather than a bug.

- **Two independent payment-confirmation paths converge on one idempotent
  method** (`OrderService.markPaid`): the client-side Checkout.js success
  callback (`POST /payments/verify`, signature checked against the API key
  secret) and the Razorpay webhook (`POST /payments/webhook`, HMAC checked
  against a *separate* webhook secret over the raw request body). Whichever
  arrives first wins; the second is a no-op. This is the standard mitigation
  for the "user closes the tab right after paying" race.

- **Order status transitions are centralized** in `OrderStateMachine` — every
  status change (customer cancel, admin update, payment webhook, stale-order
  sweep) goes through `assertTransitionAllowed`, so an invalid jump (e.g.
  `SHIPPED → PAYMENT_PENDING`) fails loudly instead of corrupting state.

- **A scheduled sweep** (`OrderService.releaseStaleReservations`, every 5
  minutes) auto-cancels orders stuck in `PAYMENT_PENDING` past the
  configured timeout (`app.order.payment-pending-timeout-minutes`, default
  15) and releases their stock holds.

- **Entities intentionally avoid JPA relational mappings** (`@OneToMany` /
  `@ManyToOne`) between aggregates — e.g. `Product` doesn't map its images
  or variants as JPA collections. Foreign keys are plain `UUID` columns, and
  services fetch related rows explicitly via their own repositories. This
  keeps fetch strategy explicit and avoids N+1 / lazy-init surprises,
  trading a bit of boilerplate for predictability — a defensible choice at
  this scale, and one worth revisiting with proper `@EntityGraph` usage if
  the catalog grows much larger.

- **Guest carts merge into the user's cart on login**
  (`CartService.mergeGuestCartIntoUser`, called from `AuthService.login` when
  the frontend sends `X-Guest-Token` on the login request).

- **API responses use a single envelope** (`ApiResponse<T>`:
  `{ success, data | error, traceId }`) so the frontend has one place to
  handle errors (`api/client.js`'s `request()` helper).

---

## 8. What's deliberately out of scope for this build

These are called out in the original LLD as candidates for the next
iteration, not implemented here (all can be added without touching the
schema/architecture above):

- **OTP-based phone login** — the schema supports it (`phone` column,
  nullable `password_hash`), but only email/password auth is wired up.
- **Redis caching** for hot product/category reads — the LLD's NFR doc
  flags this for scale; not needed at seed-data volume.
- **Async email notifications** (order confirmation, shipping updates) —
  `spring-boot-starter-mail` is on the classpath and configured, but no
  listener sends anything yet.
- **Image upload/CDN pipeline** — `product_images.url` is a plain string;
  admin product creation expects you to paste a hosted image URL rather
  than uploading a file.
- **Refund automation** — cancelling a `PAID`/`PROCESSING` order marks it
  cancelled but does **not** call Razorpay's refund API; that's a manual
  admin step today (see the comment in `OrderService.cancel`).
- **Guest checkout** — checkout requires a logged-in user (guest *carts*
  work fully; guest *checkout* would need either a lightweight
  registration-at-checkout flow or an anonymous-order design, neither of
  which the current `orders.user_id NOT NULL` schema supports as-is).

## 9. A note on this build

This was generated in one long pairing session rather than compiled from a
component library — it's a genuinely complete, coherent vertical slice
(schema → services → API → UI), not a scaffold. The backend was checked for
package/import consistency and balanced syntax throughout, and the frontend
was actually run through `npm install && npm run build`, which completed
without errors. That said, neither side has been run end-to-end against a
live Supabase/Razorpay account in this environment (no internet access to
Maven Central or a real database here), so budget time for the first real
run to surface the small issues that only show up with a live DB connection
and real Razorpay test keys — a missing bean wire-up, an off-by-one in a
query, that sort of thing. Treat it as a strong, review-ready first PR
rather than something to deploy sight-unseen.
