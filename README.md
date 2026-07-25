# 🚀 Instagram Comment-to-DM Automation SaaS Platform

A high-performance SaaS platform built with **Next.js 14**, **Node.js (Express)**, **Java 21 (Spring Boot 3.3)**, **RabbitMQ 3.13**, and **PostgreSQL 16**. Automates Instagram Direct Messages based on comment triggers using the official Meta Graph API.

---

## 🔑 How to Login & Quick Start

### Option A: Use the Test Admin Account
We pre-created an admin account in the database during system testing:
- **Dashboard URL**: [http://localhost:3001](http://localhost:3001)
- **Email**: `admin@example.com`
- **Password**: `password123`

### Option B: Create a New Account
1. Open [http://localhost:3001/auth/register](http://localhost:3001/auth/register)
2. Enter your Name, Email, and Password.
3. Click **Get Started** — you will be automatically logged in and issued a secure JWT token!

---

## 🛠️ System Services & URLs

| Component | URL | Description | Credentials / Details |
|---|---|---|---|
| 🖥️ **Web Dashboard** | [http://localhost:3001](http://localhost:3001) | Next.js 14 UI (Auth, Rules Builder, Activity Feed) | `admin@example.com` / `password123` |
| 🛡️ **Super Admin Portal** | [http://localhost:3001/admin](http://localhost:3001/admin) | Super Admin Control Portal (Users, Rates, Plans) | Live Platform Metrics & Controls |
| ⚡ **Spring Boot REST APIs** | [http://localhost:8080/api/v1](http://localhost:8080/api/v1) | Backend Worker & Management APIs | Actuator: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| 🪝 **Webhook Receiver** | [http://localhost:3000/webhook](http://localhost:3000/webhook) | Ultra-fast Node.js Webhook Receiver | Health: [http://localhost:3000/health](http://localhost:3000/health) |
| 🐇 **RabbitMQ Dashboard** | [http://localhost:15672](http://localhost:15672) | Message Broker Management UI | User: `igdm_rabbit`<br/>Pass: `changeme_rabbit_password` |
| 🐘 **PostgreSQL Database** | `localhost:5432` | Relational DB | DB: `igdm_automation`<br/>User: `igdm_admin` |

---

## 🚀 Running locally with Docker Compose

To start or restart the entire containerized stack:

```bash
# Clone or navigate to the project directory
cd /Users/rupeshchaudhari/projects/instagram-dm-automation

# Copy environment template if not already present
cp .env.example .env

# Launch all 5 container services in background
docker compose up -d
```

To view logs for any service:
```bash
# View all container logs
docker compose logs -f

# View worker service logs
docker compose logs -f worker-service

# View webhook receiver logs
docker compose logs -f webhook-receiver
```

---

## 🧪 Testing a Webhook Trigger (Simulate Instagram Comment)

You can simulate Meta sending an Instagram comment webhook trigger by executing:

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

curl -s -X POST "http://localhost:3000/webhook" \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: $SIGNATURE" \
  -d "$PAYLOAD"
```

Then check **Activity Logs** in the web dashboard at [http://localhost:3001/dashboard/logs](http://localhost:3001/dashboard/logs) to see the instant audit log!

---

## 📐 Platform Architecture

```
[ Instagram / Meta Platform ]
           │
           │ (POST /webhook) < 2s Ack
           ▼
 [ Node.js Webhook Receiver ] ──► (Publish) ──► [ RabbitMQ Topic Exchange ]
                                                        │
                                                        ▼
 [ PostgreSQL Database ] ◄── (Read/Write) ◄── [ Spring Boot Worker Service ]
                                                        │
 [ Next.js Web Dashboard ] ◄── (REST API + JWT) ────────┘
```

---

## 🔐 Features Built

- **Instant Acknowledgement**: Webhook receiver verifies HMAC-SHA256 signatures and returns 200 OK within 2 seconds.
- **Asynchronous Message Queue**: Decoupled using RabbitMQ topic exchanges and manual ACK consumers.
- **AES-256-GCM Token Encryption**: Long-lived Meta access tokens are encrypted at rest.
- **Rate Limit & 24h Messaging Window Protection**: Inspects `x-app-usage` headers for proactive 80% throttling and enforces Meta's 24-hour window constraint.
- **Exponential Backoff Retries**: Per-message TTL backoff (5s → 30s → 2m) with Dead Letter Queue (DLQ) fallback.
- **Multi-Tenant JWT Dashboard**: Next.js 14 interactive UI with dark theme, glassmorphism card layouts, and live campaign analytics.
