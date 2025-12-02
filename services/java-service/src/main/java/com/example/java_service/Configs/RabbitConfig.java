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
    @Value("${rabbitmq.exchange.name}")
    public String EXCHANGE_NAME;

    @NonFinal
    @Value("${rabbitmq.queue.name}")
    public String QUEUE_NAME;

    @NonFinal
    @Value("${rabbitmq.routing.key}")
    public String ROUTING_KEY;

    @Bean
    public DirectExchange expenseExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue expenseQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue expenseQueue, DirectExchange expenseExchange) {
        return BindingBuilder.bind(expenseQueue)
                .to(expenseExchange)
                .with(ROUTING_KEY);
    }
}