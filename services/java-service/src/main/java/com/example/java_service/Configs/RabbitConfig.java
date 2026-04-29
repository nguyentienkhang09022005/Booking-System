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
