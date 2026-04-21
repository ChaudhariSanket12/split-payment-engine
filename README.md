# ⚡ Split Payment Engine

A marketplace backend that accepts payments and **automatically splits** the amount between a Vendor and the Platform using **Razorpay Routes** (Test Mode).

> Portfolio project demonstrating Spring Boot, PostgreSQL (Supabase), Razorpay integration, and scheduled reconciliation.

---

## 🏗 Architecture

```
Customer → Frontend (HTML/JS)
              ↓ POST /api/orders
         Spring Boot Backend
              ↓ Creates Razorpay Order with Transfers
         Razorpay Routes API
         ├── 80% → Vendor Linked Account
         └── 20% → Platform Account
              ↓ Webhook (order.paid)
         PostgreSQL (Supabase) — updates order status
```

---

## 🛠 Tech Stack

| Layer      | Technology                        |
|------------|-----------------------------------|
| Backend    | Java 17, Spring Boot 3.x, Maven   |
| Database   | Supabase (PostgreSQL via JDBC)    |
| Payments   | Razorpay Java SDK (Test Mode)     |
| Frontend   | Vanilla HTML, CSS, JavaScript     |

---

## 📋 Prerequisites

- Java 17+
- Maven 3.8+
- A [Supabase](https://supabase.com) account
- A [Razorpay](https://razorpay.com) account (Test Mode)
- IntelliJ IDEA (recommended) or any IDE

---

## 🗄 Step 1 — Set Up Supabase Database

1. Go to [supabase.com](https://supabase.com) → Create a new project.
2. Navigate to **SQL Editor** and run the contents of:
   ```
   backend/src/main/resources/db/migration/V1__init_schema.sql
   ```
3. Go to **Settings → Database** and copy your connection string. It looks like:
   ```
   postgresql://postgres:[YOUR-PASSWORD]@db.xxxxxxxxxxxx.supabase.co:5432/postgres
   ```
   Format it for JDBC:
   ```
   jdbc:postgresql://db.xxxxxxxxxxxx.supabase.co:5432/postgres
   ```

---

## 💳 Step 2 — Get Razorpay Test API Keys

1. Sign up at [razorpay.com](https://razorpay.com) → Dashboard.
2. Go to **Settings → API Keys** → Generate Test Keys.
3. Copy your **Key ID** (starts with `rzp_test_`) and **Key Secret**.
4. For webhook secret: Go to **Settings → Webhooks** → Add a webhook URL and note the secret.
5. For split payments, enable **Razorpay Route** (under Products) in your dashboard.

---

## 🔧 Step 3 — Configure Environment Variables

### IntelliJ IDEA
1. Open **Run → Edit Configurations**.
2. Select your Spring Boot run config.
3. Click **Modify options → Environment variables**.
4. Add the following:

```
SUPABASE_DB_URL=jdbc:postgresql://db.xxxx.supabase.co:5432/postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=your_supabase_password
RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxx
RAZORPAY_KEY_SECRET=your_razorpay_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
RAZORPAY_PLATFORM_ACCOUNT_ID=acc_your_platform_account
```

### Terminal / Shell Export
```bash
export SUPABASE_DB_URL=jdbc:postgresql://db.xxxx.supabase.co:5432/postgres
export SUPABASE_DB_USER=postgres
export SUPABASE_DB_PASSWORD=your_supabase_password
export RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxx
export RAZORPAY_KEY_SECRET=your_razorpay_secret
export RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
export RAZORPAY_PLATFORM_ACCOUNT_ID=acc_your_platform_account
```

---

## 🚀 Step 4 — Run the Backend

```bash
cd backend
./mvnw spring-boot:run
```

Or on Windows:
```bash
mvnw.cmd spring-boot:run
```

The server starts at **http://localhost:8080**.

---

## 🌐 Step 5 — Run the Frontend

**Option A — VS Code Live Server:**
1. Open the `frontend/` folder in VS Code.
2. Right-click `index.html` → **Open with Live Server**.
3. Runs at `http://127.0.0.1:5500`.

**Option B — Python HTTP Server:**
```bash
cd frontend
python3 -m http.server 5500
```
Then open `http://localhost:5500`.

---

## 📡 API Endpoints

| Method | Endpoint                  | Description                          |
|--------|---------------------------|--------------------------------------|
| POST   | `/api/vendors`            | Create a vendor                      |
| GET    | `/api/vendors`            | List all vendors                     |
| GET    | `/api/vendors/{id}`       | Get vendor by ID                     |
| POST   | `/api/orders`             | Create order with split              |
| GET    | `/api/orders`             | List all orders                      |
| GET    | `/api/orders/{id}`        | Get order by ID                      |
| POST   | `/api/payments/verify`    | Verify checkout signature + update order |
| POST   | `/api/webhooks/razorpay`  | Razorpay webhook receiver            |

### Example: Create a Vendor
```bash
curl -X POST http://localhost:8080/api/vendors \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Vendor","email":"vendor@demo.com","razorpayAccountId":"acc_test_xyz"}'
```

### Example: Create an Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"vendorId":"<uuid>","customerEmail":"customer@test.com","amount":1000}'
```

### Example: Verify a Payment
```bash
curl -X POST http://localhost:8080/api/payments/verify \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayPaymentId":"pay_test_123",
    "razorpayOrderId":"order_test_123",
    "razorpaySignature":"generated_signature"
  }'
```

---

## 🧪 Run Tests

```bash
cd backend
./mvnw test
```

---

## 💰 Split Logic

- **Platform Fee:** 20% of total amount
- **Vendor Payout:** 80% of total amount
- Razorpay creates the order with a `transfers` array routing funds automatically via Routes.

---

## ⏰ Reconciliation

A scheduled job runs:
- Daily at **2:00 AM** (cron)
- Every **6 hours** (periodic)

It checks PENDING orders against Razorpay and logs any discrepancies to console.

---

## 🧱 Project Structure

```
split-payment-engine/
├── backend/
│   ├── src/main/java/com/example/splitpay/
│   │   ├── config/          # Razorpay + CORS config
│   │   ├── exception/       # Global error handler
│   │   ├── vendor/          # Vendor CRUD
│   │   ├── order/           # Order creation + split
│   │   ├── webhook/         # Razorpay webhook handler
│   │   └── reconciliation/  # Scheduled reconciliation
│   └── src/test/            # Unit tests (Mockito)
└── frontend/
    ├── index.html
    ├── style.css
    └── script.js
```
