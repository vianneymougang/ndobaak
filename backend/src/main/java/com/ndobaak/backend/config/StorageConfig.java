package com.ndobaak.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

import java.net.URI;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${app.storage.endpoint:}")
    private String endpoint;

    @Value("${app.storage.access-key}")
    private String accessKey;

    @Value("${app.storage.secret-key}")
    private String secretKey;

    @Value("${app.storage.region}")
    private String region;

    @Value("${app.storage.bucket}")
    private String bucket;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }

        S3Client client = builder.build();

        initBucket(client);

        return client;
    }

    private void initBucket(S3Client client) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Storage bucket '{}' exists", bucket);
        } catch (NoSuchBucketException e) {
            log.info("Creating storage bucket '{}'", bucket);
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            applyPublicReadPolicy(client);
        } catch (Exception e) {
            log.warn("Could not verify bucket '{}': {}", bucket, e.getMessage());
        }
    }

    private void applyPublicReadPolicy(S3Client client) {
        String policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Sid": "PublicReadGetObject",
                            "Effect": "Allow",
                            "Principal": "*",
                            "Action": "s3:GetObject",
                            "Resource": "arn:aws:s3:::%s/*"
                        }
                    ]
                }
                """.formatted(bucket);

        try {
            client.putBucketPolicy(PutBucketPolicyRequest.builder()
                    .bucket(bucket)
                    .policy(policy)
                    .build());
            log.info("Public read policy applied to bucket '{}'", bucket);
        } catch (Exception e) {
            log.warn("Could not apply public read policy to '{}': {}", bucket, e.getMessage());
        }
    }
}
