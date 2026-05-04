package local.ims.task1.resource_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.resource-uploaded:resource.uploaded}")
    private String queueName;

    @Value("${rabbitmq.exchange.resource:resource.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.resource-uploaded:resource.uploaded}")
    private String routingKey;

    @Bean
    public Queue resourceUploadedQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public DirectExchange resourceExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding resourceUploadedBinding(Queue resourceUploadedQueue, DirectExchange resourceExchange) {
        return BindingBuilder.bind(resourceUploadedQueue).to(resourceExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}

