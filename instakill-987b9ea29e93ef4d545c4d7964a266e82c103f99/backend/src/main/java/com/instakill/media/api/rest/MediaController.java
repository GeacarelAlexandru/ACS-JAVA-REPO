package com.instakill.media.api.rest;

import com.instakill.common.error.BadRequestException;
import com.instakill.media.processing.ImageProcessingException;
import com.instakill.media.processing.ImageProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    // webp is not allowed: the JDK cannot decode it, so it could not be resized or watermarked.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "mp4", "mov", "webm");

    private final Path mediaRoot;
    private final ImageProcessor imageProcessor;

    public MediaController(@Value("${media.base-path:media}") String basePath, ImageProcessor imageProcessor) {
        this.mediaRoot = Path.of(basePath).toAbsolutePath().normalize();
        this.imageProcessor = imageProcessor;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(mediaRoot);
            if (imageProcessor.supports(extension)) {
                // Images are resized and watermarked before they are saved; videos are stored as uploaded.
                Files.write(mediaRoot.resolve(filename), processImage(file, extension));
            } else {
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, mediaRoot.resolve(filename));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file", e);
        }
        return new MediaResponse("/media/" + filename);
    }

    private byte[] processImage(MultipartFile file, String extension) throws IOException {
        try {
            return imageProcessor.process(file.getBytes(), extension);
        } catch (ImageProcessingException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot < 0 ? "" : originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
