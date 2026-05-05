package local.ims.task1.resource_processor.clients;

import local.ims.task1.resource_processor.dto.SongMetadataDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "${services.song-service.name:song-service}")
public interface SongServiceClient {

    @PostMapping(value = "/songs", consumes = "application/json")
    Map<String, Object> saveSongMetadata(@RequestBody SongMetadataDto songMetadataDto);
}
