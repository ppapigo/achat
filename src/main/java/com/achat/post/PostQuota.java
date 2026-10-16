package com.achat.post;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** Posting limits survive restarts and deletion of the corresponding posts. */
@Entity
@Table(name = "post_quotas")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostQuota extends JpaEntity {
    @Id
    @Column(length = 36)
    private String userId;
    @Getter
    private Instant lastPostAt;
    private LocalDate countDate;
    private int dailyCount;

    public PostQuota(String userId) { this.userId = userId; }
    public void recordPost() {
        LocalDate today = LocalDate.now();
        if (!today.equals(countDate)) dailyCount = 0;
        countDate = today;
        dailyCount++;
        lastPostAt = Instant.now();
    }
    public int getDailyCount() { return LocalDate.now().equals(countDate) ? dailyCount : 0; }
}
