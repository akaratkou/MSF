package local.ims.task1.resource_service.services;

import feign.FeignException;
import local.ims.task1.resource_service.dto.DeletedIdsDto;
import local.ims.task1.resource_service.dto.SongMetadataDto;
import local.ims.task1.resource_service.interfaces.MetadataServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {

    private final MetadataServiceClient metadataServiceClient;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public String createSongMetadata(SongMetadataDto metadata) {
        log.info("Attempting to create song metadata: {}", metadata);
        metadataServiceClient.createSongMetadata(metadata);
        log.info("Successfully created song metadata for id={}", metadata.getId());
        return "";
    }

    @Recover
    public String createSongMetadataRecover(FeignException e, SongMetadataDto metadata) {
        log.error("All retry attempts exhausted for creating song metadata id={}. Error: {} - {}",
                metadata.getId(), e.status(), e.contentUTF8());
        return e.contentUTF8();
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public List<Integer> deleteSongMetadataByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String idsCsv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        log.info("Attempting to delete song metadata for ids={}", idsCsv);
        DeletedIdsDto result = metadataServiceClient.deleteSongMetadata(idsCsv);
        log.info("Successfully deleted song metadata for ids={}", idsCsv);
        return result != null ? result.ids() : List.of();
    }

    @Recover
    public List<Integer> deleteSongMetadataByIdsRecover(FeignException e, List<Integer> ids) {
        log.error("All retry attempts exhausted for deleting song metadata ids={}. Error: {} - {}",
                ids, e.status(), e.contentUTF8());
        return List.of();
    }

    public List<Integer> deleteSongMetadata(String ids) {
        List<Integer> idList = List.of(ids.split(","))
                .stream()
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        return deleteSongMetadataByIds(idList);
    }
}
