package com.instakill.notification.application;

import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.user.application.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends a notification for every new post. Runs on a background thread and only after the post's transaction has
 * committed, so a slow or failing notification service can never delay or roll back post creation.
 */
@Component
public class PostNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(PostNotificationListener.class);

    private final NotificationSender sender;
    private final UserService userService;

    public PostNotificationListener(NotificationSender sender, UserService userService) {
        this.sender = sender;
        this.userService = userService;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        try {
            String username = userService.getById(event.authorId()).username();
            sender.send("New Instakill post", format(username, event));
        } catch (RuntimeException e) {
            log.warn("Could not send notification for post {}", event.postId(), e);
        }
    }

    static String format(String username, PostCreatedEvent event) {
        return "User: " + username + "\n"
                + "Post ID: " + event.postId() + "\n"
                + "Created at: " + event.occurredAt();
    }
}
