'use strict';

const crypto = require('crypto');
const { config } = require('../config/env');
const logger = require('../utils/logger');

/**
 * Meta Webhook Signature Verification Middleware.
 *
 * Verifies the X-Hub-Signature-256 header against the raw request body
 * using HMAC-SHA256 with the Meta App Secret.
 *
 * CRITICAL: This middleware MUST run before any JSON body parser, because
 * it needs access to the raw request body bytes. We use express.json()
 * with a `verify` callback to capture the raw body before parsing.
 *
 * @see https://developers.facebook.com/docs/graph-api/webhooks/getting-started#verification-requests
 */
function verifySignature(req, res, next) {
  const signature = req.headers['x-hub-signature-256'];

  // No signature header = reject
  if (!signature) {
    logger.warn({ path: req.path, ip: req.ip }, 'Webhook request missing X-Hub-Signature-256 header');
    return res.status(401).json({ error: 'Missing signature header' });
  }

  // App secret not configured (should not happen in production)
  if (!config.meta.appSecret) {
    logger.error('META_APP_SECRET not configured — cannot verify webhook signatures');
    return res.status(500).json({ error: 'Server misconfigured' });
  }

  // The raw body is attached by the express.json() verify callback (see index.js)
  const rawBody = req.rawBody;
  if (!rawBody) {
    logger.error('Raw body not available for signature verification');
    return res.status(500).json({ error: 'Internal error' });
  }

  // Compute expected HMAC
  const expectedSignature = 'sha256=' + crypto
    .createHmac('sha256', config.meta.appSecret)
    .update(rawBody)
    .digest('hex');

  // Constant-time comparison to prevent timing attacks
  const isValid = signature.length === expectedSignature.length &&
    crypto.timingSafeEqual(
      Buffer.from(signature, 'utf8'),
      Buffer.from(expectedSignature, 'utf8')
    );

  if (!isValid) {
    logger.warn(
      { path: req.path, ip: req.ip },
      'Webhook signature verification FAILED — possible tampering'
    );
    return res.status(403).json({ error: 'Invalid signature' });
  }

  logger.debug({ path: req.path }, 'Webhook signature verified ✓');
  next();
}

module.exports = verifySignature;
