package com.achat.augment.effect;

import com.achat.augment.*;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import static com.achat.augment.AugmentPolicy.*;

@Component
public class RandomRewardEffects implements MessageEffectHandler {
    public Set<AugmentType> types() { return Set.of(AugmentType.JACKPOT_MESSENGER, AugmentType.FATE_DICE); }
    public MessageEffect create(Input input) {
        var progress = input.progress();
        var type = progress.getType();
        String hash = hash(AugmentPolicy.normalize(input.message().getContent()));
        boolean draw = input.eligible() && progress.getDraws() < MAX_DRAWS
                && !progress.getContentHashes().contains(hash)
                && (progress.getLastDrawAt() == null || !input.now().isBefore(progress.getLastDrawAt().plusSeconds(5)));
        if (draw) progress.draw(hash, input.now());
        int value;
        int points = 0;
        String status;
        if (type == AugmentType.JACKPOT_MESSENGER) {
            value = draw ? input.random().nextInt(100) : 100;
            boolean win = draw && value < JACKPOT_PERCENT && progress.getWins() < MAX_JACKPOT_WINS;
            if (win) {
                points = input.rewards().grant(input.message(), progress, "jackpot", JACKPOT_POINTS);
                if (points > 0) progress.win();
            }
            status = !draw ? "LIMIT" : points > 0 ? "WON" : "MISS";
        } else {
            value = input.random().nextInt(6) + 1;
            if (draw && value >= 5) points = input.rewards().grant(input.message(), progress, "dice", value == 6 ? 10 : 5);
            status = points > 0 ? "WON" : draw ? "ROLLED" : "NO_REWARD";
        }
        return new MessageEffect(type, status, null, null, value, points, null, progress.getDraws(), MAX_DRAWS);
    }
    private String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
