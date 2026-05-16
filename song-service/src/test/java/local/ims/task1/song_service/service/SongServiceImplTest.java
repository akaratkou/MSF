package local.ims.task1.song_service.service;

import local.ims.task1.song_service.dto.SongDto;
import local.ims.task1.song_service.exception.InputDataValidationException;
import local.ims.task1.song_service.exception.ResourceNotFoundException;
import local.ims.task1.song_service.model.Song;
import local.ims.task1.song_service.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SongServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SongService Unit Tests")
class SongServiceImplTest {

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongServiceImpl songService;

    private SongDto testSongDto;

    @BeforeEach
    void setUp() {
        testSongDto = createTestSongDto(1, "Hey Jude", "The Beatles", "Hey Jude", "04:03", "1968");
    }

    @Test
    @DisplayName("createSong - Should successfully create song metadata")
    void testCreateSong_Success() {
        // Given: Song does not exist
        when(songRepository.existsById(testSongDto.getId())).thenReturn(false);
        when(songRepository.save(any(Song.class))).thenAnswer(invocation -> invocation.getArgument(0));
    
        // When: Create song
        Integer songId = songService.createSong(testSongDto);
    
        // Then: Song created with correct ID
        assertThat(songId).isEqualTo(1);
        verify(songRepository, times(1)).existsById(1);
        verify(songRepository, times(1)).save(any(Song.class));
    }

    @Test
    @DisplayName("createSong - Should throw exception when song with ID already exists")
    void testCreateSong_DuplicateId_ThrowsException() {
        // Given: Song with ID already exists
        when(songRepository.existsById(testSongDto.getId())).thenReturn(true);
    
        // When/Then: DataIntegrityViolationException thrown
        assertThatThrownBy(() -> songService.createSong(testSongDto))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Metadata for resource ID=1 already exists");
    
        verify(songRepository, times(1)).existsById(1);
        verify(songRepository, never()).save(any(Song.class));
    }

    @Test
    @DisplayName("createSong - Should handle song with minimal metadata")
    void testCreateSong_MinimalMetadata() {
        // Given: Song with only required fields
        SongDto minimalDto = new SongDto();
        minimalDto.setId(10);
        minimalDto.setName("Minimal Song");
        when(songRepository.existsById(10)).thenReturn(false);
        when(songRepository.save(any(Song.class))).thenAnswer(invocation -> invocation.getArgument(0));
    
        // When: Create song
        Integer songId = songService.createSong(minimalDto);
    
        // Then: Song created successfully
        assertThat(songId).isEqualTo(10);
        verify(songRepository, times(1)).save(any(Song.class));
    }

