package com.achat.post;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** Posting limits survive restarts and deletion of the corresponding posts. */
@Entity
@Table(name = "post_quotas")
public class PostQuota extends JpaEntity {
    @Id
    @Column(length = 36)
    private String userId;
    private Instant lastPostAt;
    private LocalDate countDate;
    private int dailyCount;

    protected PostQuota() {}
    public PostQuota(String userId) { this.userId = userId; }
    public void recordPost() {
        LocalDate today = LocalDate.now();
        if (!today.equals(countDate)) dailyCount = 0;
        countDate = today;
        dailyCount++;
        lastPostAt = Instant.now();
    }
    public Instant getLastPostAt() { return lastPostAt; }
    public int getDailyCount() { return LocalDate.now().equals(countDate) ? dailyCount : 0; }
}
