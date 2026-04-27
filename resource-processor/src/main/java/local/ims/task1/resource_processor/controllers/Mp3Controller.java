package local.ims.task1.resource_processor.controllers;

import local.ims.task1.resource_processor.dto.SongMetadataDto;
import local.ims.task1.resource_processor.services.Mp3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
public class Mp3Controller {
    private final Mp3Service mp3Service;


    @PostMapping(value = "/resources", consumes = "audio/mpeg", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SongMetadataDto> testMp3(@RequestBody byte[] mp3Data) throws NoSuchAlgorithmException {
        log.info("Received request to test MP3 file, size: {} bytes", mp3Data.length);
        SongMetadataDto result = mp3Service.testMp3(mp3Data);
        log.info("Successfully tested MP3 file");
        return ResponseEntity.ok(result);
    }

}
