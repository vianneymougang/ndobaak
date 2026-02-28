package com.ndobaak.backend.media.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Service
public class StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String endpoint;

    public StorageService(S3Client s3Client,
                          @Value("${app.storage.bucket}") String bucket,
                          @Value("${app.storage.endpoint}") String endpoint) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.endpoint = endpoint;
    }

    public String upload(String folder, String originalFilename, InputStream inputStream,
                         long contentLength, String contentType) {
        String extension = getExtension(originalFilename);
        String key = folder + "/" + UUID.randomUUID() + extension;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

        return endpoint + "/" + bucket + "/" + key;
    }

    public String uploadNamed(String folder, String filename, InputStream inputStream,
                              long contentLength, String contentType) {
        String key = folder + "/" + filename;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

        return endpoint + "/" + bucket + "/" + key;
    }

    public void delete(String fileUrl) {
        String key = extractKey(fileUrl);
        if (key != null) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        }
    }

    private String extractKey(String fileUrl) {
        String prefix = endpoint + "/" + bucket + "/";
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        return null;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
