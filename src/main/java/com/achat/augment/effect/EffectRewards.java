package com.achat.augment.effect;

import com.achat.augment.*;
import com.achat.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** A unique, persisted message event is the award receipt. All callers hold the room lock. */
@Service
@RequiredArgsConstructor
public class EffectRewards {
    private final AugmentAwardRepository awards;

    public int grant(Message message, AugmentProgress progress, String event, int requested) {
        String key = message.getId() + ":" + event;
        if (awards.existsById(key)) return 0;
        int points = requested > 0
                ? Math.min(requested, Math.max(0, AugmentPolicy.MAX_EFFECT_POINTS - progress.getAwardedPoints()))
                : Math.max(-AugmentPolicy.FORBIDDEN_PENALTY, requested);
        if (points == 0) return 0;
        awards.saveAndFlush(new AugmentAward(key, message.getGameId(), message.getRoundId(), message.getSenderId(), points));
        progress.award(points);
        return points;
    }
}
