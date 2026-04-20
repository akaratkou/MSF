package local.ims.task1.song_service.service;

import local.ims.task1.song_service.dto.SongDto;
import local.ims.task1.song_service.exception.InputDataValidationException;
import local.ims.task1.song_service.exception.ResourceNotFoundException;
import local.ims.task1.song_service.model.Song;
import local.ims.task1.song_service.repository.SongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class SongServiceImpl implements SongService {

    public static final int MAX_CSV_LENGTH = 200;
    public static final String CSV_LENGTH_ERROR_MESSAGE_FORMAT = "CSV string is too long: received %d characters, maximum allowed is " + MAX_CSV_LENGTH;
    @Autowired
    private SongRepository songRepository;

    @Override
    public Integer createSong(SongDto songDto) {
        if (songRepository.existsById(songDto.getId())) {
            throw new DataIntegrityViolationException(String.format("Metadata for resource ID=%d already exists", songDto.getId()));
        }
        Song song = new Song();
        song.setId(songDto.getId());
        song.setName(songDto.getName());
        song.setArtist(songDto.getArtist());
        song.setAlbum(songDto.getAlbum());
        song.setDuration(songDto.getDuration());
        song.setYear(songDto.getYear());
        songRepository.save(song);
        return song.getId();
    }

    @Override
    public SongDto getSongById(Integer id) {
        if (id <= 0) {
            throw new InputDataValidationException("ID must be a positive integer");
        }

        Song song = songRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Song metadata for ID=" + id + " not found"));
        SongDto songDto = new SongDto();
        songDto.setId(song.getId());
        songDto.setName(song.getName());
        songDto.setArtist(song.getArtist());
        songDto.setAlbum(song.getAlbum());
        songDto.setDuration(song.getDuration());
        songDto.setYear(song.getYear());
        return songDto;
    }

    @Override
    public List<Integer> deleteSongs(String ids) {
        if (ids.length() > MAX_CSV_LENGTH) {
            throw new InputDataValidationException(String.format(CSV_LENGTH_ERROR_MESSAGE_FORMAT, ids.length()));
        }
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(part -> {
                    try {
                        return Integer.parseInt(part.trim());
                    } catch (NumberFormatException _) {
                        throw new InputDataValidationException(
                                String.format("Invalid ID format: '%s'. Only positive integers are allowed", part)
                        );
                    }
                })
                .toList();

        List<Integer> deletedIds = new java.util.ArrayList<>();
        for (Integer id : idList) {
            try {
                if (songRepository.existsById(id)) {
                    songRepository.deleteById(id);
                    deletedIds.add(id);
                }
            } catch (Exception e) {
                log.error("Could not delete song with id: {} ", id, e);
            }
        }

        return deletedIds;
    }
}

