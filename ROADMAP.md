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
| 0 | Git repo + push to GitHub | ✅ Completed | `58757b7` |
| 1 | Auth Guards | ✅ Completed | `d91e402` |
| 2 | Landing Page | ✅ Completed | `d91e402` |
| 3 | Demo Data Seeder | ✅ Completed | `d91e402` |
| 4 | DM Throughput Chart | ✅ Completed | `d91e402` |
| 5 | Search & Filter Logs | ✅ Completed | `d91e402` |
| 6 | Rule Edit Modal | ✅ Completed | `d91e402` |
| 7 | Nginx Reverse Proxy | ⬜ Not Started | — |
| 8 | RBAC for Admin | ⬜ Not Started | — |
| 9 | Settings Page | ⬜ Not Started | — |
| 10 | CI/CD Pipeline | ⬜ Not Started | — |
| 11 | Real-time Updates | ⬜ Not Started | — |
| 12 | Email Notifications | ⬜ Not Started | — |
| 13 | Stripe Billing | ⬜ Not Started | — |
| 14 | Production Compose | ⬜ Not Started | — |
| 15 | Test Suite | ⬜ Not Started | — |
