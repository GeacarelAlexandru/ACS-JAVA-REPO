package com.instakill.notification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the public ntfy-style notification service (https://ntfy.sh).
 * Anyone who knows the topic name can read it, so keep it unguessable for anything real.
 */
@ConfigurationProperties(prefix = "notifications.ntfy")
public record NotificationProperties(
        boolean enabled,
        String baseUrl,
        String topic,
        int timeoutMillis
) {
    public NotificationProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://ntfy.sh";
        }
        if (topic == null || topic.isBlank()) {
            topic = "instakill-demo";
        }
        if (timeoutMillis <= 0) {
            timeoutMillis = 5000;
        }
    }
}
