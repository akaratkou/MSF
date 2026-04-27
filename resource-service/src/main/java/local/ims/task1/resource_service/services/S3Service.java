package local.ims.task1.resource_service.services;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class S3Service {
    private final S3Template s3Template;

    public S3Service(S3Template s3Template) {
        this.s3Template = s3Template;
    }

    public void uploadBytes(String bucketName, String key, byte[] data) throws IOException {
        ObjectMetadata metadata = ObjectMetadata.builder()
                .contentType("audio/mpeg")
                .contentLength((long) data.length)
                .build();

        try (InputStream is = new ByteArrayInputStream(data)) {
            s3Template.upload(bucketName, key, is, metadata);
        }
    }

    public byte[] downloadFileAsBytes(String bucketName, String key) {
        S3Resource resource = s3Template.download(bucketName, key);

        try (InputStream is = resource.getInputStream()) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Cannot dowload files from S3", e);
        }
    }

    public void deleteFile(String bucketName, String key) {
        s3Template.deleteObject(bucketName, key);
    }

}
