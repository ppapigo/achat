package com.achat.message.dto;

public record LikeResponse(
        String messageId,
        int likeCount,
        boolean liked,
        String odUserId,
        long revision
) {
    public LikeResponse(String messageId, int likeCount, boolean liked, String userId) {
        this(messageId, likeCount, liked, userId, 0);
    }
    @com.fasterxml.jackson.annotation.JsonProperty("userId")
    public String userId() { return odUserId; }
}
