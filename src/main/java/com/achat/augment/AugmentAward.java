package com.achat.augment;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "augment_awards", indexes = @Index(columnList = "game_id,user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AugmentAward {
    @Id @Column(length = 100)
    private String id;
    private String gameId;
    private String roundId;
    private String userId;
    private int points;
    public AugmentAward(String eventKey, String gameId, String roundId, String userId, int points) {
        this.id = eventKey;
        this.gameId = gameId;
        this.roundId = roundId;
        this.userId = userId;
        this.points = points;
    }
}
