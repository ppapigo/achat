package com.achat.quest;

import org.springframework.transaction.annotation.Transactional;

import com.achat.user.UserStore;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

@Service
@Transactional
public class QuestService {

    private final QuestStore questStore;
    private final UserStore userStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();

    public QuestService(QuestStore questStore, UserStore userStore,
                        SimpMessagingTemplate messagingTemplate) {
        this.questStore = questStore;
        this.userStore = userStore;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 퀘스트 생성 (QuestGiverEffect에서 호출)
     */
    public Quest createQuest(String roomId, String messageId, String creatorId) {
        QuestType type = getRandomQuestType();
        Quest quest = new Quest(roomId, messageId, creatorId, type);
        questStore.save(quest);

        System.out.println("퀘스트 생성: " + quest.getType() + " by " + creatorId);
        return quest;
    }

    /**
     * 답장 시 퀘스트 진행
     */
    public void onReply(String roomId, String replyToId, String odLRXnwB) {
        questStore.findByMessageId(replyToId).ifPresent(quest -> {
            if (quest.getType() == QuestType.REPLY_QUEST ||
                    quest.getType() == QuestType.OPINION_QUEST) {

                boolean completed = quest.tryProgress(odLRXnwB);

                if (completed) {
                    giveRewards(quest);
                } else {
                    // 진행 상황 알림
                    broadcastQuestProgress(quest);
                }
            }
        });
    }

    /**
     * 좋아요 시 퀘스트 진행
     */
    public void onLike(String roomId, String messageId, String odLRXnwB) {
        questStore.findByMessageId(messageId).ifPresent(quest -> {
            if (quest.getType() == QuestType.LIKE_QUEST) {
                boolean completed = quest.tryProgress(odLRXnwB);

                if (completed) {
                    giveRewards(quest);
                } else {
                    broadcastQuestProgress(quest);
                }
            }
        });
    }

    /**
     * 보상 지급
     */
    private void giveRewards(Quest quest) {
        int coinsEach = quest.getRewardCoins();

        // 퀘스트 생성자에게 보상
        userStore.findById(quest.getCreatorId()).ifPresent(user -> {
            user.addCoins(coinsEach);
            userStore.save(user);
        });

        // 참여자들에게도 보상 (절반)
        quest.getCompletedBy().forEach(odLRXnwB -> {
            userStore.findById(odLRXnwB).ifPresent(user -> {
                user.addCoins(coinsEach / 2);
                userStore.save(user);
            });
        });

        // 완료 알림
        broadcastQuestComplete(quest);
        questStore.remove(quest.getMessageId());

        System.out.println("퀘스트 완료! 보상 지급: " + coinsEach + "코인");
    }

    private void broadcastQuestProgress(Quest quest) {
        messagingTemplate.convertAndSend("/topic/rooms/" + quest.getRoomId() + "/quest",
                Map.of(
                        "type", "progress",
                        "questId", quest.getId(),
                        "current", quest.getCurrentCount(),
                        "target", quest.getTargetCount()
                ));
    }

    private void broadcastQuestComplete(Quest quest) {
        messagingTemplate.convertAndSend("/topic/rooms/" + quest.getRoomId() + "/quest",
                Map.of(
                        "type", "complete",
                        "questId", quest.getId(),
                        "reward", quest.getRewardCoins(),
                        "creatorId", quest.getCreatorId()
                ));
    }

    private QuestType getRandomQuestType() {
        QuestType[] types = QuestType.values();
        return types[random.nextInt(types.length)];
    }
}