    @Test
    @DisplayName("getSongById - Should successfully retrieve song by ID")
    void testGetSongById_Success() {
        // Given: Song exists in repository
        Song song = createTestSong(1, "Hey Jude", "The Beatles", "Hey Jude", "04:03", "1968");
        when(songRepository.findById(1)).thenReturn(Optional.of(song));
    
        // When: Get song by ID
        SongDto result = songService.getSongById(1);
    
        // Then: Correct song returned
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("Hey Jude");
        assertThat(result.getArtist()).isEqualTo("The Beatles");
        verify(songRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("getSongById - Should throw exception for invalid ID (negative)")
    void testGetSongById_InvalidId_ThrowsException() {
        // When/Then: Negative ID throws InputDataValidationException
        InputDataValidationException ex = catchThrowableOfType(
                () -> songService.getSongById(-1),
                InputDataValidationException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("ID must be a positive integer");
    
        verifyNoInteractions(songRepository);
    }
    
    @Test
    @DisplayName("getSongById - Should throw exception for invalid ID (zero)")
    void testGetSongById_ZeroId_ThrowsException() {
        // When/Then: Zero ID throws InputDataValidationException
        InputDataValidationException ex = catchThrowableOfType(
                () -> songService.getSongById(0),
                InputDataValidationException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("ID must be a positive integer");
    
        verifyNoInteractions(songRepository);
    }

    @Test
    @DisplayName("getSongById - Should throw exception when song not found")
    void testGetSongById_NotFound_ThrowsException() {
        // Given: Song does not exist
        when(songRepository.findById(999)).thenReturn(Optional.empty());
    
        // When/Then: ResourceNotFoundException thrown
        assertThatThrownBy(() -> songService.getSongById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Song metadata for ID=999 not found");
    
        verify(songRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("deleteSongs - Should successfully delete multiple songs")
    void testDeleteSongs_Success() {
        // Given: Songs exist
        String csvIds = "1,2,3";
        when(songRepository.existsById(1)).thenReturn(true);
        when(songRepository.existsById(2)).thenReturn(true);
        when(songRepository.existsById(3)).thenReturn(true);
        doNothing().when(songRepository).deleteById(anyInt());
    
        // When: Delete songs
        List<Integer> deletedIds = songService.deleteSongs(csvIds);
    
        // Then: All songs deleted
        assertThat(deletedIds).containsExactlyInAnyOrder(1, 2, 3);
        verify(songRepository, times(3)).existsById(anyInt());
        verify(songRepository, times(3)).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteSongs - Should handle non-existent song IDs gracefully")
    void testDeleteSongs_NonExistentIds() {
        // Given: Some songs don't exist
        String csvIds = "1,2,3";
        when(songRepository.existsById(1)).thenReturn(true);
        when(songRepository.existsById(2)).thenReturn(false);
        when(songRepository.existsById(3)).thenReturn(true);
        doNothing().when(songRepository).deleteById(anyInt());
    
        // When: Delete songs
        List<Integer> deletedIds = songService.deleteSongs(csvIds);
    
        // Then: Only existing songs deleted
        assertThat(deletedIds).containsExactlyInAnyOrder(1, 3);
        verify(songRepository, times(3)).existsById(anyInt());
        verify(songRepository, times(2)).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteSongs - Should throw exception for invalid CSV format")
    void testDeleteSongs_InvalidCsv_ThrowsException() {
        // When/Then: Invalid CSV format throws exception
        String invalidCsv = "1,abc,3";
        InputDataValidationException ex = catchThrowableOfType(
                () -> songService.deleteSongs(invalidCsv),
                InputDataValidationException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("Invalid ID format: 'abc'");
    
        verifyNoInteractions(songRepository);
    }
    
    @Test
    @DisplayName("deleteSongs - Should throw exception when CSV exceeds max length")
    void testDeleteSongs_CsvTooLong_ThrowsException() {
        // Given: CSV string exceeding MAX_CSV_LENGTH (200 characters)
        String longCsv = "1,".repeat(101).substring(0, 201);
    
        // When/Then: Exception thrown
        InputDataValidationException ex = catchThrowableOfType(
                () -> songService.deleteSongs(longCsv),
                InputDataValidationException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("CSV string is too long");
        assertThat(ex.getDetails()).contains("201 characters");
    
        verifyNoInteractions(songRepository);
    }

    @Test
    @DisplayName("deleteSongs - Should handle deletion failures gracefully")
    void testDeleteSongs_PartialFailure() {
        // Given: Deletion fails for one song
        String csvIds = "1,2,3";
        when(songRepository.existsById(1)).thenReturn(true);
        when(songRepository.existsById(2)).thenReturn(true);
        when(songRepository.existsById(3)).thenReturn(true);
        doNothing().when(songRepository).deleteById(1);
        doThrow(new RuntimeException("Database error")).when(songRepository).deleteById(2);
        doNothing().when(songRepository).deleteById(3);
    
        // When: Delete songs
        List<Integer> deletedIds = songService.deleteSongs(csvIds);
    
        // Then: Only successful deletions returned
        assertThat(deletedIds).containsExactlyInAnyOrder(1, 3);
        verify(songRepository, times(3)).existsById(anyInt());
        verify(songRepository, times(3)).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteSongs - Should handle single ID")
    void testDeleteSongs_SingleId() {
        // Given: Single ID in CSV
        String csvIds = "1";
        when(songRepository.existsById(1)).thenReturn(true);
        doNothing().when(songRepository).deleteById(1);
    
        // When: Delete song
        List<Integer> deletedIds = songService.deleteSongs(csvIds);
    
        // Then: Single song deleted
        assertThat(deletedIds).containsExactly(1);
        verify(songRepository, times(1)).existsById(1);
        verify(songRepository, times(1)).deleteById(1);
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

    private Song createTestSong(Integer id, String name, String artist, String album, String duration, String year) {
        Song song = new Song();
        song.setId(id);
        song.setName(name);
        song.setArtist(artist);
        song.setAlbum(album);
        song.setDuration(duration);
        song.setYear(year);
        return song;
    }
}
