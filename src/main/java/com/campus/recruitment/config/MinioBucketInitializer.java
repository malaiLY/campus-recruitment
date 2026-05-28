package com.campus.recruitment.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class MinioBucketInitializer implements CommandLineRunner {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket-resume}")
    private String bucketResume;

    @Value("${app.minio.bucket-avatar}")
    private String bucketAvatar;

    @Value("${app.minio.bucket-license}")
    private String bucketLicense;

    @Value("${app.minio.bucket-job-attachment}")
    private String bucketJobAttachment;

    public MinioBucketInitializer(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public void run(String... args) throws Exception {
        List<String> buckets = List.of(bucketResume, bucketAvatar, bucketLicense, bucketJobAttachment);
        for (String bucket : buckets) {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            } else {
                log.info("MinIO bucket already exists: {}", bucket);
            }
        }
        log.info("MinIO bucket initialization completed");
    }
}
