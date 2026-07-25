# Phase 4 — Polish, Security & Production Readiness

## Roadmap — Sequential Implementation Order

Work through these items top-to-bottom. Each item builds on the previous ones.

---

### 🔴 High Priority (Core UX & Security)

- [ ] **1. Auth Guards on Frontend**
  - Create `useAuth()` hook that checks `localStorage` for JWT
  - Redirect unauthenticated users to `/auth/login` from all `/dashboard/*` and `/admin` pages
  - Show loading skeleton while checking auth state

- [ ] **2. Landing Page (`/`)**
  - Replace the bare redirect with a proper marketing/hero landing page
  - Hero section with product pitch, feature highlights, CTA buttons
  - Should wow visitors — glassmorphism, gradient text, animated elements

- [ ] **3. Demo Data Seeder**
  - `POST /api/v1/admin/seed` endpoint that populates sample rules, interaction logs, and stats
  - Makes the dashboard look alive on first visit instead of empty
  - Seed ~50 interaction logs across SENT/SKIPPED/FAILED/RATE_LIMITED statuses

- [ ] **4. DM Throughput Chart on Dashboard**
  - Add a time-series bar/line chart (Recharts or Chart.js) showing DMs sent over last 7 days
  - Backend: `GET /api/v1/dashboard/chart` returning daily aggregated counts

- [ ] **5. Search & Filter on Activity Logs**
  - Add search input for commenter username / comment text
  - Status filter tabs (All, Sent, Skipped, Failed, Rate Limited)
  - Wire to backend query params

- [ ] **6. Rule Edit Modal**
  - Add edit button on each rule card
  - Pre-populate form with existing rule data
  - Wire to `PUT /api/v1/rules/{id}` endpoint

---

### 🟡 Medium Priority (Infrastructure & DX)

- [ ] **7. Nginx Reverse Proxy**
  - Single entry point on port 80
  - Route `/api/*` → Spring Boot (:8080)
  - Route `/webhook` → Node.js (:3000)
  - Route `/*` → Next.js (:3001)
  - Add `nginx` service to `docker-compose.yml`

- [ ] **8. Role-Based Access Control for Admin Portal**
  - Add `role` field to `User` entity (`user` | `admin`)
  - Protect `/api/v1/admin/**` endpoints — require `role: admin`
  - Hide Admin Portal sidebar link for non-admin users
  - First registered user gets `admin` role by default

- [ ] **9. Settings / Profile Page**
  - `/dashboard/settings` page
  - Change password, update display name
  - View current plan tier and account limits

- [ ] **10. CI/CD Pipeline (GitHub Actions)**
  - `.github/workflows/ci.yml`
  - Run webhook-receiver Jest tests
  - Run worker-service Maven compile
  - Build Docker images
  - Push to GitHub Container Registry on `main` branch

---

### 🟢 Nice-to-Have (Future Enhancements)

- [ ] **11. Real-time Dashboard Updates (WebSocket/SSE)**
  - Live interaction log feed without page refresh
  - Real-time KPI counter updates when webhooks arrive

- [ ] **12. Email Notifications**
  - Alert when Meta rate limit hits 80%
  - Alert when DLQ errors accumulate
  - Daily summary digest email

- [ ] **13. Stripe Billing Integration**
  - Enforce plan tier limits (free: 100 DMs/day, pro: 1000, enterprise: unlimited)
  - Stripe checkout for plan upgrades
  - Usage metering and invoicing

- [ ] **14. Production Docker Compose**
  - `docker-compose.prod.yml` with TLS, resource limits, log rotation
  - Environment-specific configs
  - Database backup cron

- [ ] **15. Comprehensive Test Suite**
  - Spring Boot integration tests with Testcontainers
  - Frontend E2E tests with Playwright
  - API contract tests

---

## Progress Tracking

| # | Item | Status | Commit |
|---|---|---|---|
| 0 | Git repo + push to GitHub | 🔄 In Progress | — |
| 1 | Auth Guards | ⬜ Not Started | — |
| 2 | Landing Page | ⬜ Not Started | — |
| 3 | Demo Data Seeder | ⬜ Not Started | — |
| 4 | DM Throughput Chart | ⬜ Not Started | — |
| 5 | Search & Filter Logs | ⬜ Not Started | — |
| 6 | Rule Edit Modal | ⬜ Not Started | — |
| 7 | Nginx Reverse Proxy | ⬜ Not Started | — |
| 8 | RBAC for Admin | ⬜ Not Started | — |
| 9 | Settings Page | ⬜ Not Started | — |
| 10 | CI/CD Pipeline | ⬜ Not Started | — |
| 11 | Real-time Updates | ⬜ Not Started | — |
| 12 | Email Notifications | ⬜ Not Started | — |
| 13 | Stripe Billing | ⬜ Not Started | — |
| 14 | Production Compose | ⬜ Not Started | — |
| 15 | Test Suite | ⬜ Not Started | — |
