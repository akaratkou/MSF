package local.ims.task1.resource_processor.services;

import local.ims.task1.resource_processor.clients.ResourceServiceClient;
import local.ims.task1.resource_processor.clients.SongServiceClient;
import local.ims.task1.resource_processor.dto.SongMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceProcessingService {

    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;
    private final Mp3Service mp3Service;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void processResource(Integer resourceId) {
        // Step 1: fetch MP3 binary from resource-service
        log.info("Step 1 - Fetching MP3 binary for resourceId={}", resourceId);
        byte[] mp3Data = resourceServiceClient.getResourceById(resourceId);

        // Step 2: extract metadata with Tika
        log.info("Step 2 - Extracting metadata for resourceId={}", resourceId);
        SongMetadataDto metadata = mp3Service.extractMetadata(mp3Data);
        metadata.setId(resourceId);
        log.info("Extracted: name='{}', artist='{}', album='{}', duration='{}', year='{}'",
                metadata.getName(), metadata.getArtist(), metadata.getAlbum(),
                metadata.getDuration(), metadata.getYear());

        // Step 3: save metadata to song-service
        log.info("Step 3 - Saving metadata to song-service for resourceId={}", resourceId);
        Map<String, Object> response = songServiceClient.saveSongMetadata(metadata);
        log.info("Processing completed. resourceId={}, songId={}", resourceId, response.get("id"));
    }

    @Recover
    public void processResourceRecover(Exception e, Integer resourceId) {
        log.error("All Spring Retry attempts exhausted for resourceId={}. Rethrowing for AMQP retry layer. Error: {}",
                resourceId, e.getMessage());
        throw new RuntimeException("Processing failed for resourceId=" + resourceId, e);
    }
}

