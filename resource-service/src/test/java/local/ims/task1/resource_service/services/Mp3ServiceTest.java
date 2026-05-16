package local.ims.task1.resource_service.services;

import local.ims.task1.resource_service.entities.Mp3Resource;
import local.ims.task1.resource_service.exceptions.InputDataBaseRequestException;
import local.ims.task1.resource_service.exceptions.ResourceNotFoundException;
import local.ims.task1.resource_service.repositories.Mp3ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Mp3Service.
 * Tests business logic in isolation with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Mp3Service Unit Tests")
class Mp3ServiceTest {

    @Mock
    private Mp3ResourceRepository repository;

    @Mock
    private MetadataService metadataService;

    @Mock
    private S3Service s3Service;

    @Mock
    private ResourceMessageSender resourceMessageSender;

    @InjectMocks
    private Mp3Service mp3Service;

    private static final String TEST_BUCKET = "test-bucket";
    private static final byte[] VALID_MP3_DATA = "fake-mp3-data".getBytes();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mp3Service, "bucketName", TEST_BUCKET);
    }

    @Test
    @DisplayName("saveMp3 - Should successfully save MP3 and return resource ID")
    void testSaveMp3_Success() throws Exception {
        // Given: Valid MP3 data
        Mp3Resource savedResource = createMockResource(1, "s3://test-bucket/test-file.mp3");
        when(repository.save(any(Mp3Resource.class))).thenReturn(savedResource);
        doNothing().when(s3Service).uploadBytes(anyString(), anyString(), any(byte[].class));
        doNothing().when(resourceMessageSender).sendResourceUploaded(anyInt());

        // When: Save MP3
        Integer resourceId = mp3Service.saveMp3(VALID_MP3_DATA);

        // Then: Resource ID returned, S3 uploaded, message sent
        assertThat(resourceId).isEqualTo(1);
        verify(s3Service, times(1)).uploadBytes(eq(TEST_BUCKET), anyString(), eq(VALID_MP3_DATA));
        verify(repository, times(1)).save(any(Mp3Resource.class));
        verify(resourceMessageSender, times(1)).sendResourceUploaded(1);
    }

    @Test
    @DisplayName("saveMp3 - Should throw exception when MP3 data is null")
    void testSaveMp3_NullData_ThrowsException() throws Exception {
        // When/Then: Null data throws InputDataBaseRequestException
        InputDataBaseRequestException ex = catchThrowableOfType(
                () -> mp3Service.saveMp3(null),
                InputDataBaseRequestException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("MP3 data must not be empty");
    
        verifyNoInteractions(s3Service, repository, resourceMessageSender);
    }
    
    @Test
    @DisplayName("saveMp3 - Should throw exception when MP3 data is empty")
    void testSaveMp3_EmptyData_ThrowsException() throws Exception {
        // When/Then: Empty data throws InputDataBaseRequestException
        InputDataBaseRequestException ex = catchThrowableOfType(
                () -> mp3Service.saveMp3(new byte[0]),
                InputDataBaseRequestException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("MP3 data must not be empty");
    
        verifyNoInteractions(s3Service, repository, resourceMessageSender);
    }

    @Test
    @DisplayName("getMp3ById - Should successfully retrieve MP3 by ID")
    void testGetMp3ById_Success() {
        // Given: Resource exists in repository
        int resourceId = 1;
        String location = "s3://test-bucket/file.mp3";
        Mp3Resource resource = createMockResource(resourceId, location);
        when(repository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(s3Service.downloadFileAsBytes(TEST_BUCKET, location)).thenReturn(VALID_MP3_DATA);
    
        // When: Get MP3 by ID
        byte[] result = mp3Service.getMp3ById(resourceId);
    
        // Then: Correct data returned
        assertThat(result).isEqualTo(VALID_MP3_DATA);
        verify(repository, times(1)).findById(resourceId);
        verify(s3Service, times(1)).downloadFileAsBytes(TEST_BUCKET, location);
    }

    @Test
    @DisplayName("getMp3ById - Should throw exception for invalid ID (negative)")
    void testGetMp3ById_InvalidId_ThrowsException() {
        // When/Then: Negative ID throws InputDataBaseRequestException
        InputDataBaseRequestException ex = catchThrowableOfType(
                () -> mp3Service.getMp3ById(-1),
                InputDataBaseRequestException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("ID must be a positive integer");
    
        verifyNoInteractions(repository, s3Service);
    }
    
    @Test
    @DisplayName("getMp3ById - Should throw exception for invalid ID (zero)")
    void testGetMp3ById_ZeroId_ThrowsException() {
        // When/Then: Zero ID throws InputDataBaseRequestException
        InputDataBaseRequestException ex = catchThrowableOfType(
                () -> mp3Service.getMp3ById(0),
                InputDataBaseRequestException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("ID must be a positive integer");
    
        verifyNoInteractions(repository, s3Service);
    }

    @Test
    @DisplayName("getMp3ById - Should throw exception when resource not found")
    void testGetMp3ById_NotFound_ThrowsException() {
        // Given: Resource does not exist
        int resourceId = 999;
        when(repository.findById(resourceId)).thenReturn(Optional.empty());
    
        // When/Then: ResourceNotFoundException thrown
        assertThatThrownBy(() -> mp3Service.getMp3ById(resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resource with ID=999 not found");
    
        verify(repository, times(1)).findById(resourceId);
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("deleteResourcesWithCascading - Should successfully delete multiple resources")
    void testDeleteResourcesWithCascading_Success() {
        // Given: Resources exist
        String csvIds = "1,2,3";
        List<Mp3Resource> resources = List.of(
                createMockResource(1, "s3://test-bucket/file1.mp3"),
                createMockResource(2, "s3://test-bucket/file2.mp3"),
                createMockResource(3, "s3://test-bucket/file3.mp3")
        );
        when(repository.findAllById(List.of(1, 2, 3))).thenReturn(resources);
        doNothing().when(s3Service).deleteFile(anyString(), anyString());
        when(metadataService.deleteSongMetadataByIds(anyList())).thenReturn(List.of(1, 2, 3));
        doNothing().when(repository).deleteById(anyInt());
    
        // When: Delete cascade
        List<Integer> deletedIds = mp3Service.deleteResourcesWithCascading(csvIds);
    
        // Then: All resources deleted
        assertThat(deletedIds).containsExactlyInAnyOrder(1, 2, 3);
        verify(repository, times(1)).findAllById(List.of(1, 2, 3));
        verify(metadataService, times(1)).deleteSongMetadataByIds(List.of(1, 2, 3));
        // S3 delete is called 6 times: 3 in deleteResourcesWithCascading + 3 in deleteMp3DataOneByOne
        verify(s3Service, times(6)).deleteFile(anyString(), anyString());
        verify(repository, times(3)).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteResourcesWithCascading - Should return empty list for null CSV")
    void testDeleteResourcesWithCascading_NullCsv_ReturnsEmpty() {
        // When: Null CSV provided
        List<Integer> deletedIds = mp3Service.deleteResourcesWithCascading(null);
    
        // Then: Empty list returned
        assertThat(deletedIds).isEmpty();
        verifyNoInteractions(repository, s3Service, metadataService);
    }

    @Test
    @DisplayName("deleteResourcesWithCascading - Should return empty list for blank CSV")
    void testDeleteResourcesWithCascading_BlankCsv_ReturnsEmpty() {
        // When: Blank CSV provided
        List<Integer> deletedIds = mp3Service.deleteResourcesWithCascading("   ");
    
        // Then: Empty list returned
        assertThat(deletedIds).isEmpty();
        verifyNoInteractions(repository, s3Service, metadataService);
    }

    @Test
    @DisplayName("deleteResourcesWithCascading - Should throw exception for invalid CSV format")
    void testDeleteResourcesWithCascading_InvalidCsv_ThrowsException() {
        // When/Then: Invalid CSV format throws exception
        String invalidCsv = "1,abc,3";
        InputDataBaseRequestException ex = catchThrowableOfType(
                () -> mp3Service.deleteResourcesWithCascading(invalidCsv),
                InputDataBaseRequestException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("Invalid ID format: 'abc'");
    
        verifyNoInteractions(repository, s3Service, metadataService);
    }
    
    @Test
    @DisplayName("deleteResourcesWithCascading - Should throw exception when CSV exceeds max length")
    void testDeleteResourcesWithCascading_CsvTooLong_ThrowsException() {
        // Given: CSV string exceeding MAX_CSV_LENGTH (200 characters)
        String longCsv = "1,".repeat(101).substring(0, 201);
    
        // When/Then: Exception thrown
        InputDataBaseRequestException ex = catchThrowableOfType(
                () -> mp3Service.deleteResourcesWithCascading(longCsv),
                InputDataBaseRequestException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getDetails()).contains("CSV string is too long");
        assertThat(ex.getDetails()).contains("201 characters");
        assertThat(ex.getDetails()).contains("maximum allowed is 200");
    
        verifyNoInteractions(repository, s3Service, metadataService);
    }

    @Test
    @DisplayName("deleteResourcesWithCascading - Should handle partial deletion gracefully")
    void testDeleteResourcesWithCascading_PartialSuccess() {
        // Given: 3 resources, but one deletion fails
        String csvIds = "1,2,3";
        List<Mp3Resource> resources = List.of(
                createMockResource(1, "s3://test-bucket/file1.mp3"),
                createMockResource(2, "s3://test-bucket/file2.mp3"),
                createMockResource(3, "s3://test-bucket/file3.mp3")
        );
        when(repository.findAllById(List.of(1, 2, 3))).thenReturn(resources);
        doNothing().when(s3Service).deleteFile(anyString(), anyString());
        when(metadataService.deleteSongMetadataByIds(anyList())).thenReturn(List.of(1, 2, 3));
    
        doNothing().when(repository).deleteById(1);
        doThrow(new RuntimeException("DB error")).when(repository).deleteById(2);
        doNothing().when(repository).deleteById(3);
    
        // When: Delete cascade
        List<Integer> deletedIds = mp3Service.deleteResourcesWithCascading(csvIds);
    
        // Then: Only successful deletions returned
        assertThat(deletedIds).containsExactlyInAnyOrder(1, 3);
        verify(repository, times(1)).findAllById(List.of(1, 2, 3));
        verify(metadataService, times(1)).deleteSongMetadataByIds(List.of(1, 2, 3));
        verify(repository, times(3)).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteMp3DataOneByOne - Should delete all resources successfully")
    void testDeleteMp3DataOneByOne_AllSuccess() {
        // Given: List of resources
        List<Mp3Resource> resources = List.of(
                createMockResource(1, "s3://test-bucket/file1.mp3"),
                createMockResource(2, "s3://test-bucket/file2.mp3")
        );
        doNothing().when(s3Service).deleteFile(anyString(), anyString());
        doNothing().when(repository).deleteById(anyInt());
    
        // When: Delete one by one
        List<Integer> deletedIds = mp3Service.deleteMp3DataOneByOne(resources);
    
        // Then: All deleted
        assertThat(deletedIds).containsExactlyInAnyOrder(1, 2);
        verify(s3Service, times(2)).deleteFile(anyString(), anyString());
        verify(repository, times(2)).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteMp3DataOneByOne - Should skip failed deletions and continue")
    void testDeleteMp3DataOneByOne_PartialFailure() {
        // Given: List of resources, one deletion fails
        List<Mp3Resource> resources = List.of(
                createMockResource(1, "s3://test-bucket/file1.mp3"),
                createMockResource(2, "s3://test-bucket/file2.mp3"),
                createMockResource(3, "s3://test-bucket/file3.mp3")
        );
        doNothing().when(s3Service).deleteFile(anyString(), anyString());
        doNothing().when(repository).deleteById(1);
        doThrow(new RuntimeException("Deletion failed")).when(repository).deleteById(2);
        doNothing().when(repository).deleteById(3);
    
        // When: Delete one by one
        List<Integer> deletedIds = mp3Service.deleteMp3DataOneByOne(resources);
    
        // Then: Only successful deletions returned
        assertThat(deletedIds).containsExactlyInAnyOrder(1, 3);
        verify(repository, times(3)).deleteById(anyInt());
    }

    // Helper methods
    private Mp3Resource createMockResource(Integer id, String location) {
        Mp3Resource resource = new Mp3Resource();
        resource.setId(id);
        resource.setLocation(location);
        return resource;
    }
}
