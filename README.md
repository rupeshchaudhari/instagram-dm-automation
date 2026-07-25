# 🚀 Instagram Comment-to-DM Automation SaaS Platform

A high-performance, multi-tenant SaaS platform built with **Next.js 14**, **Node.js (Express)**, **Java 21 (Spring Boot 3.3)**, **Nginx**, **RabbitMQ 3.13**, and **PostgreSQL 16**. Automates Instagram Direct Messages based on comment triggers using the official Meta Graph API.

---

## 🔑 How to Login & Quick Start

### 🌐 Unified Gateway Entry Point: [http://localhost](http://localhost) (Port 80)

### Option A: Use the Pre-Configured Super Admin Account
- **Web App URL**: [http://localhost/auth/login](http://localhost/auth/login)
- **Email**: `admin@example.com`
- **Password**: `password123`
- **Role**: `ADMIN` (Access to Super Admin Control Portal at [http://localhost/admin](http://localhost/admin))

### Option B: Create a New Account
1. Open [http://localhost/auth/register](http://localhost/auth/register)
2. Enter your Name, Email, and Password.
3. Click **Get Started Free** — automatically logs you in and issues a secure JWT token!

---

## 🛠️ System Services & Port 80 Gateway URLs

| Component | URL (via Port 80 Nginx Gateway) | Direct Service Port | Description / Credentials |
|---|---|---|---|
| 🌐 **Landing Page & Marketing** | [http://localhost](http://localhost) | Port 3001 | Next.js 14 Hero & Feature Showcase |
| 🖥️ **User SaaS Dashboard** | [http://localhost/dashboard](http://localhost/dashboard) | Port 3001 | Overview, Accounts, Rules, Activity Logs |
| 🛡️ **Super Admin Portal** | [http://localhost/admin](http://localhost/admin) | Port 3001 | Platform KPIs, Rates, User Management, Seeder |
| ⚙️ **Account Settings** | [http://localhost/dashboard/settings](http://localhost/dashboard/settings) | Port 3001 | Profile Name, Password Credentials, Plan Tier |
| ⚡ **Spring Boot REST APIs** | [http://localhost/api/v1](http://localhost/api/v1) | Port 8080 | Actuator: [http://localhost/actuator/health](http://localhost/actuator/health) |
| 🪝 **Webhook Receiver** | [http://localhost/webhook](http://localhost/webhook) | Port 3000 | Ultra-fast Node.js Webhook Receiver |
| 🐇 **RabbitMQ Dashboard** | [http://localhost:15672](http://localhost:15672) | Port 15672 | User: `igdm_rabbit` \| Pass: `changeme_rabbit_password` |
| 🐘 **PostgreSQL Database** | `localhost:5432` | Port 5432 | DB: `igdm_automation` \| User: `igdm_admin` |

---

## 🚀 Running Locally with Docker Compose

To start all 6 containerized services (`postgres`, `rabbitmq`, `webhook-receiver`, `worker-service`, `frontend`, `nginx`):

```bash
# Clone the repository
git clone https://github.com/rupeshchaudhari/instagram-dm-automation.git
cd instagram-dm-automation

# Copy environment template
cp .env.example .env

# Launch all 6 containers in background
docker compose up -d
```

To view logs for any service:
```bash
# Follow all container logs
docker compose logs -f

# Follow Nginx gateway logs
docker compose logs -f nginx

# Follow Spring Boot worker logs
docker compose logs -f worker-service
```

---

## 🧪 Testing Webhook Triggers & Seeding Demo Data

### 1. Seed Sample Demo Data
To immediately populate sample rules and 50 realistic interaction logs across all statuses:
- Click **"Seed Sample Demo Data"** in the Admin Portal at [http://localhost/admin](http://localhost/admin), or run:

```bash
curl -X POST http://localhost/api/v1/admin/seed
```

### 2. Simulate an Instagram Comment Webhook
You can simulate Meta sending an Instagram comment webhook trigger by running:

```bash
PAYLOAD='{
  "object": "instagram",
  "entry": [{
    "id": "17841400123456789",
    "time": '"$(date +%s)"',
    "changes": [{
      "field": "comments",
      "value": {
        "from": {"id": "17841400987654321", "username": "alex_creator"},
        "media": {"id": "17890012345678901", "media_product_type": "FEED"},
        "id": "COMMENT_TEST_'"$(date +%s)"'",
        "text": "Send me the link please!"
      }
    }]
  }]
}'
SIGNATURE="sha256=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "your_custom_verify_token_here" | awk '{print $2}')"

curl -s -X POST "http://localhost/webhook" \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: $SIGNATURE" \
  -d "$PAYLOAD"
```

Then check **Activity Logs** in the web dashboard at [http://localhost/dashboard/logs](http://localhost/dashboard/logs) to see the instant audit log!

---

## 📐 Platform Architecture

```
[ Instagram / Meta Platform ]
           │
           │ (POST /webhook) Port 80 Gateway < 2s Ack
           ▼
     [ Nginx Reverse Proxy Gateway (:80) ]
       ├── /webhook  ──► [ Node.js Webhook Receiver (:3000) ] ──► [ RabbitMQ Topic Exchange ]
       ├── /api/v1   ──► [ Spring Boot Worker Service (:8080) ] ◄──────────┘ (Consume Event)
       └── /*        ──► [ Next.js 14 Web Application (:3001) ]
                                 │
                        [ PostgreSQL 16 DB ]
```

---

## 🔐 Key Features Built

- **Unified Nginx Port 80 Gateway**: Single entry point proxying frontend, REST APIs, and webhooks.
- **Instant Acknowledgement**: Webhook receiver verifies HMAC-SHA256 signatures and returns 200 OK within 2 seconds.
- **Asynchronous Message Queue**: Decoupled using RabbitMQ topic exchanges and manual ACK consumers.
- **AES-256-GCM Token Encryption**: Long-lived Meta access tokens encrypted at rest.
- **Rate Limit & 24h Window Protection**: Parses `x-app-usage` headers for proactive 80% throttling and enforces Meta's 24-hour window constraint.
- **Exponential Backoff Retries**: Per-message TTL backoff (5s → 30s → 2m) with Dead Letter Queue (DLQ) fallback.
- **Role-Based Access Control (RBAC)**: Enforces `USER` vs `ADMIN` roles across DB, REST endpoints, and UI sidebar links.
- **Multi-Tenant JWT Dashboard**: Next.js 14 interactive UI with 7-Day throughput volume bar chart, live log search & status filtering, rule builder edit modal, and settings page.
- **GitHub Actions CI/CD**: `.github/workflows/ci.yml` automating Node.js Jest tests, Spring Boot Maven compilation, and Docker image builds.
