package com.instakill.post.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Published (in-process) after a post has been created. Listeners react after the transaction commits.
 */
public record PostCreatedEvent(UUID postId, UUID authorId, Instant occurredAt) {
    public PostCreatedEvent {
        if (postId == null || authorId == null || occurredAt == null) {
            throw new IllegalArgumentException("postId, authorId and occurredAt must not be null");
        }
    }
}
