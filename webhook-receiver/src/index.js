'use strict';

const express = require('express');
const { config, validateConfig } = require('./config/env');
const logger = require('./utils/logger');
const webhookRoutes = require('./routes/webhook');
const queuePublisher = require('./services/queuePublisher');

const app = express();

// ============================================
// Raw body capture for signature verification
// ============================================
// CRITICAL: express.json() must be configured with a `verify` callback
// that captures the raw Buffer BEFORE parsing. The signature middleware
// needs the raw bytes to compute the HMAC — parsed JSON won't match.
app.use(express.json({
  limit: '1mb',
  verify: (req, _res, buf) => {
    req.rawBody = buf;
  },
}));

// ============================================
// Health check (no auth, no signature)
// ============================================
app.get('/health', (_req, res) => {
  const status = {
    status: 'ok',
    service: 'webhook-receiver',
    uptime: Math.floor(process.uptime()),
    rabbitmq: queuePublisher.isReady() ? 'connected' : 'disconnected',
    timestamp: new Date().toISOString(),
  };

  const httpCode = queuePublisher.isReady() ? 200 : 503;
  res.status(httpCode).json(status);
});

// ============================================
// Webhook routes
// ============================================
app.use('/', webhookRoutes);

// ============================================
// 404 handler
// ============================================
app.use((_req, res) => {
  res.status(404).json({ error: 'Not found' });
});

// ============================================
// Global error handler
// ============================================
app.use((err, _req, res, _next) => {
  logger.error({ err }, 'Unhandled express error');
  res.status(500).json({ error: 'Internal server error' });
});

// ============================================
// Startup
// ============================================
async function start() {
  // Validate config
  const missingVars = validateConfig();
  if (missingVars.length > 0) {
    logger.warn({ missing: missingVars }, '⚠ Missing env vars — some features will be disabled');
  }

  // Connect to RabbitMQ (non-blocking — reconnects automatically)
  try {
    await queuePublisher.connect();
  } catch (err) {
    logger.warn({ err }, 'Initial RabbitMQ connection failed — will retry in background');
  }

  // Start HTTP server
  const server = app.listen(config.port, () => {
    logger.info(
      { port: config.port, env: config.nodeEnv },
      `🚀 Webhook receiver started on port ${config.port}`
    );
  });

  // Graceful shutdown
  const shutdown = async (signal) => {
    logger.info({ signal }, 'Received shutdown signal — closing gracefully');
    server.close(async () => {
      await queuePublisher.close();
      logger.info('Server closed. Goodbye.');
      process.exit(0);
    });

    // Force shutdown after 10 seconds
    setTimeout(() => {
      logger.error('Forced shutdown after timeout');
      process.exit(1);
    }, 10000);
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));

  // Catch unhandled rejections
  process.on('unhandledRejection', (reason) => {
    logger.error({ reason }, 'Unhandled promise rejection');
  });
}

start();

module.exports = app; // Export for testing
