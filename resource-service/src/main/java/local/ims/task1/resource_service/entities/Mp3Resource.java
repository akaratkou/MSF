package local.ims.task1.resource_service.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mp3_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mp3Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public Mp3Resource(byte[] data) {
        this.data = data;
    }
}

