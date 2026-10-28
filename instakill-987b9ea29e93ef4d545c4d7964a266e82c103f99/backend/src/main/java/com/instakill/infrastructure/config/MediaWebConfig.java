package com.instakill.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    private final Path mediaRoot;

    public MediaWebConfig(@Value("${media.base-path:media}") String basePath) throws IOException {
        this.mediaRoot = Path.of(basePath).toAbsolutePath().normalize();
        Files.createDirectories(mediaRoot);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(mediaRoot.toUri().toString());
    }
}
