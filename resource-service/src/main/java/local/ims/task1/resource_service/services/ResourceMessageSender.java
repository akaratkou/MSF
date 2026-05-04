package local.ims.task1.resource_service.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMessageSender {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.resource:resource.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.resource-uploaded:resource.uploaded}")
    private String routingKey;

    public void sendResourceUploaded(Integer resourceId) {
        Map<String, Object> message = Map.of("resourceId", resourceId);
        log.info("Sending resource uploaded message for resourceId={} to exchange={}", resourceId, exchange);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Message sent successfully for resourceId={}", resourceId);
    }
}

