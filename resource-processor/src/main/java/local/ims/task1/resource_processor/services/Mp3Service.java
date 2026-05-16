package local.ims.task1.resource_processor.services;

import local.ims.task1.resource_processor.dto.SongMetadataDto;
import local.ims.task1.resource_processor.exceptions.InputDataBaseRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.LyricsHandler;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class Mp3Service {
    public static final int SECONDS_IN_ONE_MINUTE = 60;

    public SongMetadataDto extractMetadata(byte[] mp3Data) {
        if (mp3Data == null || mp3Data.length == 0) {
            throw new InputDataBaseRequestException("MP3 data must not be empty");
        }
        return extractMp3Headers(mp3Data);
    }

    public SongMetadataDto testMp3(byte[] mp3Data) throws NoSuchAlgorithmException {
        if (mp3Data == null || mp3Data.length == 0) {
            throw new InputDataBaseRequestException("MP3 data must not be empty");
        }

        SongMetadataDto songMetadataDto = extractMp3Headers(mp3Data);
        return songMetadataDto;

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
        songMetadataDto.setDuration(durationInFormatMMSS(metadata.get("xmpDM:duration")));
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
        } catch (NumberFormatException e) {
            return "00:00";
        }
    }
}