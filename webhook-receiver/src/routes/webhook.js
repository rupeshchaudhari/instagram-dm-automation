'use strict';

const { Router } = require('express');
const verifySignature = require('../middleware/verifySignature');
const queuePublisher = require('../services/queuePublisher');
const { config } = require('../config/env');
const logger = require('../utils/logger');

const router = Router();

/**
 * GET /webhook — Meta Verification Handshake.
 *
 * When you configure a webhook in the Meta App Dashboard, Meta sends a GET
 * request with these query params to verify you own the endpoint:
 *   - hub.mode        = "subscribe"
 *   - hub.verify_token = your custom token (set in Meta dashboard)
 *   - hub.challenge    = a random string you must echo back
 *
 * @see https://developers.facebook.com/docs/graph-api/webhooks/getting-started#verification-requests
 */
router.get('/webhook', (req, res) => {
  const mode = req.query['hub.mode'];
  const token = req.query['hub.verify_token'];
  const challenge = req.query['hub.challenge'];

  logger.info({ mode, hasToken: !!token, hasChallenge: !!challenge }, 'Webhook verification request');

  if (mode === 'subscribe' && token === config.meta.verifyToken) {
    logger.info('✓ Webhook verification successful — echoing challenge');
    return res.status(200).send(challenge);
  }

  logger.warn({ mode, ip: req.ip }, '✗ Webhook verification failed — token mismatch');
  return res.status(403).json({ error: 'Verification failed' });
});

/**
 * POST /webhook — Receive Instagram Events.
 *
 * CRITICAL FLOW (must complete in <2 seconds):
 *   1. Signature already verified by verifySignature middleware
 *   2. Parse the payload
 *   3. For each entry with comment changes → publish to RabbitMQ
 *   4. Return 200 OK immediately
 *
 * We do NOT await the publish — we fire-and-forget to the queue
 * to ensure we respond within Meta's timeout window. The confirm
 * channel in queuePublisher handles delivery guarantees independently.
 *
 * Meta payload structure:
 * {
 *   "object": "instagram",
 *   "entry": [
 *     {
 *       "id": "<IG_USER_ID>",
 *       "time": 1234567890,
 *       "changes": [{ "field": "comments", "value": { ... } }]
 *     }
 *   ]
 * }
 */
router.post('/webhook', verifySignature, (req, res) => {
  // Respond 200 IMMEDIATELY — before any processing
  // This is the most critical constraint: Meta requires < 2s response
  res.status(200).send('EVENT_RECEIVED');

  // Now process asynchronously (response already sent)
  const body = req.body;

  if (body.object !== 'instagram') {
    logger.debug({ object: body.object }, 'Ignoring non-Instagram webhook event');
    return;
  }

  const entries = body.entry || [];
  logger.info({ entryCount: entries.length }, 'Processing Instagram webhook entries');

  for (const entry of entries) {
    const changes = entry.changes || [];

    for (const change of changes) {
      // Only process comment events
      if (change.field !== 'comments') {
        logger.debug(
          { field: change.field, entryId: entry.id },
          'Skipping non-comment change'
        );
        continue;
      }

      // Fire-and-forget publish to RabbitMQ
      // Error handling is internal to the publisher (logged, not thrown)
      queuePublisher.publishCommentEvent(entry).catch((err) => {
        logger.error(
          { err, entryId: entry.id, commentId: change.value?.id },
          'Failed to queue comment event (async)'
        );
      });
    }
  }
});

module.exports = router;
