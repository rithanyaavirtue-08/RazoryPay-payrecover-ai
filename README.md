# PayRecover AI

AI-powered failed payment recovery platform inspired by Razorpay. Detects failed payments, analyzes them with Gemini AI, and executes recovery actions (retry, reminder, escalate, stop).

## Features

| Phase | Feature |
|-------|---------|
| 1 | Spring Boot + React scaffold |
| 2 | Payments CRUD, MySQL/H2 database |
| 3 | Revenue at Risk dashboard |
| 4 | AI analysis via Google Gemini |
| 5 | Execute recovery actions with history |
| 6 | Razorpay order creation + webhooks |
| 7 | Docker, tests, documentation |

## Architecture

```
Frontend (React + Vite)
    ↓ /api/*
Backend (Spring Boot)
    ├── Payments API
    ├── Revenue at Risk API
    ├── AI Analysis (Gemini)
    ├── Recovery Execution
    └── Razorpay (orders + webhooks)
    ↓
MySQL / H2 Database
```

## Quick Start (Docker)

1. Copy environment file:
   ```bash
   cp .env.example .env
   ```

2. Add your `GEMINI_API_KEY` to `.env` (required for AI analysis).

3. Optionally add Razorpay test keys for live checkout:
   ```
   RAZORPAY_KEY_ID=rzp_test_...
   RAZORPAY_KEY_SECRET=...
   RAZORPAY_WEBHOOK_SECRET=...
   ```

4. Start the stack:
   ```bash
   docker compose up -d --build
   ```

5. Open **http://localhost:3001**

## Local Development

### Backend (H2, port 8081)
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend (port 5173)
```bash
cd frontend
npm install
npm run dev
```

## End-to-End Workflow

### Path A: Razorpay payment (recommended)

```
1. Create Razorpay Order (UI)
        ↓
2. Payment saved as PENDING (source: RAZORPAY)
        ↓
3a. Real checkout (if keys configured) → success → RECOVERED via webhook
3b. Simulation mode → click "Simulate Failure" → status FAILED
        ↓
4. Click Analyze → Gemini returns RETRY / REMINDER / ESCALATE / STOP
        ↓
5. Click Execute → action applied, status updated, history saved
        ↓
6. If RETRY on Razorpay payment → new Razorpay order created (PENDING)
```

### Path B: Manual failed payment

```
1. Add Manual Failed Payment (form)
        ↓
2. Payment saved as FAILED (source: MANUAL)
        ↓
3. Analyze → Execute (same as above)
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/payments` | List all payments |
| POST | `/api/payments` | Create manual payment |
| GET | `/api/revenue-at-risk` | Dashboard stats |
| POST | `/api/ai/analyze/{paymentId}` | AI analysis (FAILED only) |
| POST | `/api/recovery/execute/{paymentId}` | Execute AI recommendation |
| GET | `/api/recovery/history` | Recovery action history |
| GET | `/api/razorpay/config` | Razorpay configuration status |
| POST | `/api/razorpay/orders` | Create Razorpay order |
| POST | `/api/razorpay/webhook` | Razorpay webhook handler |
| POST | `/api/razorpay/simulate-failure/{paymentId}` | Simulate failure (demo mode) |

## Recovery Actions

| Action | Effect |
|--------|--------|
| RETRY | Sets PENDING; Razorpay payments get a new order; stops after 3 attempts |
| REMINDER | Sets REMINDER_SENT |
| ESCALATE | Sets ESCALATED |
| STOP | Sets STOPPED |

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | Yes (for AI) | Google Gemini API key |
| `GEMINI_API_MODEL` | No | Default: `gemini-2.5-flash` |
| `RAZORPAY_KEY_ID` | No | Razorpay test/live key ID |
| `RAZORPAY_KEY_SECRET` | No | Razorpay key secret |
| `RAZORPAY_WEBHOOK_SECRET` | No | Webhook signature verification |
| `DB_PASSWORD` | Docker | MySQL root password |

## Tests

```bash
cd backend
mvn test
```

## Tech Stack

- **Backend:** Java 17, Spring Boot 3, JPA, MySQL/H2
- **Frontend:** React 19, Vite
- **AI:** Google Gemini 2.5 Flash
- **Payments:** Razorpay Java SDK
- **Infra:** Docker Compose (MySQL + backend + nginx frontend)
