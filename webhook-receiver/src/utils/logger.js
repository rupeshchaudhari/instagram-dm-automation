'use strict';

const pino = require('pino');
const { config } = require('../config/env');

/**
 * Structured JSON logger using pino.
 * - Ultra-fast (pino is ~5x faster than winston)
 * - JSON output in production, pretty-printed in development
 * - Includes service name and environment in every log line
 */
const logger = pino({
  level: config.logLevel,
  transport: config.nodeEnv === 'development'
    ? { target: 'pino-pretty', options: { colorize: true, translateTime: 'SYS:standard' } }
    : undefined,
  base: {
    service: 'webhook-receiver',
    env: config.nodeEnv,
  },
  // Redact sensitive fields from logs
  redact: {
    paths: ['req.headers.authorization', 'req.headers["x-hub-signature-256"]'],
    censor: '[REDACTED]',
  },
  serializers: {
    err: pino.stdSerializers.err,
    req: (req) => ({
      method: req.method,
      url: req.url,
      remoteAddress: req.ip,
    }),
  },
});

module.exports = logger;
