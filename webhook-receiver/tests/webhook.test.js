'use strict';

const crypto = require('crypto');

/**
 * Test suite for the Webhook Receiver.
 *
 * Tests cover:
 * 1. GET /webhook — Meta verification handshake
 * 2. POST /webhook — Signature verification and event routing
 * 3. GET /health — Health check endpoint
 *
 * Note: These tests mock RabbitMQ (no live broker needed).
 */

// ============================================
// Mock setup — must be before require()
// ============================================

// Mock amqplib to avoid real RabbitMQ connections
jest.mock('amqplib', () => ({
  connect: jest.fn().mockResolvedValue({
    createConfirmChannel: jest.fn().mockResolvedValue({
      assertExchange: jest.fn().mockResolvedValue({}),
      publish: jest.fn().mockReturnValue(true),
      waitForConfirms: jest.fn().mockResolvedValue(),
      close: jest.fn().mockResolvedValue(),
    }),
    on: jest.fn(),
    close: jest.fn().mockResolvedValue(),
  }),
}));

// Mock pino to suppress log output during tests
jest.mock('pino', () => {
  const mockLogger = {
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
    debug: jest.fn(),
    child: jest.fn().mockReturnThis(),
  };
  const pinoFn = jest.fn(() => mockLogger);
  pinoFn.stdSerializers = { err: jest.fn((e) => e) };
  return pinoFn;
});

// Set env vars for testing
process.env.META_APP_SECRET = 'test_secret_12345';
process.env.META_VERIFY_TOKEN = 'test_verify_token';
process.env.NODE_ENV = 'test';

const http = require('http');
const app = require('../src/index');
const commentFixture = require('./fixtures/comment_event.json');

let server;
let baseUrl;

beforeAll((done) => {
  // Use a random port for testing
  server = http.createServer(app);
  server.listen(0, () => {
    const port = server.address().port;
    baseUrl = `http://localhost:${port}`;
    done();
  });
});

afterAll((done) => {
  server.close(done);
});

// ============================================
// Helper: compute HMAC signature
// ============================================
function computeSignature(body, secret = 'test_secret_12345') {
  const hmac = crypto.createHmac('sha256', secret).update(body).digest('hex');
  return `sha256=${hmac}`;
}

// ============================================
// GET /health
// ============================================
describe('GET /health', () => {
  it('should return health status', async () => {
    const res = await fetch(`${baseUrl}/health`);
    const data = await res.json();

    expect(res.status).toBe(200);
    expect(data.service).toBe('webhook-receiver');
    expect(data.status).toBe('ok');
    expect(data).toHaveProperty('uptime');
    expect(data).toHaveProperty('rabbitmq');
  });
});

// ============================================
// GET /webhook — Verification Handshake
// ============================================
describe('GET /webhook', () => {
  it('should echo challenge when token matches', async () => {
    const params = new URLSearchParams({
      'hub.mode': 'subscribe',
      'hub.verify_token': 'test_verify_token',
      'hub.challenge': 'challenge_string_12345',
    });

    const res = await fetch(`${baseUrl}/webhook?${params}`);
    const text = await res.text();

    expect(res.status).toBe(200);
    expect(text).toBe('challenge_string_12345');
  });

  it('should reject when token does not match', async () => {
    const params = new URLSearchParams({
      'hub.mode': 'subscribe',
      'hub.verify_token': 'wrong_token',
      'hub.challenge': 'challenge_string',
    });

    const res = await fetch(`${baseUrl}/webhook?${params}`);
    expect(res.status).toBe(403);
  });

  it('should reject when mode is not subscribe', async () => {
    const params = new URLSearchParams({
      'hub.mode': 'unsubscribe',
      'hub.verify_token': 'test_verify_token',
      'hub.challenge': 'challenge_string',
    });

    const res = await fetch(`${baseUrl}/webhook?${params}`);
    expect(res.status).toBe(403);
  });
});

// ============================================
// POST /webhook — Event Ingestion
// ============================================
describe('POST /webhook', () => {
  it('should return 200 with valid signature', async () => {
    const body = JSON.stringify(commentFixture);
    const signature = computeSignature(body);

    const res = await fetch(`${baseUrl}/webhook`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Hub-Signature-256': signature,
      },
      body,
    });

    expect(res.status).toBe(200);
    const text = await res.text();
    expect(text).toBe('EVENT_RECEIVED');
  });

  it('should return 401 when signature header is missing', async () => {
    const res = await fetch(`${baseUrl}/webhook`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(commentFixture),
    });

    expect(res.status).toBe(401);
  });

  it('should return 403 when signature is invalid', async () => {
    const body = JSON.stringify(commentFixture);

    const res = await fetch(`${baseUrl}/webhook`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Hub-Signature-256': 'sha256=deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef',
      },
      body,
    });

    expect(res.status).toBe(403);
  });

  it('should return 403 when body is tampered after signing', async () => {
    const originalBody = JSON.stringify(commentFixture);
    const signature = computeSignature(originalBody);

    // Tamper with the body
    const tamperedBody = JSON.stringify({ ...commentFixture, object: 'tampered' });

    const res = await fetch(`${baseUrl}/webhook`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Hub-Signature-256': signature,
      },
      body: tamperedBody,
    });

    expect(res.status).toBe(403);
  });
});

// ============================================
// 404 handler
// ============================================
describe('Unknown routes', () => {
  it('should return 404 for unknown GET routes', async () => {
    const res = await fetch(`${baseUrl}/unknown-path`);
    expect(res.status).toBe(404);
  });
});
