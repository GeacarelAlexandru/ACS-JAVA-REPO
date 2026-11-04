package com.instakill.media.processing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "image-processing")
public record ImageProcessingProperties(
        int maxWidth,
        String watermarkText
) {
    public ImageProcessingProperties {
        if (maxWidth <= 0) {
            maxWidth = 300;
        }
        if (watermarkText == null || watermarkText.isBlank()) {
            watermarkText = "Instakill Demo";
        }
    }
}
