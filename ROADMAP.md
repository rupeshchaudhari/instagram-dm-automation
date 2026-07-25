# Phase 4 — Polish, Security & Production Readiness

## Roadmap — Sequential Implementation Order

Work through these items top-to-bottom. Each item builds on the previous ones.

---

### 🔴 High Priority (Core UX & Security)

- [x] **1. Auth Guards on Frontend**
  - Created `useAuth()` hook and `<AuthGuard>` wrapper component
  - Protected all routes (`/dashboard`, `/dashboard/accounts`, `/dashboard/rules`, `/dashboard/logs`, `/admin`)
  - Redirects unauthenticated visitors to `/auth/login`

- [x] **2. Landing Page (`/`)**
  - Built marketing landing page at `/` with hero pitch, feature highlights, and CTA buttons
  - Responsive glassmorphism cards with glowing gradients and Meta Graph API compliance badge

- [x] **3. Demo Data Seeder**
  - Added `POST /api/v1/admin/seed` endpoint in Spring Boot
  - Populates realistic rules ("Summer Sale Promo", "Free VIP Guide", "Pricing Info Request") and 50 interaction logs across all statuses
  - One-click "Seed Sample Demo Data" action button in Super Admin Portal

- [x] **4. DM Throughput Chart on Dashboard**
  - Built 7-Day DM Automation Volume Bar Chart on `/dashboard`
  - Added `GET /api/v1/dashboard/chart` endpoint returning daily aggregated message throughput

- [x] **5. Search & Filter on Activity Logs**
  - Added live search input for commenter username, comment text, or rule name on `/dashboard/logs`
  - Added status filter tabs (All, Sent, Skipped, Rate Limited, Failed)

- [x] **6. Rule Edit Modal**
  - Added Edit icon button to every automation rule card on `/dashboard/rules`
  - Pre-populates rule modal with existing rule parameters and updates via `PUT /api/v1/rules/{id}`

---

### 🟡 Medium Priority (Infrastructure & DX)

- [x] **7. Nginx Reverse Proxy**
  - Configured Nginx gateway on Port `:80` in `nginx/nginx.conf`
  - Routes `/api/v1/*` → Spring Boot (`:8080`), `/webhook` → Node.js (`:3000`), `/*` → Next.js (`:3001`)
  - Added `nginx` container service to `docker-compose.yml`

- [x] **8. Role-Based Access Control for Admin Portal**
  - Added `role` column (`USER` | `ADMIN`) to PostgreSQL `users` table and `User.java` entity
  - Enforced `role == ADMIN` authorization check on `/api/v1/admin/*` REST endpoints
  - Automatically assigned `ADMIN` role to first registered user
  - Conditionally hidden Admin Portal sidebar link for regular users

- [x] **9. Settings / Profile Page**
  - Built `/dashboard/settings` page for user profile management
  - Display name update, password credentials change, and subscription tier / role display

- [x] **10. CI/CD Pipeline (GitHub Actions)**
  - Created `.github/workflows/ci.yml` pipeline
  - Automates Node.js Jest test suite, Spring Boot JDK 21 Maven compilation, and Docker image builds

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
| 0 | Git repo + push to GitHub | ✅ Completed | `58757b7` |
| 1 | Auth Guards | ✅ Completed | `d91e402` |
| 2 | Landing Page | ✅ Completed | `d91e402` |
| 3 | Demo Data Seeder | ✅ Completed | `d91e402` |
| 4 | DM Throughput Chart | ✅ Completed | `d91e402` |
| 5 | Search & Filter Logs | ✅ Completed | `d91e402` |
| 6 | Rule Edit Modal | ✅ Completed | `d91e402` |
| 7 | Nginx Reverse Proxy | ✅ Completed | `e619a41` |
| 8 | RBAC for Admin | ✅ Completed | `e619a41` |
| 9 | Settings Page | ✅ Completed | `e619a41` |
| 10 | CI/CD Pipeline | ✅ Completed | `e619a41` |
| 11 | Real-time Updates | ⬜ Not Started | — |
| 12 | Email Notifications | ⬜ Not Started | — |
| 13 | Stripe Billing | ⬜ Not Started | — |
| 14 | Production Compose | ⬜ Not Started | — |
| 15 | Test Suite | ⬜ Not Started | — |
