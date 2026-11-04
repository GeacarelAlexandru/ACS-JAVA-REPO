package com.instakill.post.application;

import com.instakill.common.id.IdGenerator;
import com.instakill.feed.application.FeedService;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.post.domain.PostRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceTest {

    @Test
    void createPostPublishesPostCreatedEvent() {
        PostRepository repository = mock(PostRepository.class);
        when(repository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-04T10:00:00Z");
        IdGenerator ids = () -> postId;
        PostService service = new PostService(repository, ids, () -> now, mock(FeedService.class), events);

        service.createPost(authorId, "hello", null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new PostCreatedEvent(postId, authorId, now));
    }
}
