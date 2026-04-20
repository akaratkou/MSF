package local.ims.task1.resource_service.services;

import local.ims.task1.resource_service.dto.SongMetadataDto;
import local.ims.task1.resource_service.entities.Mp3Resource;
import local.ims.task1.resource_service.exceptions.BaseRequestException;
import local.ims.task1.resource_service.exceptions.InputDataBaseRequestException;
import local.ims.task1.resource_service.exceptions.ResourceNotFoundException;
import local.ims.task1.resource_service.repositories.Mp3ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.LyricsHandler;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mp3Service {
    public static final int MAX_CSV_LENGTH = 200;
    public static final int SECONDS_IN_ONE_MINUTE = 60;
    private final Mp3ResourceRepository repository;
    private final MetadataService metadataService;


    @Transactional
    public Integer saveMp3(byte[] mp3Data) {
        if (mp3Data == null || mp3Data.length == 0) {
            throw new InputDataBaseRequestException("MP3 data must not be empty");
        }
        Mp3Resource resource = new Mp3Resource(mp3Data);
        resource = repository.save(resource);


        SongMetadataDto songMetadataDto = extractMp3Headers(mp3Data);
        songMetadataDto.setId(resource.getId());
        songMetadataDto.setDuration(durationInFormatMMSS(songMetadataDto.getDuration()));

        if (!metadataService.createSongMetadata(songMetadataDto).isEmpty()) {
            log.warn("Failed to create song metadata for resource ID={}. Deleting the resource.", resource.getId());
            repository.deleteById(resource.getId());
            throw new BaseRequestException("Failed to create song metadata: " + metadataService.createSongMetadata(songMetadataDto));
        }
        return resource.getId();
    }

    @Transactional(readOnly = true)
    public byte[] getMp3ById(int id) {
        if (id <= 0) {
            throw new InputDataBaseRequestException("ID must be a positive integer");
        }
        Optional<Mp3Resource> resourceOpt = repository.findById(id);
        if (resourceOpt.isEmpty()) {
            throw new ResourceNotFoundException("Resource with ID=" + id + " not found");
        }
        return resourceOpt.get().getData();
    }

    @Transactional
    public List<Integer> deleteResourcesWithCascading(String idsCsv) {
        if (idsCsv == null || idsCsv.isBlank()) {
            return List.of();
        }

        if (idsCsv.length() >= MAX_CSV_LENGTH) {
            throw new InputDataBaseRequestException(String.format("CSV string is too long: received %d characters, maximum allowed is %d", idsCsv.length(), MAX_CSV_LENGTH));
        }

        List<Integer> idsToDelete = Arrays.stream(idsCsv.split(","))
                .map(String::trim)
                .map(part -> {
                    try {
                        return Integer.parseInt(part);
                    } catch (NumberFormatException _) {
                        throw new InputDataBaseRequestException(
                                String.format("Invalid ID format: '%s'. Only positive integers are allowed", part)
                        );
                    }
                })
                .toList();

        List<Mp3Resource> found = repository.findAllById(idsToDelete);
        List<Integer> metadataForRemoveIds = found.stream().map(Mp3Resource::getId).toList();
        List<Integer> metadataRemovedIds = metadataService.deleteSongMetadataByIds(metadataForRemoveIds);

        return deleteMp3DataOneByOne(metadataRemovedIds);
    }


    private SongMetadataDto extractMp3Headers(byte[] mp3Data) {
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext pcontext = new ParseContext();
        InputStream inputstream = new ByteArrayInputStream(mp3Data);

        Mp3Parser mp3Parser = new Mp3Parser();
        LyricsHandler lyrics;
        try {
            mp3Parser.parse(inputstream, handler, metadata, pcontext);
            lyrics = new LyricsHandler(inputstream, handler);
        } catch (IOException | SAXException | TikaException ioException) {
            throw new InputDataBaseRequestException("Failed to parse MP3 file: " + ioException.getMessage());
        }

        while (lyrics.hasLyrics()) {
            log.debug("Lyrics: {}", lyrics);
        }

        log.debug("Contents of the document: {}", handler);
        log.debug("Metadata of the document");

        SongMetadataDto songMetadataDto = new SongMetadataDto();
        songMetadataDto.setAlbum(metadata.get("xmpDM:album"));
        songMetadataDto.setArtist(metadata.get("xmpDM:artist"));
        songMetadataDto.setDuration(metadata.get("xmpDM:duration"));
        songMetadataDto.setName(metadata.get("dc:title"));
        songMetadataDto.setYear(metadata.get("xmpDM:releaseDate"));
        return songMetadataDto;
    }

    private String durationInFormatMMSS(String durationStr) {
        try {
            double durationSeconds = Double.parseDouble(durationStr);
            int minutes = (int) (durationSeconds / SECONDS_IN_ONE_MINUTE);
            int seconds = (int) (durationSeconds % 60);
            return String.format("%02d:%02d", minutes, seconds);
        } catch (NumberFormatException _) {
            return "00:00";
        }
    }

    public List<Integer> deleteMp3DataOneByOne(List<Integer> idList) {
        List<Integer> deletedIds = new ArrayList<>();
        for (Integer id : idList) {
            try {
                if (repository.existsById(id)) {
                    repository.deleteById(id);
                    deletedIds.add(id);
                }
            } catch (Exception e) {
                log.error("Could not delete resource with id: {}", id, e);
            }
        }
        return deletedIds;
    }


}