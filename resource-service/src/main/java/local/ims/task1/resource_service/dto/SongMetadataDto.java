package local.ims.task1.resource_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongMetadataDto {

    private Integer id;
    private String name;
    private String artist;
    private String album;
    private String duration;
    private String year;


}
