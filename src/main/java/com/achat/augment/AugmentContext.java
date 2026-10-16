package com.achat.augment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import com.achat.quest.QuestType;

@RequiredArgsConstructor
@Getter
@Setter
public class AugmentContext {
    @Getter(AccessLevel.NONE)
    private final String odLRXnwB;
    private final String roomId;
    private final String originalMessage;
    private QuestType questType;  // ✅ 추가

    public String getUserId() { return odLRXnwB; }
}
