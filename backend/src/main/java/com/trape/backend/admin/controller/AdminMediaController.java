package com.trape.backend.admin.controller;

import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.common.service.SupabaseStorageService;
import com.trape.backend.common.service.SupabaseStorageService.MediaUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/media")
@Tag(name = "Admin Media", description = "Media and image upload endpoints for product catalog")
public class AdminMediaController {

    private final SupabaseStorageService storageService;

    public AdminMediaController(SupabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a single product image from the admin's device")
    public ApiResponse<MediaUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        MediaUploadResponse response = storageService.uploadImage(file);
        return ApiResponse.ok(response);
    }

    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple product images from the admin's device in a single request")
    public ApiResponse<List<MediaUploadResponse>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        List<MediaUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(storageService.uploadImage(file));
        }
        return ApiResponse.ok(responses);
    }
}
