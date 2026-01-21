package hskl.cn.serverless.executor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXECUTION_QUEUE = "function.execution";
    public static final String EXECUTION_EXCHANGE = "function.exchange";
    public static final String EXECUTION_ROUTING_KEY = "function.execute";
    public static final String RESULT_QUEUE = "function.result";
    public static final String RESULT_ROUTING_KEY = "function.result";

    @Bean
    public Queue executionQueue() {
        return QueueBuilder.durable(EXECUTION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "function.dlq")
                .build();
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("function.dlq").build();
    }

    @Bean
    public DirectExchange executionExchange() {
        return new DirectExchange(EXECUTION_EXCHANGE);
    }

    @Bean
    public Binding executionBinding(@Qualifier("executionQueue") Queue executionQueue, DirectExchange executionExchange) {
        return BindingBuilder.bind(executionQueue).to(executionExchange).with(EXECUTION_ROUTING_KEY);
    }

    @Bean
    public Binding resultBinding(@Qualifier("resultQueue") Queue resultQueue, DirectExchange executionExchange) {
        return BindingBuilder.bind(resultQueue).to(executionExchange).with(RESULT_ROUTING_KEY);
    }

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