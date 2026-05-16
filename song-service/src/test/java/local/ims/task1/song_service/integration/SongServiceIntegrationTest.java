package local.ims.task1.song_service.integration;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import local.ims.task1.song_service.dto.SongDto;
import local.ims.task1.song_service.model.Song;
import local.ims.task1.song_service.repository.SongRepository;
import local.ims.task1.song_service.service.SongService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Song Service.
 * Tests actual integration with PostgreSQL using Testcontainers.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Song Service Integration Tests")
class SongServiceIntegrationTest {

    @BeforeAll
    static void setupDocker() {
        // Force Docker API version configuration
        System.setProperty("DOCKER_HOST", "tcp://127.0.0.1:2375");
        System.setProperty("DOCKER_TLS_VERIFY", "0");
        System.setProperty("DOCKER_API_VERSION", "1.44");

        // Alternative: set via DockerClientConfig
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("tcp://127.0.0.1:2375")
            .withDockerTlsVerify(false)
            .withApiVersion("1.44")
            .build();
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("song_db_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        songRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create and retrieve song metadata")
    void testCreateAndRetrieveSong() {
        // Given: Song metadata
        SongDto songDto = createTestSongDto(1, "Hey Jude", "The Beatles", "Hey Jude", "04:03", "1968");

        // When: Create song
        Integer songId = songService.createSong(songDto);

        // Then: Song created in database
        assertThat(songId).isEqualTo(1);

        // And: Can retrieve song
        SongDto retrieved = songService.getSongById(songId);
        assertThat(retrieved.getId()).isEqualTo(songId);
        assertThat(retrieved.getName()).isEqualTo("Hey Jude");
        assertThat(retrieved.getArtist()).isEqualTo("The Beatles");
        assertThat(retrieved.getAlbum()).isEqualTo("Hey Jude");
        assertThat(retrieved.getDuration()).isEqualTo("04:03");
        assertThat(retrieved.getYear()).isEqualTo("1968");
    }

    @Test
    @DisplayName("Should enforce unique constraint on song ID")
    void testUniqueSongId() {
        // Given: Song already exists
        SongDto songDto = createTestSongDto(1, "Song 1", "Artist 1", "Album 1", "03:00", "2020");
        songService.createSong(songDto);

        // When/Then: Creating song with same ID throws exception
        SongDto duplicateDto = createTestSongDto(1, "Song 2", "Artist 2", "Album 2", "04:00", "2021");
        assertThatThrownBy(() -> songService.createSong(duplicateDto))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Metadata for resource ID=1 already exists");
    }

    @Test
    @DisplayName("Should persist song with minimal metadata")
    void testSongWithMinimalMetadata() {
        // Given: Song with only required fields
        SongDto minimalDto = new SongDto();
        minimalDto.setId(10);
        minimalDto.setName("Minimal Song");

        // When: Create song
        Integer songId = songService.createSong(minimalDto);

        // Then: Song persisted successfully
        assertThat(songId).isEqualTo(10);
        Song song = songRepository.findById(songId).orElseThrow();
        assertThat(song.getName()).isEqualTo("Minimal Song");
        assertThat(song.getArtist()).isNull();
        assertThat(song.getAlbum()).isNull();
    }

    @Test
    @DisplayName("Should persist song with all metadata fields")
    void testSongWithAllMetadata() {
        // Given: Song with all fields populated
        SongDto fullDto = createTestSongDto(20, "Complete Song", "Full Artist", "Full Album", "05:30", "2023");

        // When: Create song
        Integer songId = songService.createSong(fullDto);

        // Then: All fields persisted correctly
        Song song = songRepository.findById(songId).orElseThrow();
        assertThat(song.getName()).isEqualTo("Complete Song");
        assertThat(song.getArtist()).isEqualTo("Full Artist");
        assertThat(song.getAlbum()).isEqualTo("Full Album");
        assertThat(song.getDuration()).isEqualTo("05:30");
        assertThat(song.getYear()).isEqualTo("2023");
    }

    @Test
    @DisplayName("Should delete song from database")
    void testDeleteSong() {
        // Given: Song exists
        SongDto songDto = createTestSongDto(30, "To Delete", "Artist", "Album", "03:00", "2020");
        songService.createSong(songDto);

        // When: Delete song
        List<Integer> deletedIds = songService.deleteSongs("30");

        // Then: Song deleted
        assertThat(deletedIds).containsExactly(30);
        assertThat(songRepository.findById(30)).isEmpty();
    }

    @Test
    @DisplayName("Should delete multiple songs")
    void testDeleteMultipleSongs() {
        // Given: Multiple songs exist
        songService.createSong(createTestSongDto(40, "Song 1", "Artist 1", "Album 1", "03:00", "2020"));
        songService.createSong(createTestSongDto(41, "Song 2", "Artist 2", "Album 2", "04:00", "2021"));
        songService.createSong(createTestSongDto(42, "Song 3", "Artist 3", "Album 3", "05:00", "2022"));

        // When: Delete all songs
        List<Integer> deletedIds = songService.deleteSongs("40,41,42");

        // Then: All songs deleted
        assertThat(deletedIds).containsExactlyInAnyOrder(40, 41, 42);
        assertThat(songRepository.findById(40)).isEmpty();
        assertThat(songRepository.findById(41)).isEmpty();
        assertThat(songRepository.findById(42)).isEmpty();
    }

    @Test
    @DisplayName("Should handle deletion of non-existent songs gracefully")
    void testDeleteNonExistentSongs() {
        // Given: Only one song exists
        songService.createSong(createTestSongDto(50, "Existing Song", "Artist", "Album", "03:00", "2020"));

        // When: Try to delete existing and non-existent songs
        List<Integer> deletedIds = songService.deleteSongs("50,999,1000");

        // Then: Only existing song deleted
        assertThat(deletedIds).containsExactly(50);
        assertThat(songRepository.findById(50)).isEmpty();
    }

    @Test
    @DisplayName("Should handle special characters in song metadata")
    void testSpecialCharactersInMetadata() {
        // Given: Song with special characters
        SongDto songDto = createTestSongDto(
                60,
                "Señor (Tales of Yankee Power)",
                "Bob Dylan & The Band",
                "Street-Legal (Remastered)",
                "05:42",
                "1978"
        );

        // When: Create song
        Integer songId = songService.createSong(songDto);

        // Then: Special characters persisted correctly
        Song song = songRepository.findById(songId).orElseThrow();
        assertThat(song.getName()).isEqualTo("Señor (Tales of Yankee Power)");
        assertThat(song.getArtist()).isEqualTo("Bob Dylan & The Band");
        assertThat(song.getAlbum()).isEqualTo("Street-Legal (Remastered)");
    }

    @Test
    @DisplayName("Should handle long text in metadata fields")
    void testLongTextInMetadata() {
        // Given: Song with long metadata
        String longName = "A".repeat(255);
        String longArtist = "B".repeat(255);
        SongDto songDto = createTestSongDto(70, longName, longArtist, "Album", "03:00", "2020");

        // When: Create song
        Integer songId = songService.createSong(songDto);

        // Then: Long text persisted correctly
        Song song = songRepository.findById(songId).orElseThrow();
        assertThat(song.getName()).hasSize(255);
        assertThat(song.getArtist()).hasSize(255);
    }

    @Test
    @DisplayName("Should handle database transaction rollback")
    void testTransactionRollback() {
        // This test documents expected transactional behavior
        // In real scenario: if multiple operations fail, all should rollback
        
        // Given: Song exists
        songService.createSong(createTestSongDto(80, "Song 1", "Artist", "Album", "03:00", "2020"));

        // When: Try to create duplicate (should fail)
        assertThatThrownBy(() -> 
            songService.createSong(createTestSongDto(80, "Duplicate", "Artist", "Album", "03:00", "2020"))
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Then: Original song still exists
        assertThat(songRepository.findById(80)).isPresent();
    }

    @Test
    @DisplayName("Should support concurrent song creation")
    void testConcurrentSongCreation() throws InterruptedException {
        // Given: Multiple threads creating songs
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                SongDto dto = createTestSongDto(
                        100 + index,
                        "Song " + index,
                        "Artist " + index,
                        "Album " + index,
                        "03:00",
                        "2020"
                );
                songService.createSong(dto);
            });
        }

        // When: Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then: All songs created
        for (int i = 0; i < threadCount; i++) {
            assertThat(songRepository.findById(100 + i)).isPresent();
        }
    }

    @Test
    @DisplayName("Should verify JPA entity mapping")
    void testJpaEntityMapping() {
        // Given: Song DTO
        SongDto dto = createTestSongDto(90, "Test Song", "Test Artist", "Test Album", "03:30", "2022");

        // When: Save via service
        songService.createSong(dto);

        // Then: JPA entity correctly mapped
        Optional<Song> songOptional = songRepository.findById(90);
        assertThat(songOptional).isPresent();
        Song song = songOptional.get();
        assertThat(song.getId()).isEqualTo(90);
        assertThat(song.getName()).isEqualTo("Test Song");
    }

    // Helper methods

    private SongDto createTestSongDto(Integer id, String name, String artist, String album, String duration, String year) {
        SongDto dto = new SongDto();
        dto.setId(id);
        dto.setName(name);
        dto.setArtist(artist);
        dto.setAlbum(album);
        dto.setDuration(duration);
        dto.setYear(year);
        return dto;
    }
}
