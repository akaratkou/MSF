package local.ims.task1.resource_service.services;

import local.ims.task1.resource_service.dto.SongMetadataDto;
import local.ims.task1.resource_service.entities.Mp3Resource;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mp3Service {
    public static final int MAX_CSV_LENGTH = 200;
    public static final int SECONDS_IN_ONE_MINUTE = 60;
    private final Mp3ResourceRepository repository;
    private final MetadataService metadataService;
    private final S3Service s3Service;
    @Value("${files.s3.bucket.name:resource-bucket}")
    String bucketName;


    @Transactional
    public Integer saveMp3(byte[] mp3Data) throws NoSuchAlgorithmException {
        if (mp3Data == null || mp3Data.length == 0) {
            throw new InputDataBaseRequestException("MP3 data must not be empty");
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(mp3Data);
        String shaStr = bytesToHex(encodedhash);
        String location = "s3://" + bucketName + "/" + shaStr + "_" + UUID.randomUUID();

        s3Service.uploadBytes(bucketName, location, mp3Data);

        Mp3Resource resource = new Mp3Resource();
        resource.setLocation(location);

        resource = repository.save(resource);


//        SongMetadataDto songMetadataDto = extractMp3Headers(mp3Data);
//        songMetadataDto.setId(resource.getId());
//        songMetadataDto.setDuration(durationInFormatMMSS(songMetadataDto.getDuration()));

//        if (!metadataService.createSongMetadata(songMetadataDto).isEmpty()) {
//            log.warn("Failed to create song metadata for resource ID={}. Deleting the resource.", resource.getId());
//            repository.deleteById(resource.getId());
//            throw new BaseRequestException("Failed to create song metadata: " + metadataService.createSongMetadata(songMetadataDto));
//        }
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
        return s3Service.downloadFileAsBytes(bucketName, resourceOpt.get().getLocation());
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
        try {
            found.forEach(mp3Resource -> {
                s3Service.deleteFile(bucketName, mp3Resource.getLocation());
            });
        } catch (Exception e) {
            log.error("Error deleting files from S3 for resources with ids: {}", idsToDelete, e);
        }
        List<Integer> metadataForRemoveIds = found.stream().map(Mp3Resource::getId).toList();
        metadataService.deleteSongMetadataByIds(metadataForRemoveIds);

        return deleteMp3DataOneByOne(found);

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

    public List<Integer> deleteMp3DataOneByOne(List<Mp3Resource> mp3Resources) {
        List<Integer> deletedIds = new ArrayList<>();
        for (Mp3Resource resource : mp3Resources) {
            try {
                deleteFile(resource.getLocation());
                repository.deleteById(resource.getId());
                deletedIds.add(resource.getId());
            } catch (Exception e) {
                log.error("Could not delete resource with id: {}", resource.getId(), e);
            }
        }
        return deletedIds;
    }

    private void deleteFile(String s3Location) throws URISyntaxException {
        URI uri = new URI(s3Location);
        String bucket = uri.getHost();
        String key = uri.getPath().substring(1);
        s3Service.deleteFile(bucket, key);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


}