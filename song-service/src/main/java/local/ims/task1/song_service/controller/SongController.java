package local.ims.task1.song_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import local.ims.task1.song_service.dto.DeletedIdsDto;
import local.ims.task1.song_service.dto.ResourceIdDto;
import local.ims.task1.song_service.dto.SongDto;
import local.ims.task1.song_service.service.SongService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/songs")
@Validated
@Slf4j
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @PostMapping
    public ResponseEntity<ResourceIdDto> createSong(@Valid @RequestBody SongDto songDto) {
        log.info("POST /songs - Creating song with resource ID: {}", songDto.getId());
        ResourceIdDto result = new ResourceIdDto(songService.createSong(songDto));
        log.info("POST /songs - Song created successfully with ID: {}", result.id());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongDto> getSongById(@PathVariable @Min(value = 1, message = "Must be a positive integer") Integer id) {
        log.info("GET /songs/{} - Retrieving song by ID", id);
        SongDto songDto = songService.getSongById(id);
        log.info("GET /songs/{} - Song retrieved successfully: {}", id, songDto.getName());
        return new ResponseEntity<>(songDto, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<DeletedIdsDto> deleteSongs(@RequestParam String id) {
        log.info("DELETE /songs - Deleting songs with IDs: {}", id);
        DeletedIdsDto result = new DeletedIdsDto(songService.deleteSongs(id));
        log.info("DELETE /songs - Deleted {} song(s): {}", result.ids().size(), result.ids());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}

