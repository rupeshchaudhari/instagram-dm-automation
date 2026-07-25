'use strict';

const amqplib = require('amqplib');
const { v4: uuidv4 } = require('uuid');
const { config } = require('../config/env');
const logger = require('../utils/logger');

/**
 * RabbitMQ Publisher Service.
 *
 * Manages a persistent connection to RabbitMQ with automatic reconnection.
 * Publishes webhook payloads to the ig.events exchange as persistent messages.
 *
 * Design decisions:
 * - Uses confirm channels for publish acknowledgement
 * - Messages are marked persistent (deliveryMode: 2)
 * - Automatic reconnection with exponential-ish backoff
 * - Connection is established lazily on first publish or via explicit connect()
 */
class QueuePublisher {
  constructor() {
    this.connection = null;
    this.channel = null;
    this.isConnecting = false;
    this.reconnectAttempts = 0;
  }

  /**
   * Establish connection to RabbitMQ.
   * Idempotent — safe to call multiple times.
   */
  async connect() {
    if (this.channel) return;
    if (this.isConnecting) return;

    this.isConnecting = true;

    try {
      logger.info({ url: config.rabbitmq.url.replace(/\/\/.*@/, '//***@') }, 'Connecting to RabbitMQ...');

      this.connection = await amqplib.connect(config.rabbitmq.url);
      this.channel = await this.connection.createConfirmChannel();

      // Assert exchange exists (idempotent, matches definitions.json)
      await this.channel.assertExchange(config.rabbitmq.exchange, 'topic', {
        durable: true,
      });

      this.reconnectAttempts = 0;
      logger.info('✓ Connected to RabbitMQ successfully');

      // Handle connection errors and closures
      this.connection.on('error', (err) => {
        logger.error({ err }, 'RabbitMQ connection error');
        this._scheduleReconnect();
      });

      this.connection.on('close', () => {
        logger.warn('RabbitMQ connection closed');
        this.channel = null;
        this.connection = null;
        this._scheduleReconnect();
      });

    } catch (err) {
      logger.error({ err }, 'Failed to connect to RabbitMQ');
      this.channel = null;
      this.connection = null;
      this._scheduleReconnect();
    } finally {
      this.isConnecting = false;
    }
  }

  /**
   * Schedule a reconnection attempt with backoff.
   */
  _scheduleReconnect() {
    if (this.reconnectAttempts >= config.rabbitmq.maxReconnectAttempts) {
      logger.error(
        { attempts: this.reconnectAttempts },
        'Max RabbitMQ reconnection attempts reached — giving up'
      );
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(
      config.rabbitmq.reconnectInterval * this.reconnectAttempts,
      30000 // Cap at 30 seconds
    );

    logger.info(
      { attempt: this.reconnectAttempts, delayMs: delay },
      'Scheduling RabbitMQ reconnection...'
    );

    setTimeout(() => this.connect(), delay);
  }

  /**
   * Publish a comment event to the message queue.
   *
   * @param {Object} webhookEntry - The raw Meta webhook entry object
   * @returns {Promise<boolean>} - true if published successfully
   */
  async publishCommentEvent(webhookEntry) {
    if (!this.channel) {
      logger.error('Cannot publish — RabbitMQ channel not available');
      return false;
    }

    const message = {
      messageId: uuidv4(),
      timestamp: new Date().toISOString(),
      eventType: 'comment.created',
      retryCount: 0,
      maxRetries: 3,
      payload: this._extractCommentPayload(webhookEntry),
      rawWebhookEntry: webhookEntry,
    };

    const buffer = Buffer.from(JSON.stringify(message));

    try {
      this.channel.publish(
        config.rabbitmq.exchange,
        config.rabbitmq.routingKey,
        buffer,
        {
          persistent: true,          // deliveryMode: 2
          contentType: 'application/json',
          messageId: message.messageId,
          timestamp: Math.floor(Date.now() / 1000),
        }
      );

      // Wait for broker confirmation
      await this.channel.waitForConfirms();

      logger.info(
        {
          messageId: message.messageId,
          commentId: message.payload?.commentId,
          igAccountId: message.payload?.igAccountId,
        },
        'Published comment event to queue ✓'
      );

      return true;

    } catch (err) {
      logger.error({ err, messageId: message.messageId }, 'Failed to publish message to RabbitMQ');
      return false;
    }
  }

  /**
   * Extract structured comment data from the raw Meta webhook entry.
   *
   * Meta webhook payload structure for Instagram comments:
   * {
   *   "id": "<IG_USER_ID>",
   *   "time": 1234567890,
   *   "changes": [{
   *     "field": "comments",
   *     "value": {
   *       "from": { "id": "...", "username": "..." },
   *       "media": { "id": "...", "media_product_type": "..." },
   *       "id": "<COMMENT_ID>",
   *       "text": "I want the link!"
   *     }
   *   }]
   * }
   */
  _extractCommentPayload(entry) {
    try {
      const change = entry.changes?.[0];
      const value = change?.value || {};

      return {
        igAccountId: entry.id,
        mediaId: value.media?.id || null,
        commentId: value.id || null,
        commentText: value.text || '',
        commentTimestamp: entry.time
          ? new Date(entry.time * 1000).toISOString()
          : new Date().toISOString(),
        commenter: {
          id: value.from?.id || null,
          username: value.from?.username || null,
        },
      };
    } catch (err) {
      logger.warn({ err, entryId: entry.id }, 'Failed to extract comment payload — using raw entry');
      return {
        igAccountId: entry.id,
        mediaId: null,
        commentId: null,
        commentText: '',
        commentTimestamp: new Date().toISOString(),
        commenter: { id: null, username: null },
      };
    }
  }

  /**
   * Check if the publisher is connected and ready.
   */
  isReady() {
    return this.channel !== null;
  }

  /**
   * Gracefully close the connection.
   */
  async close() {
    try {
      if (this.channel) await this.channel.close();
      if (this.connection) await this.connection.close();
      logger.info('RabbitMQ connection closed gracefully');
    } catch (err) {
      logger.error({ err }, 'Error closing RabbitMQ connection');
    } finally {
      this.channel = null;
      this.connection = null;
    }
  }
}

// Export a singleton instance
module.exports = new QueuePublisher();
