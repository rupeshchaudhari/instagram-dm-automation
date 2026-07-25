package com.igdm.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration.
 *
 * Declares exchanges, queues, and bindings as a safety net — these should
 * already exist from rabbitmq/definitions.json, but Spring's declarations
 * are idempotent and act as documentation-in-code.
 *
 * Topology:
 *   ig.events (topic) --[comment.created]--> ig.comments.process
 *   ig.comments.process --[reject/nack]--> ig.events.retry (direct)
 *   ig.events.retry --[comment.retry]--> ig.comments.retry (TTL → DLX back to ig.events)
 *   ig.events.dlx (direct) --[comment.dead]--> ig.comments.dead-letter
 */
@Configuration
public class RabbitConfig {

    // --- Exchange Names ---
    public static final String EVENTS_EXCHANGE = "ig.events";
    public static final String RETRY_EXCHANGE = "ig.events.retry";
    public static final String DLX_EXCHANGE = "ig.events.dlx";

    // --- Queue Names ---
    public static final String PROCESS_QUEUE = "ig.comments.process";
    public static final String RETRY_QUEUE = "ig.comments.retry";
    public static final String DEAD_LETTER_QUEUE = "ig.comments.dead-letter";

    // --- Routing Keys ---
    public static final String COMMENT_CREATED_KEY = "comment.created";
    public static final String COMMENT_RETRY_KEY = "comment.retry";
    public static final String COMMENT_DEAD_KEY = "comment.dead";

    // --- Exchanges ---

    @Bean
    public TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange retryExchange() {
        return ExchangeBuilder.directExchange(RETRY_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    // --- Queues ---

    @Bean
    public Queue processQueue() {
        return QueueBuilder.durable(PROCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", RETRY_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", COMMENT_RETRY_KEY)
                .build();
    }

    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", COMMENT_CREATED_KEY)
                .withArgument("x-message-ttl", 5000)  // Default 5s TTL
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // --- Bindings ---

    @Bean
    public Binding processBinding() {
        return BindingBuilder.bind(processQueue()).to(eventsExchange()).with(COMMENT_CREATED_KEY);
    }

    @Bean
    public Binding retryBinding() {
        return BindingBuilder.bind(retryQueue()).to(retryExchange()).with(COMMENT_RETRY_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(dlxExchange()).with(COMMENT_DEAD_KEY);
    }

    // --- Message Converter (JSON) ---

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(10);
        // Manual ack for explicit control over retry/reject
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
