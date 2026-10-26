package com.achat;

import com.achat.common.GlobalExceptionHandler;
import com.achat.feed.FeedController;
import com.achat.feed.FeedService;
import com.achat.friend.FriendStore;
import com.achat.post.Post;
import com.achat.post.PostStore;
import com.achat.post.PostType;
import com.achat.social.InteractionStore;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class FeedTests {
    @Autowired FeedService feed;
    @Autowired FeedController controller;
    @Autowired PostStore posts;
    @Autowired UserStore users;
    @Autowired FriendStore friends;
    @Autowired InteractionStore interactions;

    private String user() {
        User user = new User(UUID.randomUUID() + "@example.test", "password-hash");
        users.save(user);
        return user.getId();
    }

    private Post post(String author, Instant createdAt) {
        Post post = new Post(author, PostType.FREE, "개인 피드 소식");
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        posts.save(post);
        return post;
    }

    @Test
    void cursorKeepsEqualTimestampPostsStableWhenNewPostsArrive() {
        String author = user();
        Instant time = Instant.parse("2026-01-01T12:00:00Z");
        var expected = new ArrayList<String>();
        for (int i = 0; i < 23; i++) expected.add(post(author, time).getId());
        expected.sort(Comparator.reverseOrder());

        var first = feed.getFeed(author, 20, null);
        assertThat(first.posts()).extracting(p -> p.id()).containsExactlyElementsOf(expected.subList(0, 20));
        assertThat(first.hasMore()).isTrue();
        assertThat(first.nextCursor()).isEqualTo(expected.get(19));
        post(author, time.plusSeconds(1));

        var second = feed.getFeed(author, 20, first.nextCursor());
        assertThat(second.posts()).extracting(p -> p.id()).containsExactlyElementsOf(expected.subList(20, 23));
        assertThat(second.hasMore()).isFalse();
        assertThat(second.nextCursor()).isNull();
    }

    @Test
    void feedContainsSelfFriendsAndRecentContactsOnly() throws Exception {
        String viewer = user(), friend = user(), contact = user(), stranger = user();
        friends.addFriend(viewer, friend);
        interactions.record(viewer, contact);
        Instant time = Instant.now();
        var own = post(viewer, time);
        var friendPost = post(friend, time);
        var contactPost = post(contact, time);
        post(stranger, time.plusSeconds(1));
        assertThat(feed.getFeed(viewer, 20, null).posts()).extracting(p -> p.id())
                .containsExactlyInAnyOrder(own.getId(), friendPost.getId(), contactPost.getId());
        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/feed").requestAttr("userId", viewer).param("limit", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.posts.length()").value(1))
                .andExpect(jsonPath("$.data.hasMore").value(true)).andExpect(jsonPath("$.data.nextCursor").isString());
    }

    @Test
    void commentsValidateContentAndOnlyTheirAuthorCanDeleteThem() throws Exception {
        String author = user(), other = user();
        Post post = post(author, Instant.now());
        Post anotherPost = post(author, Instant.now());
        assertThatThrownBy(() -> feed.addComment(post.getId(), author, "  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> feed.addComment(post.getId(), author, "x".repeat(2001))).isInstanceOf(IllegalArgumentException.class);
        var comment = feed.addComment(post.getId(), author, "내 댓글");
        assertThat(feed.getComments(post.getId())).hasSize(1);
        assertThat(feed.getFeed(author, 20, null).posts()).filteredOn(p -> p.id().equals(post.getId()))
                .allSatisfy(p -> assertThat(p.commentCount()).isEqualTo(1));
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(delete("/api/feed/{post}/comments/{comment}", post.getId(), comment.id()).requestAttr("userId", other))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/feed/{post}/comments/{comment}", anotherPost.getId(), comment.id()).requestAttr("userId", author))
                .andExpect(status().isBadRequest());
        assertThat(feed.getComments(post.getId())).hasSize(1);
        mvc.perform(delete("/api/feed/{post}/comments/{comment}", post.getId(), comment.id()).requestAttr("userId", author))
                .andExpect(status().isOk());
        assertThat(feed.getComments(post.getId())).isEmpty();
    }

    @Test
    void emptyAndInvalidCursorsAreHandled() {
        String author = user();
        assertThat(feed.getFeed(author, 20, null).posts()).isEmpty();
        assertThat(feed.getFeed(author, 20, null).hasMore()).isFalse();
        assertThatThrownBy(() -> feed.getFeed(author, 20, "missing-post")).isInstanceOf(IllegalArgumentException.class);
    }
}
