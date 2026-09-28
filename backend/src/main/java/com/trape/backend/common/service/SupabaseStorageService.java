package com.trape.backend.common.service;

import com.trape.backend.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucketName;
    private final HttpClient httpClient;

    public record MediaUploadResponse(
            String url,
            String fileName,
            long size,
            String contentType,
            String provider
    ) {}

    public SupabaseStorageService(@Value("${supabase.url}") String supabaseUrl,
                                  @Value("${supabase.key:}") String supabaseKey,
                                  @Value("${supabase.storage.bucket:products}") String bucketName) {
        this.supabaseUrl = supabaseUrl.replaceAll("/$", "");
        this.supabaseKey = supabaseKey;
        this.bucketName = bucketName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public MediaUploadResponse uploadImage(MultipartFile file) {
        validateFile(file);

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
        String cleanName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueFileName = UUID.randomUUID() + "-" + cleanName;

       
        if (supabaseKey != null && !supabaseKey.isBlank()) {
            try {
                String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, uniqueFileName);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uploadUrl))
                        .header("Authorization", "Bearer " + supabaseKey)
                        .header("apiKey", supabaseKey)
                        .header("Content-Type", file.getContentType())
                        .header("x-upsert", "true")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String publicUrl = String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, uniqueFileName);
                    log.info("Uploaded image to Supabase Storage: {}", publicUrl);
                    return new MediaUploadResponse(publicUrl, uniqueFileName, file.getSize(), file.getContentType(), "SUPABASE");
                } else {
                    log.warn("Supabase Storage upload returned HTTP {} ({}). Falling back to local storage.", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.error("Failed to upload to Supabase Storage, attempting local fallback: {}", e.getMessage());
            }
        }

        // Local storage fallback
        return uploadLocally(file, uniqueFileName);
    }

    private MediaUploadResponse uploadLocally(MultipartFile file, String uniqueFileName) {
        try {
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path targetPath = uploadDir.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String localUrl = "/uploads/" + uniqueFileName;
            log.info("Saved image to local storage at {}", localUrl);
            return new MediaUploadResponse(localUrl, uniqueFileName, file.getSize(), file.getContentType(), "LOCAL");
        } catch (IOException e) {
            log.error("Failed to store image locally", e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", "Could not save uploaded file. Please try again.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Please choose a file to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "File size exceeds the 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw ApiException.badRequest("INVALID_FILE_TYPE", "Only JPEG, PNG, WEBP, GIF, and SVG images are allowed");
        }
    }
}
