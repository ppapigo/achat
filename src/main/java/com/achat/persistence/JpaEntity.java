package com.achat.persistence;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/** Optimistic locking also lets Spring Data recognize new entities with assigned IDs. */
@MappedSuperclass
public abstract class JpaEntity {
    @Version
    private Long version;
}
