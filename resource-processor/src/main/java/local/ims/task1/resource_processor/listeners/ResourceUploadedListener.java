package local.ims.task1.resource_processor.listeners;

import local.ims.task1.resource_processor.clients.ResourceServiceClient;
import local.ims.task1.resource_processor.clients.SongServiceClient;
import local.ims.task1.resource_processor.dto.SongMetadataDto;
import local.ims.task1.resource_processor.services.Mp3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceUploadedListener {

    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;
    private final Mp3Service mp3Service;

    @RabbitListener(queues = "${rabbitmq.queue.resource-uploaded:resource.uploaded}")
    public void handleResourceUploaded(Map<String, Object> message) {
        Object resourceIdObj = message.get("resourceId");
        log.info("Received resource uploaded message: resourceId={}", resourceIdObj);

        if (resourceIdObj == null) {
            log.warn("Received message with null resourceId, skipping processing");
            return;
        }

        Integer resourceId = ((Number) resourceIdObj).intValue();

        // Step 1: fetch MP3 binary from resource-service
        log.info("Step 1 - Fetching MP3 binary for resourceId={}", resourceId);
        byte[] mp3Data = resourceServiceClient.getResourceById(resourceId);

        // Step 2: extract metadata with Tika
        log.info("Step 2 - Extracting metadata from MP3 for resourceId={}", resourceId);
        SongMetadataDto metadata = mp3Service.extractMetadata(mp3Data);
        metadata.setId(resourceId);
        log.info("Extracted metadata: name='{}', artist='{}', album='{}', duration='{}', year='{}'",
                metadata.getName(), metadata.getArtist(), metadata.getAlbum(),
                metadata.getDuration(), metadata.getYear());

        // Step 3: save metadata to song-service
        log.info("Step 3 - Saving song metadata to song-service for resourceId={}", resourceId);
        Map<String, Object> response = songServiceClient.saveSongMetadata(metadata);
        log.info("Resource processing completed. resourceId={}, songId={}", resourceId, response.get("id"));
    }
}
