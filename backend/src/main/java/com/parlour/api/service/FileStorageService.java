package com.parlour.api.service;

import com.parlour.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Saves owner-uploaded images to the local "uploads" folder. Only real images are accepted. */
@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** Returns the public URL path, e.g. /uploads/3f2a....png */
    public String store(MultipartFile file) {
        String type = file.getContentType();
        String ext = type == null ? null : EXTENSIONS.get(type.toLowerCase());
        if (ext == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, WEBP or GIF images are allowed");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (!looksLikeImage(head, ext)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "File content is not a valid image");
            }
            String name = UUID.randomUUID() + "." + ext; // never trust the client's file name
            Path target = root.resolve(name).normalize();
            try (InputStream again = file.getInputStream()) {
                Files.copy(again, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the file");
        }
    }

    private static boolean looksLikeImage(byte[] h, String ext) {
        if (h.length < 12) {
            return false;
        }
        return switch (ext) {
            case "jpg" -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "png" -> (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "gif" -> h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
            case "webp" -> new String(h, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(h, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }
}
