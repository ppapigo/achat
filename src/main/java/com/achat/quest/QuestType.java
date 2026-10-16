package com.achat.quest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum QuestType {
    REPLY_QUEST("답장 달기", 3, 10),        // 답장 3개 → 10코인
    LIKE_QUEST("좋아요 누르기", 5, 15),     // 좋아요 5개 → 15코인
    OPINION_QUEST("의견 남기기", 3, 10);    // 답장 3개 → 10코인

    private final String description;
    private final int defaultTarget;
    private final int rewardCoins;
}
