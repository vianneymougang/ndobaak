package com.ndobaak.backend.admin.controller;

import com.ndobaak.backend.media.service.ImageProcessingService;
import com.ndobaak.backend.media.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class StaticAssetController {

    private final ImageProcessingService imageProcessingService;
    private final StorageService storageService;

    public StaticAssetController(ImageProcessingService imageProcessingService,
                                 StorageService storageService) {
        this.imageProcessingService = imageProcessingService;
        this.storageService = storageService;
    }

    @PostMapping("/assets")
    public ResponseEntity<Map<String, String>> uploadAsset(
            @RequestParam MultipartFile file,
            @RequestParam String name) throws IOException {

        ImageProcessingService.ProcessedImages processed =
                imageProcessingService.process(file.getInputStream());

        byte[] original = processed.original();

        String key = storageService.uploadNamed(
                "static", name + ".webp",
                new ByteArrayInputStream(original),
                original.length,
                "image/webp");

        String url = storageService.resolvePublicUrl(key);

        return ResponseEntity.ok(Map.of("url", url));
    }
}
