package com.achat.post.dto;

import com.achat.post.Post;
import com.achat.post.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class PostDto {

    private PostDto() {}

    public record CreatePostRequest(
            PostType type,

            @NotBlank(message = "내용은 필수입니다")
            @Size(max = 500, message = "내용은 500자 이하입니다")
            String content,

            @Size(max = 3, message = "태그는 최대 3개입니다")
            List<String> tags
    ) {
        public CreatePostRequest {
            if (type == null) type = PostType.FREE;
        }
    }

    public record PostResponse(
            String id,
            String authorId,
            String authorUsername,
            PostType type,
            String content,
            List<String> tags,
            long likeCount,
            int commentCount,  // ⭐ 추가!
            boolean liked,
            Instant createdAt
    ) {
        // 기존 2개 파라미터 버전 (호환용)
        public static PostResponse from(Post post, boolean liked) {
            return new PostResponse(
                    post.getId(),
                    post.getAuthorId(),
                    null,
                    post.getType(),
                    post.getContent(),
                    post.getTags(),
                    post.getLikeCount(),
                    0,  // commentCount
                    liked,
                    post.getCreatedAt()
            );
        }

        // 3개 파라미터 버전 (username 포함)
        public static PostResponse from(Post post, boolean liked, String authorUsername) {
            return new PostResponse(
                    post.getId(),
                    post.getAuthorId(),
                    authorUsername,
                    post.getType(),
                    post.getContent(),
                    post.getTags(),
                    post.getLikeCount(),
                    0,  // commentCount
                    liked,
                    post.getCreatedAt()
            );
        }

        // ⭐ 4개 파라미터 버전 (username + commentCount)
        public static PostResponse from(Post post, boolean liked, String authorUsername, int commentCount) {
            return new PostResponse(
                    post.getId(),
                    post.getAuthorId(),
                    authorUsername,
                    post.getType(),
                    post.getContent(),
                    post.getTags(),
                    post.getLikeCount(),
                    commentCount,
                    liked,
                    post.getCreatedAt()
            );
        }
    }

    public record PostListResponse(
            List<PostResponse> posts,
            boolean hasMore
    ) {}
}