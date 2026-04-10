package com.ndobaak.backend.media.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Service
public class StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicUrl;

    public StorageService(S3Client s3Client,
                          @Value("${app.storage.bucket}") String bucket,
                          @Value("${app.storage.public-url}") String publicUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicUrl = publicUrl;
    }

    public String upload(String folder, String originalFilename, InputStream inputStream,
                         long contentLength, String contentType) {
        String extension = getExtension(originalFilename);
        String key = folder + "/" + UUID.randomUUID() + extension;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

        return key;
    }

    public String uploadNamed(String folder, String filename, InputStream inputStream,
                              long contentLength, String contentType) {
        String key = folder + "/" + filename;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

        return key;
    }

    public byte[] download(String key) {
        String resolved = stripToKey(key);
        return s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(resolved).build(),
                ResponseTransformer.toBytes()
        ).asByteArray();
    }

    public void delete(String key) {
        if (key == null) return;
        String resolved = stripToKey(key);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(resolved)
                .build());
    }

    public String resolvePublicUrl(String key) {
        if (key == null) return null;
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }
        return publicUrl + "/" + key;
    }

    private String stripToKey(String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            int bucketSlash = value.indexOf('/', value.indexOf("//") + 2);
            if (bucketSlash >= 0) {
                int keyStart = value.indexOf('/', bucketSlash + 1);
                if (keyStart >= 0) {
                    return value.substring(keyStart + 1);
                }
            }
        }
        return value;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
