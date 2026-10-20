package com.achat.quest;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.user.UserStore;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import com.achat.common.RoomExecution;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestService {

    private final QuestStore questStore;
    private final RoomExecution execution;
    private final UserStore userStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();

    /**
     * 퀘스트 생성 (QuestGiverEffect에서 호출)
     */
    public Quest createQuest(String roomId, String messageId, String creatorId) {
        return execution.run(roomId, () -> createQuestLocked(roomId, messageId, creatorId));
    }
    private Quest createQuestLocked(String roomId, String messageId, String creatorId) {
        var existing = questStore.findByMessageId(messageId);
        if (existing.isPresent()) return existing.get();
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
        execution.run(roomId, () -> onReplyLocked(roomId, replyToId, odLRXnwB));
    }
    private void onReplyLocked(String roomId, String replyToId, String odLRXnwB) {
        questStore.findByMessageId(replyToId).ifPresent(quest -> {
            if (!quest.getRoomId().equals(roomId) || quest.isCompleted() || quest.isCancelled() || quest.isExpired()) return;
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
        execution.run(roomId, () -> onLikeLocked(roomId, messageId, odLRXnwB));
    }
    private void onLikeLocked(String roomId, String messageId, String odLRXnwB) {
        questStore.findByMessageId(messageId).ifPresent(quest -> {
            if (!quest.getRoomId().equals(roomId) || quest.isCompleted() || quest.isCancelled() || quest.isExpired()) return;
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

        System.out.println("퀘스트 완료! 보상 지급: " + coinsEach + "코인");
    }

    private void broadcastQuestProgress(Quest quest) {
        RoomExecution.afterCommit(() -> messagingTemplate.convertAndSend("/topic/rooms/" + quest.getRoomId() + "/quest",
                Map.of(
                        "type", "progress",
                        "questId", quest.getId(), "messageId", quest.getMessageId(),
                        "current", quest.getCurrentCount(),
                        "target", quest.getTargetCount()
                )));
    }

    private void broadcastQuestComplete(Quest quest) {
        RoomExecution.afterCommit(() -> messagingTemplate.convertAndSend("/topic/rooms/" + quest.getRoomId() + "/quest",
                Map.of(
                        "type", "complete",
                        "questId", quest.getId(), "messageId", quest.getMessageId(),
                        "reward", quest.getRewardCoins()
                )));
    }

    private QuestType getRandomQuestType() {
        QuestType[] types = QuestType.values();
        return types[random.nextInt(types.length)];
    }
}
