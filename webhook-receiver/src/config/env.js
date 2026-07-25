'use strict';

/**
 * Environment configuration.
 * Centralizes all env var access with defaults and validation.
 */
const config = {
  port: parseInt(process.env.WEBHOOK_PORT, 10) || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  logLevel: process.env.LOG_LEVEL || 'info',

  meta: {
    appSecret: process.env.META_APP_SECRET,
    verifyToken: process.env.META_VERIFY_TOKEN,
  },

  rabbitmq: {
    url: process.env.RABBITMQ_URL || 'amqp://igdm_rabbit:changeme_rabbit_password@localhost:5672',
    exchange: 'ig.events',
    routingKey: 'comment.created',
    // Reconnect settings
    reconnectInterval: 5000,  // 5 seconds
    maxReconnectAttempts: 20,
  },
};

/**
 * Validate that critical config values are present.
 * Logs warnings in development, throws in production.
 */
function validateConfig() {
  const missing = [];

  if (!config.meta.appSecret) missing.push('META_APP_SECRET');
  if (!config.meta.verifyToken) missing.push('META_VERIFY_TOKEN');

  if (missing.length > 0) {
    const msg = `Missing required environment variables: ${missing.join(', ')}`;
    if (config.nodeEnv === 'production') {
      throw new Error(msg);
    }
    // In dev, we warn but continue (allows testing without Meta credentials)
    return missing;
  }

  return [];
}

module.exports = { config, validateConfig };
