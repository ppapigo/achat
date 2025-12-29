package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;
import com.achat.quest.QuestType;

import java.util.Random;

public class QuestGiverEffect implements AugmentEffect {

    private final Random random = new Random();

    @Override
    public AugmentType getType() { return AugmentType.QUEST_GIVER; }

    @Override
    public String getName() { return "퀘스트 기버"; }

    @Override
    public String getDescription() { return "채팅 미션을 생성하고 보상을 줍니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        // 퀘스트 타입 랜덤 선택
        QuestType questType = QuestType.values()[random.nextInt(QuestType.values().length)];

        String questText = switch (questType) {
            case REPLY_QUEST -> String.format(
                    "\n\n🎯 [퀘스트] 이 메시지에 답장 %d개 달면 %d코인!",
                    questType.getDefaultTarget(), questType.getRewardCoins()
            );
            case LIKE_QUEST -> String.format(
                    "\n\n🎯 [퀘스트] 좋아요 %d개 달면 %d코인!",
                    questType.getDefaultTarget(), questType.getRewardCoins()
            );
            case OPINION_QUEST -> String.format(
                    "\n\n🎯 [퀘스트] 의견 %d개 남기면 %d코인!",
                    questType.getDefaultTarget(), questType.getRewardCoins()
            );
        };

        // context에 퀘스트 타입 저장 (나중에 QuestService에서 사용)
        context.setQuestType(questType);

        return "📜 " + message + questText;
    }
}