package com.example.java_service.Configs;

import lombok.experimental.NonFinal;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig
{
    // Report queue config
    @NonFinal
    @Value("${rabbitmq.exchange.report}")
    public String EXCHANGE_REPORT;

    @NonFinal
    @Value("${rabbitmq.queue.report}")
    public String QUEUE_REPORT;

    @NonFinal
    @Value("${rabbitmq.routing.key.report}")
    public String ROUTING_KEY_REPORT;

    @Bean
    public DirectExchange reportExchange() {
        return new DirectExchange(EXCHANGE_REPORT);
    }

    @Bean
    public Queue reportQueue() {
        return new Queue(QUEUE_REPORT, true);
    }

    @Bean
    public Binding binding(Queue reportQueue, DirectExchange reportExchange) {
        return BindingBuilder.bind(reportQueue)
                .to(reportExchange)
                .with(ROUTING_KEY_REPORT);
    }

    // Notification queue config
    @NonFinal
    @Value("${rabbitmq.exchange.notification}")
    public String EXCHANGE_NOTIFICATION;

    @NonFinal
    @Value("${rabbitmq.queue.notification}")
    public String QUEUE_NOTIFICATION;

    @NonFinal
    @Value("${rabbitmq.routing.key.notification}")
    public String ROUTING_KEY_NOTIFICATION;

    @Bean("notificationExchange")
    public DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE_NOTIFICATION);
    }

    @Bean("notificationQueue")
    public Queue notificationQueue() {
        return new Queue(QUEUE_NOTIFICATION, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(ROUTING_KEY_NOTIFICATION);
    }
}