package local.ims.task1.resource_service.interfaces;

import local.ims.task1.resource_service.dto.DeletedIdsDto;
import local.ims.task1.resource_service.dto.SongMetadataDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "song-service")
public interface MetadataServiceClient {
    @PostMapping("/songs")
    void createSongMetadata(@RequestBody SongMetadataDto metadata);

    @DeleteMapping("/songs")
    DeletedIdsDto deleteSongMetadata(@RequestParam("id") String ids);


}
