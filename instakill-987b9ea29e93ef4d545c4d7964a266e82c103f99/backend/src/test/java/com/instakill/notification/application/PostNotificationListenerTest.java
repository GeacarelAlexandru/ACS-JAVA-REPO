package com.instakill.notification.application;

import com.instakill.common.error.NotFoundException;
import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.user.application.UserService;
import com.instakill.user.domain.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostNotificationListenerTest {

    private final NotificationSender sender = mock(NotificationSender.class);
    private final UserService userService = mock(UserService.class);
    private final PostNotificationListener listener = new PostNotificationListener(sender, userService);

    private final UUID authorId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();
    private final Instant when = Instant.parse("2026-11-04T10:15:30Z");

    @Test
    void sendsUsernamePostIdAndTimestamp() {
        User user = mock(User.class);
        when(user.username()).thenReturn("alice");
        when(userService.getById(authorId)).thenReturn(user);

        listener.onPostCreated(new PostCreatedEvent(postId, authorId, when));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sender).send(org.mockito.ArgumentMatchers.eq("New Instakill post"), message.capture());
        assertThat(message.getValue())
                .contains("alice")
                .contains(postId.toString())
                .contains("2026-11-04T10:15:30Z");
    }

    @Test
    void failuresAreSwallowedSoPostCreationIsNeverAffected() {
        when(userService.getById(authorId)).thenThrow(new NotFoundException("User not found"));

        assertThatCode(() -> listener.onPostCreated(new PostCreatedEvent(postId, authorId, when)))
                .doesNotThrowAnyException();
        verify(sender, never()).send(any(), any());
    }

    @Test
    void senderErrorsAreSwallowed() {
        User user = mock(User.class);
        when(user.username()).thenReturn("bob");
        when(userService.getById(authorId)).thenReturn(user);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(sender).send(any(), any());

        assertThatCode(() -> listener.onPostCreated(new PostCreatedEvent(postId, authorId, when)))
                .doesNotThrowAnyException();
    }
}
