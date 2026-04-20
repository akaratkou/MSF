package local.ims.task1.song_service.service;

import local.ims.task1.song_service.dto.SongDto;

import java.util.List;

public interface SongService {
    Integer createSong(SongDto songDto);

    SongDto getSongById(Integer id);

    List<Integer> deleteSongs(String ids);
}

