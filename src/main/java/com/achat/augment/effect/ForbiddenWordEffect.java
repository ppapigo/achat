package com.achat.augment.effect;

import com.achat.augment.*;
import org.springframework.stereotype.Component;
import java.util.Set;
import static com.achat.augment.AugmentPolicy.*;

@Component
public class ForbiddenWordEffect implements MessageEffectHandler {
    public Set<AugmentType> types() { return Set.of(AugmentType.FORBIDDEN_WORD); }
    public MessageEffect create(Input input) {
        var progress = input.progress();
        int count = Math.min(progress.getMessagesSeen() + 1, FORBIDDEN_MESSAGES);
        int points = 0;
        String status = "COMPLETE";
        if (progress.getMessagesSeen() < FORBIDDEN_MESSAGES) {
            boolean violation = normalize(input.message().getContent()).contains(normalize(progress.getForbiddenWord()));
            if (violation) {
                progress.violate();
                points = input.rewards().grant(input.message(), progress, "forbidden", -FORBIDDEN_PENALTY);
            }
            status = violation ? "VIOLATION" : "SAFE";
            if (count == FORBIDDEN_MESSAGES) {
                status = progress.getViolations() == 0 ? "SUCCESS" : "FAILED";
                if (progress.getViolations() == 0) points = input.rewards().grant(input.message(), progress, "forbidden-complete", FORBIDDEN_BONUS);
            }
        }
        return new MessageEffect(AugmentType.FORBIDDEN_WORD, status, null, null, progress.getViolations(),
                points, progress.getForbiddenWord(), count, FORBIDDEN_MESSAGES);
    }
}
