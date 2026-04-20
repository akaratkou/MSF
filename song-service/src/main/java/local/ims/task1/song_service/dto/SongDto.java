package local.ims.task1.song_service.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({SongDto.FirstOrderValidation.class, SongDto.class})

public class SongDto {

    public interface FirstOrderValidation{}

    @NotNull(groups = FirstOrderValidation.class, message = "Id is required")
    private Integer id;

    @NotNull(groups = FirstOrderValidation.class, message = "Song name is required")
    @Size(min = 1, max = 100, message = "Song name must be between 1 and 100 characters")
    private String name;

    @NotNull(groups = FirstOrderValidation.class, message = "Artist name is required")
    @Size(min = 1, max = 100, message = "Artist name must be between 1 and 100 characters")
    private String artist;

    @NotNull(groups = FirstOrderValidation.class, message = "Album name is required")
    @Size(min = 1, max = 100, message = "Album name must be between 1 and 100 characters")
    private String album;

    @NotNull(groups = FirstOrderValidation.class,message = "Duration is required")
    @Pattern(regexp = "^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$", message = "Duration must be in mm:ss format with leading zeros")
    private String duration;

    @NotNull(groups = FirstOrderValidation.class,message = "Year is required")
    @Pattern(regexp = "^(19|20)\\d{2}$", message = "Year must be between 1900 and 2099")
    private String year;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}

