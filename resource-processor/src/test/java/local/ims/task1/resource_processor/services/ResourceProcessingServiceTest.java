package local.ims.task1.resource_processor.services;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import local.ims.task1.resource_processor.clients.ResourceServiceClient;
import local.ims.task1.resource_processor.clients.SongServiceClient;
import local.ims.task1.resource_processor.dto.SongMetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResourceProcessingService.
 * Tests business logic in isolation with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceProcessingService Unit Tests")
class ResourceProcessingServiceTest {

    @Mock
    private ResourceServiceClient resourceServiceClient;

    @Mock
    private SongServiceClient songServiceClient;

    @Mock
    private Mp3Service mp3Service;

    @InjectMocks
    private ResourceProcessingService resourceProcessingService;

    private static final Integer TEST_RESOURCE_ID = 123;
    private static final byte[] TEST_MP3_DATA = "test-mp3-data".getBytes();
    private SongMetadataDto testMetadata;

    @BeforeEach
    void setUp() {
        testMetadata = createTestMetadata("Hey Jude", "The Beatles", "Hey Jude", "04:03", "1968");
    }

    @Test
    @DisplayName("processResource - Should successfully process resource")
    void testProcessResource_Success() {
        // Given: All services respond successfully
        when(resourceServiceClient.getResourceById(TEST_RESOURCE_ID)).thenReturn(TEST_MP3_DATA);
        when(mp3Service.extractMetadata(TEST_MP3_DATA)).thenReturn(testMetadata);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", TEST_RESOURCE_ID);
        when(songServiceClient.saveSongMetadata(any(SongMetadataDto.class))).thenReturn(response);

        // When: Process resource
        resourceProcessingService.processResource(TEST_RESOURCE_ID);

        // Then: All steps executed
        verify(resourceServiceClient, times(1)).getResourceById(TEST_RESOURCE_ID);
        verify(mp3Service, times(1)).extractMetadata(TEST_MP3_DATA);
        verify(songServiceClient, times(1)).saveSongMetadata(argThat(dto -> 
            dto.getId().equals(TEST_RESOURCE_ID) &&
            dto.getName().equals("Hey Jude") &&
            dto.getArtist().equals("The Beatles")
        ));
    }

