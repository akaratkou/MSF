package local.ims.task1.resource_service.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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

    @Retryable(
            retryFor = AmqpException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void sendResourceUploaded(Integer resourceId) {
        Map<String, Object> message = Map.of("resourceId", resourceId);
        log.info("Sending resource uploaded message for resourceId={} to exchange={}", resourceId, exchange);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Message sent successfully for resourceId={}", resourceId);
    }

    @Recover
    public void sendResourceUploadedRecover(AmqpException e, Integer resourceId) {
        log.error("All retry attempts exhausted for sending message resourceId={}. The message was NOT published. Error: {}",
                resourceId, e.getMessage());
    }
}
