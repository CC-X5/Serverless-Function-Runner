package hskl.cn.serverless.registry.service;

import hskl.cn.serverless.registry.config.MinioConfig;
import hskl.cn.serverless.registry.exception.StorageException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .build()
                );
                log.info("Created bucket: {}", minioConfig.getBucket());
            } else {
                log.info("Bucket already exists: {}", minioConfig.getBucket());
            }
        } catch (Exception e) {
            log.warn("Could not initialize bucket (MinIO might not be available): {}", e.getMessage());
        }
    }

    public String uploadJar(String functionName, MultipartFile file) {
        String objectName = functionName + "/" + file.getOriginalFilename();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/java-archive")
                            .build()
            );
            log.info("Uploaded JAR for function '{}': {}", functionName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload JAR for function '{}': {}", functionName, e.getMessage());
            throw new StorageException("Failed to upload JAR file", e);
        }
    }

    public InputStream downloadJar(String objectPath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectPath)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new StorageException("JAR file not found: " + objectPath);
            }
            throw new StorageException("Failed to download JAR file", e);
        } catch (Exception e) {
            log.error("Failed to download JAR: {}", e.getMessage());
            throw new StorageException("Failed to download JAR file", e);
        }
    }

    public void deleteJar(String objectPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectPath)
                            .build()
            );
            log.info("Deleted JAR: {}", objectPath);
        } catch (Exception e) {
            log.error("Failed to delete JAR: {}", e.getMessage());
            throw new StorageException("Failed to delete JAR file", e);
        }
    }
}