    @Test
    @DisplayName("processResource - Should set resource ID in metadata")
    void testProcessResource_SetsResourceId() {
        // Given: Metadata without ID
        SongMetadataDto metadataWithoutId = createTestMetadata("Test Song", "Test Artist", "Test Album", "03:00", "2023");
        when(resourceServiceClient.getResourceById(TEST_RESOURCE_ID)).thenReturn(TEST_MP3_DATA);
        when(mp3Service.extractMetadata(TEST_MP3_DATA)).thenReturn(metadataWithoutId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", TEST_RESOURCE_ID);
        when(songServiceClient.saveSongMetadata(any(SongMetadataDto.class))).thenReturn(response);

        // When: Process resource
        resourceProcessingService.processResource(TEST_RESOURCE_ID);

        // Then: Metadata saved with correct resource ID
        verify(songServiceClient, times(1)).saveSongMetadata(argThat(dto -> 
            dto.getId().equals(TEST_RESOURCE_ID)
        ));
    }

    @Test
    @DisplayName("processResource - Should handle resource service failure and retry")
    void testProcessResource_ResourceServiceFailure_Retries() {
        // Given: Resource Service fails
        when(resourceServiceClient.getResourceById(TEST_RESOURCE_ID))
                .thenThrow(createFeignException(503, "Service Unavailable"));
    
        // When/Then: Exception propagated (retry happens in real scenario with Spring context)
        assertThatThrownBy(() -> resourceProcessingService.processResource(TEST_RESOURCE_ID))
                .isInstanceOf(FeignException.class);
    
        // Then: Service was called
        verify(resourceServiceClient, atLeastOnce()).getResourceById(TEST_RESOURCE_ID);
        // Mp3Service and SongService should not be called when resource fetch fails
        verifyNoInteractions(mp3Service, songServiceClient);
    }

    @Test
    @DisplayName("processResource - Should throw exception when metadata extraction fails")
    void testProcessResource_MetadataExtractionFailure() {
        // Given: Metadata extraction fails
        when(resourceServiceClient.getResourceById(TEST_RESOURCE_ID)).thenReturn(TEST_MP3_DATA);
        when(mp3Service.extractMetadata(TEST_MP3_DATA))
                .thenThrow(new RuntimeException("Failed to parse MP3 file"));

        // When/Then: Exception propagated
        assertThatThrownBy(() -> resourceProcessingService.processResource(TEST_RESOURCE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse MP3 file");

        verify(resourceServiceClient, times(1)).getResourceById(TEST_RESOURCE_ID);
        verify(mp3Service, times(1)).extractMetadata(TEST_MP3_DATA);
        verifyNoInteractions(songServiceClient);
    }

    @Test
    @DisplayName("processResource - Should throw exception when song service fails")
    void testProcessResource_SongServiceFailure() {
        // Given: Song service fails
        when(resourceServiceClient.getResourceById(TEST_RESOURCE_ID)).thenReturn(TEST_MP3_DATA);
        when(mp3Service.extractMetadata(TEST_MP3_DATA)).thenReturn(testMetadata);
        when(songServiceClient.saveSongMetadata(any(SongMetadataDto.class)))
                .thenThrow(createFeignException(500, "Internal Server Error"));

        // When/Then: Exception propagated
        assertThatThrownBy(() -> resourceProcessingService.processResource(TEST_RESOURCE_ID))
                .isInstanceOf(FeignException.class);

        verify(resourceServiceClient, times(1)).getResourceById(TEST_RESOURCE_ID);
        verify(mp3Service, times(1)).extractMetadata(TEST_MP3_DATA);
        verify(songServiceClient, times(1)).saveSongMetadata(any(SongMetadataDto.class));
    }

    @Test
    @DisplayName("processResource - Should handle null resource ID gracefully")
    void testProcessResource_NullResourceId() {
        // When/Then: Null resource ID causes NullPointerException in real code
        // This test documents current behavior
        assertThatThrownBy(() -> resourceProcessingService.processResource(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("processResource - Should handle empty MP3 data")
    void testProcessResource_EmptyMp3Data() {
        // Given: Resource service returns empty data
        byte[] emptyData = new byte[0];
        when(resourceServiceClient.getResourceById(TEST_RESOURCE_ID)).thenReturn(emptyData);
        when(mp3Service.extractMetadata(emptyData))
                .thenThrow(new RuntimeException("MP3 data is empty"));

        // When/Then: Exception thrown
        assertThatThrownBy(() -> resourceProcessingService.processResource(TEST_RESOURCE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MP3 data is empty");

        verify(resourceServiceClient, times(1)).getResourceById(TEST_RESOURCE_ID);
        verify(mp3Service, times(1)).extractMetadata(emptyData);
        verifyNoInteractions(songServiceClient);
    }

    @Test
    @DisplayName("processResourceRecover - Should log error and rethrow exception")
    void testProcessResourceRecover_LogsAndRethrows() {
        // Given: Original exception
        Exception originalException = new RuntimeException("Service unavailable");

        // When/Then: Recover method rethrows
        assertThatThrownBy(() -> 
            resourceProcessingService.processResourceRecover(originalException, TEST_RESOURCE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Processing failed for resourceId=123")
                .hasCause(originalException);
    }

    @Test
    @DisplayName("processResource - Should handle metadata with minimal fields")
    void testProcessResource_MinimalMetadata() {
        // Given: Metadata with only name
        SongMetadataDto minimalMetadata = new SongMetadataDto();
        minimalMetadata.setName("Unknown Song");
        
        when(resourceServiceClient.getResourceById(TEST_RESOURCE_ID)).thenReturn(TEST_MP3_DATA);
        when(mp3Service.extractMetadata(TEST_MP3_DATA)).thenReturn(minimalMetadata);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", TEST_RESOURCE_ID);
        when(songServiceClient.saveSongMetadata(any(SongMetadataDto.class))).thenReturn(response);

        // When: Process resource
        resourceProcessingService.processResource(TEST_RESOURCE_ID);

        // Then: Metadata saved even with minimal fields
        verify(songServiceClient, times(1)).saveSongMetadata(argThat(dto -> 
            dto.getId().equals(TEST_RESOURCE_ID) &&
            dto.getName().equals("Unknown Song")
        ));
    }

    @Test
    @DisplayName("processResource - Should handle special characters in metadata")
    void testProcessResource_SpecialCharactersInMetadata() {
        // Given: Metadata with special characters
        SongMetadataDto specialMetadata = createTestMetadata(
            "Señor (Tales of Yankee Power)",
            "Bob Dylan & The Band",
            "Street-Legal (Remastered)",
            "05:42",
            "1978"
        );
        
        when(resourceServiceClient.getResourceById(TEST_RESOURCE_ID)).thenReturn(TEST_MP3_DATA);
        when(mp3Service.extractMetadata(TEST_MP3_DATA)).thenReturn(specialMetadata);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", TEST_RESOURCE_ID);
        when(songServiceClient.saveSongMetadata(any(SongMetadataDto.class))).thenReturn(response);

        // When: Process resource
        resourceProcessingService.processResource(TEST_RESOURCE_ID);

        // Then: Special characters handled correctly
        verify(songServiceClient, times(1)).saveSongMetadata(argThat(dto -> 
            dto.getName().equals("Señor (Tales of Yankee Power)") &&
            dto.getArtist().equals("Bob Dylan & The Band")
        ));
    }

    // Helper methods

    private SongMetadataDto createTestMetadata(String name, String artist, String album, String duration, String year) {
        SongMetadataDto metadata = new SongMetadataDto();
        metadata.setName(name);
        metadata.setArtist(artist);
        metadata.setAlbum(album);
        metadata.setDuration(duration);
        metadata.setYear(year);
        return metadata;
    }

    private FeignException createFeignException(int status, String message) {
        Request request = Request.create(Request.HttpMethod.GET, "/test", new HashMap<>(), null, new RequestTemplate());
        return FeignException.errorStatus(message, 
            feign.Response.builder()
                .status(status)
                .reason(message)
                .request(request)
                .headers(new HashMap<>())
                .build());
    }
}
