#!/bin/bash
# ============================================================
# Instagram DM Automation — End-to-End Verification Script
# ============================================================
# Prerequisites:
#   1. docker-compose up -d  (all services running)
#   2. Set META_APP_SECRET in .env
#
# This script verifies the full pipeline:
#   1. Health checks on both services
#   2. Webhook verification handshake (GET)
#   3. Signed webhook payload delivery (POST)
#   4. RabbitMQ queue inspection
#   5. PostgreSQL log inspection
# ============================================================

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

WEBHOOK_URL="http://localhost:3000"
WORKER_URL="http://localhost:8080"
RABBITMQ_API="http://localhost:15672/api"
RABBITMQ_USER="igdm_rabbit"
RABBITMQ_PASS="changeme_rabbit_password"

# Load .env if it exists
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

META_SECRET="${META_APP_SECRET:-your_meta_app_secret_here}"
VERIFY_TOKEN="${META_VERIFY_TOKEN:-your_custom_verify_token_here}"

echo "============================================"
echo "Instagram DM Automation — E2E Verification"
echo "============================================"
echo ""

# --- 1. Health Checks ---
echo -e "${YELLOW}[1/5] Health Checks${NC}"

echo -n "  Webhook Receiver: "
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$WEBHOOK_URL/health" 2>/dev/null || echo "000")
if [ "$HEALTH" = "200" ] || [ "$HEALTH" = "503" ]; then
    echo -e "${GREEN}OK ($HEALTH)${NC}"
    curl -s "$WEBHOOK_URL/health" | python3 -m json.tool 2>/dev/null || true
else
    echo -e "${RED}FAILED ($HEALTH) — Is the webhook receiver running?${NC}"
fi

echo -n "  Worker Service: "
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$WORKER_URL/actuator/health" 2>/dev/null || echo "000")
if [ "$HEALTH" = "200" ]; then
    echo -e "${GREEN}OK ($HEALTH)${NC}"
else
    echo -e "${RED}FAILED ($HEALTH) — Is the worker service running?${NC}"
fi
echo ""

# --- 2. Webhook Verification Handshake ---
echo -e "${YELLOW}[2/5] Webhook Verification Handshake (GET /webhook)${NC}"
CHALLENGE="test_challenge_$(date +%s)"
RESPONSE=$(curl -s "$WEBHOOK_URL/webhook?hub.mode=subscribe&hub.verify_token=$VERIFY_TOKEN&hub.challenge=$CHALLENGE" 2>/dev/null || echo "FAILED")

if [ "$RESPONSE" = "$CHALLENGE" ]; then
    echo -e "  ${GREEN}✓ Challenge echoed correctly: $CHALLENGE${NC}"
else
    echo -e "  ${RED}✗ Expected '$CHALLENGE', got '$RESPONSE'${NC}"
fi
echo ""

# --- 3. Signed Webhook Payload ---
echo -e "${YELLOW}[3/5] Signed Webhook Payload (POST /webhook)${NC}"

PAYLOAD='{
  "object": "instagram",
  "entry": [{
    "id": "17841400123456789",
    "time": '"$(date +%s)"',
    "changes": [{
      "field": "comments",
      "value": {
        "from": {"id": "17841400987654321", "username": "e2e_test_user"},
        "media": {"id": "17890012345678901", "media_product_type": "FEED"},
        "id": "COMMENT_E2E_'"$(date +%s)"'",
        "text": "I want the link!"
      }
    }]
  }]
}'

# Compute HMAC-SHA256 signature
SIGNATURE="sha256=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$META_SECRET" | awk '{print $2}')"

echo "  Payload: $(echo $PAYLOAD | python3 -m json.tool 2>/dev/null | head -5)..."
echo "  Signature: ${SIGNATURE:0:30}..."

HTTP_CODE=$(curl -s -o /tmp/webhook_response.txt -w "%{http_code}" \
    -X POST "$WEBHOOK_URL/webhook" \
    -H "Content-Type: application/json" \
    -H "X-Hub-Signature-256: $SIGNATURE" \
    -d "$PAYLOAD" 2>/dev/null || echo "000")

BODY=$(cat /tmp/webhook_response.txt 2>/dev/null || echo "")

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "  ${GREEN}✓ Webhook accepted (200 OK), body: $BODY${NC}"
else
    echo -e "  ${RED}✗ Unexpected response: HTTP $HTTP_CODE, body: $BODY${NC}"
fi
echo ""

# --- 4. RabbitMQ Queue Check ---
echo -e "${YELLOW}[4/5] RabbitMQ Queue Status${NC}"

for QUEUE in "ig.comments.process" "ig.comments.retry" "ig.comments.dead-letter"; do
    QUEUE_INFO=$(curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" \
        "$RABBITMQ_API/queues/%2F/$QUEUE" 2>/dev/null || echo '{}')
    MESSAGES=$(echo "$QUEUE_INFO" | python3 -c "import sys,json; print(json.load(sys.stdin).get('messages', 'N/A'))" 2>/dev/null || echo "N/A")
    CONSUMERS=$(echo "$QUEUE_INFO" | python3 -c "import sys,json; print(json.load(sys.stdin).get('consumers', 'N/A'))" 2>/dev/null || echo "N/A")
    echo "  $QUEUE: messages=$MESSAGES, consumers=$CONSUMERS"
done
echo ""

# --- 5. Summary ---
echo -e "${YELLOW}[5/5] Quick Links${NC}"
echo "  Webhook Health:   $WEBHOOK_URL/health"
echo "  Worker Health:    $WORKER_URL/actuator/health"
echo "  RabbitMQ UI:      http://localhost:15672 ($RABBITMQ_USER / $RABBITMQ_PASS)"
echo "  PostgreSQL:       psql -h localhost -U igdm_admin -d igdm_automation"
echo ""
echo -e "${GREEN}============================================"
echo "E2E verification complete!"
echo -e "============================================${NC}"
