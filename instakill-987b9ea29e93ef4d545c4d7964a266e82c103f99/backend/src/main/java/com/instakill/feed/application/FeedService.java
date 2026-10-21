package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedService {

    private final PostRepository posts;
    private final Map<FeedSort, FeedRankingStrategy> strategies = new EnumMap<>(FeedSort.class);

    public FeedService(PostRepository posts, List<FeedRankingStrategy> rankingStrategies) {
        this.posts = posts;
        for (FeedRankingStrategy strategy : rankingStrategies) {
            FeedRankingStrategy previous = strategies.put(strategy.sort(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Multiple feed ranking strategies registered for " + strategy.sort());
            }
        }
    }

    public List<Post> listFeed(FeedSort sort, int page, int size) {
        FeedSort requested = sort == null ? FeedSort.NEW : sort;
        FeedRankingStrategy strategy = strategies.get(requested);
        if (strategy == null) {
            throw new IllegalArgumentException("No feed ranking strategy registered for " + requested);
        }
        return strategy.fetch(posts, page, size);
    }
}
