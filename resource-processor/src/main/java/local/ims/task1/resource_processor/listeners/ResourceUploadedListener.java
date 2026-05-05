package local.ims.task1.resource_processor.listeners;

import local.ims.task1.resource_processor.services.ResourceProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceUploadedListener {

    private final ResourceProcessingService resourceProcessingService;

    @RabbitListener(queues = "${rabbitmq.queue.resource-uploaded:resource.uploaded}")
    public void handleResourceUploaded(Map<String, Object> message) {
        Object resourceIdObj = message.get("resourceId");
        log.info("Received resource uploaded message: resourceId={}", resourceIdObj);

        if (resourceIdObj == null) {
            log.warn("Received message with null resourceId, skipping processing");
            return;
        }

        Integer resourceId = ((Number) resourceIdObj).intValue();
        resourceProcessingService.processResource(resourceId);
    }
}
