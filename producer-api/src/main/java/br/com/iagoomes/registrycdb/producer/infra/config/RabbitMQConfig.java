package br.com.iagoomes.registrycdb.producer.infra.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Value("${fixed-income.queue.name}")
    private String fixedIncomeQueueName;

    @Value("${fixed-income.queue.exchange}")
    private String fixedIncomeExchange;

    @Value("${fixed-income.queue.routing-key}")
    private String fixedIncomeRoutingKey;

    // Main Queue
    @Bean
    public Queue fixedIncomeQueue() {
        return QueueBuilder.durable(fixedIncomeQueueName)
                .build();
    }

    // Main Exchange
    @Bean
    public DirectExchange fixedIncomeExchange() {
        return new DirectExchange(fixedIncomeExchange);
    }

    // Bindings
    @Bean
    public Binding ordersBinding() {
        return BindingBuilder
                .bind(fixedIncomeQueue())
                .to(fixedIncomeExchange())
                .with(fixedIncomeRoutingKey);
    }

    // Message Converter
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

}