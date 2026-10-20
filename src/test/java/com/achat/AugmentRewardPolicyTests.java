package com.achat;

import com.achat.augment.*;
import com.achat.augment.effect.*;
import com.achat.message.Message;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AugmentRewardPolicyTests {
    private final AugmentRandom random = mock(AugmentRandom.class);
    private final AugmentAwardRepository awards = mock(AugmentAwardRepository.class);
    private final EffectRewards rewards = new EffectRewards(awards);
    private final RandomRewardEffects rules = new RandomRewardEffects();
    private final Set<String> receipts = new HashSet<>();
    private final Instant start = Instant.parse("2026-10-20T12:00:00Z");

    AugmentRewardPolicyTests() {
        when(awards.existsById(anyString())).thenAnswer(call -> receipts.contains(call.getArgument(0)));
        when(awards.saveAndFlush(any())).thenAnswer(call -> {
            AugmentAward award = call.getArgument(0); receipts.add(award.getId()); return award;
        });
    }
    private MessageEffect draw(AugmentProgress state, String content, int seconds, boolean eligible) {
        Message message = new Message("room", "target", null, content, content, state.getType().name());
        message.setGameId("game"); message.setRoundId("cycle");
        return rules.create(new MessageEffectHandler.Input(message, state, eligible, start.plusSeconds(seconds), random, rewards));
    }
    @Test void jackpotHasDrawWinAndPointCapsWithNoDuplicateContentOrFastRepeatRewards() {
        var state = new AugmentProgress("room", "cycle", "target", AugmentType.JACKPOT_MESSENGER, null);
        assertThat(draw(state, "unique eligible content", 0, true).points()).isEqualTo(10);
        assertThat(draw(state, "UNIQUE  eligible content", 5, true).points()).isZero();
        assertThat(draw(state, "another eligible content", 1, true).points()).isZero();
        for (int i = 1; i < 15; i++) draw(state, "different eligible content " + i, i * 6, true);
        assertThat(state.getDraws()).isEqualTo(10);
        assertThat(state.getWins()).isEqualTo(3);
        assertThat(state.getAwardedPoints()).isEqualTo(30);
    }
    @Test void diceAwardsAreBoundedAndUnscoredMessagesStillHaveAStableVisualResult() {
        when(random.nextInt(6)).thenReturn(5);
        var state = new AugmentProgress("room", "cycle", "target", AugmentType.FATE_DICE, null);
        for (int i = 0; i < 15; i++) assertThat(draw(state, "eligible dice content " + i, i * 6, true).value()).isEqualTo(6);
        assertThat(state.getDraws()).isEqualTo(10);
        assertThat(state.getAwardedPoints()).isEqualTo(50);
        var noReward = draw(state, "unscored content", 120, false);
        assertThat(noReward.value()).isEqualTo(6); assertThat(noReward.points()).isZero();
    }
    @Test void diceValidatesAllSixOutcomesAndRejectsOutOfRangeMetadata() {
        for (int face = 1; face <= 6; face++) {
            when(random.nextInt(6)).thenReturn(face - 1);
            var state = new AugmentProgress("room", "cycle", "target", AugmentType.FATE_DICE, null);
            var effect = draw(state, "dice result " + face, 0, true);
            assertThat(effect.value()).isEqualTo(face);
            assertThat(effect.points()).isEqualTo(face == 6 ? 10 : face == 5 ? 5 : 0);
        }
        assertThatThrownBy(() -> new MessageEffect(AugmentType.FATE_DICE, "ROLLED", null, null, 7, 0, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void persistedEventReceiptPreventsAwardingTheSameMessageTwice() {
        var state = new AugmentProgress("room", "cycle", "target", AugmentType.TIME_BOMB, null);
        var message = new Message("room", "target", null, "content", "content", "TIME_BOMB");
        assertThat(rewards.grant(message, state, "bomb", 10)).isEqualTo(10);
        assertThat(rewards.grant(message, state, "bomb", 10)).isZero();
        assertThat(state.getAwardedPoints()).isEqualTo(10);
    }
    @Test void legacyNullMetadataAndEveryNewMetadataFieldRoundTrip() {
        var converter = new MessageEffectsConverter();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        var effect = new MessageEffect(AugmentType.FATE_DICE, "WON", start, start.plusSeconds(15), 6, 10, "safe", 1, 10);
        assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(List.of(effect)))).containsExactly(effect);
    }
}
