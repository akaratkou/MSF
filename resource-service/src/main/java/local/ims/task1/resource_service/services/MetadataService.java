package local.ims.task1.resource_service.services;

import feign.FeignException;
import local.ims.task1.resource_service.dto.DeletedIdsDto;
import local.ims.task1.resource_service.dto.SongMetadataDto;
import local.ims.task1.resource_service.interfaces.MetadataServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {

    private final MetadataServiceClient metadataServiceClient;


    public String createSongMetadata(SongMetadataDto metadata) {
        try {
            metadataServiceClient.createSongMetadata(metadata);
            log.info("Successfully created song metadata: {}", metadata);
            return StringUtils.EMPTY;
        } catch (FeignException ex) {
            log.error("Error creating song metadata: {} - {}", ex.status(), ex.contentUTF8());
            return ex.contentUTF8();
        }
    }

    public List<Integer> deleteSongMetadata(String ids) {
        try {
            DeletedIdsDto deletedIdsDto = metadataServiceClient.deleteSongMetadata(ids);
            log.info("Successfully deleted song metadata with ids: {}", deletedIdsDto);
            if (deletedIdsDto == null) {
                log.warn("Received null response body when deleting song metadata with ids: {}", ids);
                return List.of();
            }
            return deletedIdsDto.ids();
        } catch (FeignException ex) {
            log.error("Error deleting song metadata: {} - {}", ex.status(), ex.contentUTF8());
            return List.of();
        }
    }

    public List<Integer> deleteSongMetadataByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String idsCsv = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return deleteSongMetadata(idsCsv);
    }
}
