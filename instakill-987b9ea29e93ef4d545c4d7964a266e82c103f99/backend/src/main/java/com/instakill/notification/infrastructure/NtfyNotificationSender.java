package com.instakill.notification.infrastructure;

import com.instakill.notification.application.NotificationProperties;
import com.instakill.notification.application.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Publishes messages to an ntfy server with a plain HTTP POST: the body is the message, the Title header the title.
 */
@Component
public class NtfyNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NtfyNotificationSender.class);

    private final NotificationProperties properties;
    private final RestClient client;

    public NtfyNotificationSender(NotificationProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeoutMillis());
        factory.setReadTimeout(properties.timeoutMillis());
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void send(String title, String message) {
        if (!properties.enabled()) {
            log.debug("Notifications disabled, skipping: {}", title);
            return;
        }
        String url = properties.baseUrl().replaceAll("/+$", "") + "/" + properties.topic();
        try {
            client.post()
                    .uri(url)
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("Title", title)
                    .header("Tags", "camera")
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notification sent to {}", url);
        } catch (RuntimeException e) {
            log.warn("Notification to {} failed: {}", url, e.getMessage());
        }
    }
}
