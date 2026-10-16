package com.achat;

import com.achat.augment.*;
import com.achat.augment.dto.PickAugmentRequest;
import com.achat.augment.effect.AugmentEffectRegistry;
import com.achat.job.JobStore;
import com.achat.job.UserJobState;
import com.achat.message.*;
import com.achat.message.dto.SendMessageRequest;
import com.achat.quest.QuestService;
import com.achat.room.Room;
import com.achat.room.RoomStore;
import com.achat.round.RoundService;
import com.achat.sanction.SanctionService;
import com.achat.shop.ShopService;
import com.achat.shop.UserEquipment;
import com.achat.user.UserStore;
import com.achat.websocket.ChatController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AugmentTargetingTests {
    private final AugmentStore states = new AugmentStore();
    private final RoomStore rooms = mock(RoomStore.class);
    private final JobStore jobs = mock(JobStore.class);
    private final UserStore users = mock(UserStore.class);
    private final Room room = new Room("Target selection", "alice", 10);
    private AugmentService augments;

    @BeforeEach
    void setUp() {
        room.addMember("bob");
        room.addMember("charlie");
        when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
        when(jobs.getOrCreate(anyString())).thenAnswer(call -> new UserJobState(call.getArgument(0)));
        AugmentEffectRegistry effects = new AugmentEffectRegistry();
        effects.init();
        augments = new AugmentService(states, rooms, effects, jobs, users);
    }

    private UserAugmentState offer(String userId, AugmentType type) {
        UserAugmentState state = new UserAugmentState(userId, room.getId(), List.of(type));
        states.save(state);
        return state;
    }

    private void pick(String userId, AugmentType type, String targetUserId) {
        offer(userId, type);
        augments.pick(room.getId(), userId, new PickAugmentRequest(type, targetUserId));
    }

    @Test
    void chosenAugmentOnlyAffectsTheSelectedOpponent() {
        pick("alice", AugmentType.ECHO_BURST, "bob");

        assertThat(augments.applyAugment(room.getId(), "alice", "hello")).isEqualTo("hello");
        assertThat(augments.applyAugment(room.getId(), "bob", "hello")).isEqualTo("📢 HELLO");
        assertThat(augments.applyAugment(room.getId(), "charlie", "hello")).isEqualTo("hello");
        var response = augments.getOptions(room.getId(), "alice");
        assertThat(response.selected()).isEqualTo(AugmentType.ECHO_BURST);
        assertThat(response.targetUserId()).isEqualTo("bob");
        assertThat(response.targets()).extracting("userId").containsExactly("bob", "charlie");

        pick("bob", AugmentType.THREAD_WEAVER, "alice");
        assertThat(augments.applyAugment(room.getId(), "alice", "hello")).isEqualTo("🧵 hello");
        assertThat(augments.applyAugment(room.getId(), "bob", "hello")).isEqualTo("📢 HELLO");
    }

    @Test
    void rejectsSelfMissingAndNonMemberTargetsWithoutConsumingThePick() {
        UserAugmentState state = offer("alice", AugmentType.ECHO_BURST);
        for (String invalidTarget : new String[] { "alice", "outsider", "", null }) {
            assertThatThrownBy(() -> augments.pick(room.getId(), "alice",
                    new PickAugmentRequest(AugmentType.ECHO_BURST, invalidTarget)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(state.hasSelected()).isFalse();
        }
        room.removeMember("bob");
        assertThatThrownBy(() -> augments.pick(room.getId(), "alice",
                new PickAugmentRequest(AugmentType.ECHO_BURST, "bob")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(state.hasSelected()).isFalse();
    }

    @Test
    void nonMembersCannotFetchOptionsOrSendAugments() {
        assertThatThrownBy(() -> augments.getOptions(room.getId(), "outsider"))
                .isInstanceOf(IllegalArgumentException.class);
        offer("outsider", AugmentType.ECHO_BURST);
        assertThatThrownBy(() -> augments.pick(room.getId(), "outsider",
                new PickAugmentRequest(AugmentType.ECHO_BURST, "bob")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refreshingTargetsPreservesOptionsAndCannotChangeACompletedPick() {
        var original = augments.getOptions(room.getId(), "alice");
        room.removeMember("charlie");
        var refreshed = augments.getOptions(room.getId(), "alice");
        assertThat(refreshed.options()).isEqualTo(original.options());
        assertThat(refreshed.targets()).extracting("userId").containsExactly("bob");

        AugmentType type = original.options().get(0).type();
        augments.pick(room.getId(), "alice", new PickAugmentRequest(type, "bob"));
        room.addMember("charlie");
        assertThatThrownBy(() -> augments.pick(room.getId(), "alice", new PickAugmentRequest(type, "charlie")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(augments.getOptions(room.getId(), "alice").targetUserId()).isEqualTo("bob");
    }

    @Test
    void automaticSelectionChoosesOneOpponentAndPreservesManualTargets() {
        offer("alice", AugmentType.ECHO_BURST);
        augments.autoSelectIfNotSelected(room.getId(), "alice");
        var response = augments.getOptions(room.getId(), "alice");
        assertThat(response.targetUserId()).isIn("bob", "charlie");
        assertThat(augments.applyAugment(room.getId(), response.targetUserId(), "hello")).isEqualTo("📢 HELLO");
        assertThat(augments.applyAugment(room.getId(), "alice", "hello")).isEqualTo("hello");

        pick("bob", AugmentType.THREAD_WEAVER, "charlie");
        augments.autoSelectIfNotSelected(room.getId(), "bob");
        assertThat(augments.getOptions(room.getId(), "bob").targetUserId()).isEqualTo("charlie");
        augments.autoSelectIfNotSelected(room.getId(), "charlie");
        assertThat(augments.getOptions(room.getId(), "charlie").targetUserId()).isIn("alice", "bob");
    }

    @Test
    void automaticSelectionDoesNothingWithoutAnOpponent() {
        room.removeMember("bob");
        room.removeMember("charlie");
        augments.autoSelectIfNotSelected(room.getId(), "alice");
        assertThat(augments.hasSelected(room.getId(), "alice")).isFalse();
        assertThat(augments.getOptions(room.getId(), "alice").targets()).isEmpty();
        assertThat(augments.applyAugment(room.getId(), "alice", "hello")).isEqualTo("hello");
    }

    @Test
    void expiredDepartedAndOtherRoomSelectionsDoNotApply() {
        pick("alice", AugmentType.ECHO_BURST, "bob");
        UserAugmentState state = states.find("alice", room.getId()).orElseThrow();
        ReflectionTestUtils.setField(state, "expiresAt", Instant.now().minusSeconds(1));
        assertThat(augments.applyAugment(room.getId(), "bob", "hello")).isEqualTo("hello");

        pick("alice", AugmentType.ECHO_BURST, "bob");
        Room other = new Room("Other room", "alice", 10);
        other.addMember("bob");
        when(rooms.findById(other.getId())).thenReturn(Optional.of(other));
        assertThat(augments.applyAugment(other.getId(), "bob", "hello")).isEqualTo("hello");
        room.removeMember("bob");
        assertThat(augments.applyAugment(room.getId(), "bob", "hello")).isEqualTo("hello");
        room.addMember("bob");
        room.removeMember("alice");
        assertThat(augments.applyAugment(room.getId(), "bob", "hello")).isEqualTo("hello");
    }

    @Test
    void identicalEffectsFromMultipleOpponentsOnlyApplyOnce() {
        pick("alice", AugmentType.ECHO_BURST, "bob");
        pick("charlie", AugmentType.ECHO_BURST, "bob");
        assertThat(augments.applyAugment(room.getId(), "bob", "hello")).isEqualTo("📢 HELLO");
        assertThat(augments.getAppliedAugmentTypes(room.getId(), "bob")).containsExactly(AugmentType.ECHO_BURST);
    }

    @Test
    void chatUsesIncomingEffectsForCooldownMetadataAndQuests() {
        pick("alice", AugmentType.TURBO_CASTER, "bob");
        pick("charlie", AugmentType.QUEST_GIVER, "bob");
        pick("bob", AugmentType.TIME_SHIFTER, "charlie");
        MessageStore messages = mock(MessageStore.class);
        MessageCooldownStore cooldowns = mock(MessageCooldownStore.class);
        UserMessageTracker aliceTracker = mock(UserMessageTracker.class);
        UserMessageTracker bobTracker = mock(UserMessageTracker.class);
        UserMessageTracker charlieTracker = mock(UserMessageTracker.class);
        when(cooldowns.getTracker(room.getId(), "alice")).thenReturn(aliceTracker);
        when(cooldowns.getTracker(room.getId(), "bob")).thenReturn(bobTracker);
        when(cooldowns.getTracker(room.getId(), "charlie")).thenReturn(charlieTracker);
        ShopService shop = mock(ShopService.class);
        when(shop.getEquipment(anyString())).thenAnswer(call -> new UserEquipment(call.getArgument(0)));
        QuestService quests = mock(QuestService.class);
        SanctionService sanctions = mock(SanctionService.class);
        when(sanctions.canChat(anyString())).thenReturn(true);
        ChatController chat = new ChatController(mock(SimpMessagingTemplate.class), rooms, messages,
                augments, cooldowns, mock(RoundService.class), shop, users, quests, sanctions);

        for (String sender : List.of("alice", "bob", "charlie")) {
            chat.sendMessage(room.getId(), new SendMessageRequest("hello"), () -> sender);
        }

        verify(aliceTracker).checkCooldownWithMultiplier(5, 1.0);
        verify(bobTracker).checkCooldownWithMultiplier(5, 0.6);
        verify(charlieTracker).checkCooldownWithMultiplier(5, 0.8);
        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        verify(messages, times(3)).save(saved.capture());
        List<Message> sent = saved.getAllValues();
        assertThat(sent.get(0).getAppliedContent()).isEqualTo("hello");
        assertThat(sent.get(0).getAppliedAugment()).isNull();
        assertThat(sent.get(1).getAppliedAugment()).isEqualTo("TURBO_CASTER,QUEST_GIVER");
        assertThat(sent.get(1).getAppliedContent()).contains("hello", "[퀘스트]");
        assertThat(sent.get(2).getAppliedAugment()).isEqualTo("TIME_SHIFTER");
        verify(quests).createQuest(room.getId(), sent.get(1).getId(), "bob");
        verifyNoMoreInteractions(quests);
    }
}
