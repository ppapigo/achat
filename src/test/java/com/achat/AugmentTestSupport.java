package com.achat;

import com.achat.augment.*;
import com.achat.augment.dto.PickAugmentRequest;
import com.achat.common.RoomExecution;
import com.achat.message.*;
import com.achat.message.dto.*;
import com.achat.room.*;
import com.achat.round.*;
import com.achat.round.dto.StartRoundRequest;
import com.achat.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;

@SpringBootTest
abstract class AugmentTestSupport {
    @Autowired AugmentService augments;
    @Autowired AugmentStore states;
    @Autowired AugmentProgressRepository progress;
    @Autowired AugmentAwardRepository awards;
    @Autowired RoomStore rooms;
    @Autowired UserStore users;
    @Autowired RoundStore roundStore;
    @Autowired RoundService rounds;
    @Autowired RoundScheduler scheduler;
    @Autowired RoomExecution execution;
    @Autowired MessageStore messages;
    @Autowired MessageRepository messageRepository;
    @Autowired MessagePresenter presenter;
    @Autowired MessageCooldownStore cooldowns;
    @Autowired ChatMessageService chat;
    String roomId;
    List<String> ids;

    @BeforeEach
    void createRoomAndCycle() {
        ids = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            User user = new User(UUID.randomUUID() + "@augment.test", "hash");
            users.save(user); ids.add(user.getId());
        }
        Room room = new Room("augment test", ids.getFirst(), 10);
        ids.forEach(room::addMember); rooms.save(room); roomId = room.getId();
        rounds.startRound(roomId, ids.getFirst(), new StartRoundRequest(10));
        scheduler.cancelScheduledTask(roomId);
    }

    @AfterEach
    void stopTimer() { if (roomId != null) scheduler.cancelScheduledTask(roomId); }

    Round round() { return roundStore.findByRoomId(roomId).orElseThrow(); }
    UserAugmentState offer(int user, AugmentType type) {
        return execution.run(roomId, () -> {
            states.remove(ids.get(user), roomId);
            return states.saveIfAbsent(new UserAugmentState(ids.get(user), roomId, round().getRoundId(),
                    List.of(type), round().getPhaseEndsAt()));
        });
    }
    void pick(int sender, int target, AugmentType type) {
        offer(sender, type);
        augments.pick(roomId, ids.get(sender), new PickAugmentRequest(type, ids.get(target), round().getRoundId()));
    }
    void playing(AugmentType type) {
        pick(0, 1, type);
        execution.run(roomId, () -> round().startPlaying());
    }
    MessageResponse send(int sender, String content) {
        return chat.send(roomId, ids.get(sender), new SendMessageRequest(content, null, UUID.randomUUID().toString()));
    }
}
