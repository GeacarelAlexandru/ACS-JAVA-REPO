package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedServiceTest {

    private final PostRepository repository = Mockito.mock(PostRepository.class);

    @Test
    void delegatesToStrategyMatchingRequestedSort() {
        FeedRankingStrategy newest = strategyFor(FeedSort.NEW);
        FeedRankingStrategy trending = strategyFor(FeedSort.TRENDING);
        List<Post> expected = List.of(post());
        when(trending.fetch(repository, 1, 5)).thenReturn(expected);
        FeedService service = new FeedService(repository, List.of(newest, trending));

        List<Post> result = service.listFeed(FeedSort.TRENDING, 1, 5);

        assertThat(result).isEqualTo(expected);
        verify(trending).fetch(repository, 1, 5);
        verify(newest, never()).fetch(any(), anyInt(), anyInt());
    }

    @Test
    void defaultsToNewWhenSortIsNull() {
        FeedRankingStrategy newest = strategyFor(FeedSort.NEW);
        FeedRankingStrategy trending = strategyFor(FeedSort.TRENDING);
        List<Post> expected = List.of(post());
        when(newest.fetch(repository, 0, 5)).thenReturn(expected);
        FeedService service = new FeedService(repository, List.of(newest, trending));

        List<Post> result = service.listFeed(null, 0, 5);

        assertThat(result).isEqualTo(expected);
        verify(trending, never()).fetch(any(), anyInt(), anyInt());
    }

    @Test
    void usesWhateverStrategyIsRegisteredWithoutChangingTheService() {
        List<Post> custom = List.of(post());
        FeedRankingStrategy replacement = new FeedRankingStrategy() {
            @Override
            public FeedSort sort() {
                return FeedSort.TRENDING;
            }

            @Override
            public List<Post> fetch(PostRepository repo, int page, int size) {
                return custom;
            }
        };
        FeedService service = new FeedService(repository, List.of(replacement));

        assertThat(service.listFeed(FeedSort.TRENDING, 0, 10)).isSameAs(custom);
    }

    @Test
    void worksWithRealStrategies() {
        List<Post> posts = List.of(post());
        when(repository.findLatest(0, 5)).thenReturn(posts);
        FeedService service = new FeedService(repository,
                List.of(new NewestFeedRankingStrategy(), new TrendingFeedRankingStrategy()));

        assertThat(service.listFeed(FeedSort.NEW, 0, 5)).isEqualTo(posts);
    }

    @Test
    void failsWhenNoStrategyIsRegisteredForSort() {
        FeedService service = new FeedService(repository, List.of(strategyFor(FeedSort.NEW)));

        assertThatThrownBy(() -> service.listFeed(FeedSort.TRENDING, 0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TRENDING");
    }

    @Test
    void rejectsTwoStrategiesForTheSameSort() {
        List<FeedRankingStrategy> duplicates = List.of(strategyFor(FeedSort.NEW), strategyFor(FeedSort.NEW));

        assertThatThrownBy(() -> new FeedService(repository, duplicates))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NEW");
    }

    private FeedRankingStrategy strategyFor(FeedSort sort) {
        FeedRankingStrategy strategy = mock(FeedRankingStrategy.class);
        when(strategy.sort()).thenReturn(sort);
        return strategy;
    }

    private Post post() {
        Instant now = Instant.now();
        return new Post(UUID.randomUUID(), UUID.randomUUID(), "content", null, now, now, 0, 0);
    }
}
