package com.achat.season;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;

@Entity
@Table(name = "seasons")
public class Season extends JpaEntity {
    protected Season() {}

    @Id
    private int id;
    private String name;
    private Instant startDate;
    private Instant endDate;
    private boolean active;

    public Season(int id, String name, Instant startDate, Instant endDate) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Instant getStartDate() { return startDate; }
    public Instant getEndDate() { return endDate; }
    public boolean isActive() { return active; }

    public void end() {
        this.active = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(endDate);
    }

    public long getRemainingDays() {
        long seconds = endDate.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, seconds / 86400);
    }
}