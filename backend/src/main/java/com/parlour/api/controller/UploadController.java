package com.parlour.api.controller;

import com.parlour.api.dto.catalog.UploadResponse;
import com.parlour.api.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Image upload for services and the hero banner. OWNER only. */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService files;

    public UploadController(FileStorageService files) {
        this.files = files;
    }

    @PostMapping
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(files.store(file));
    }
}
