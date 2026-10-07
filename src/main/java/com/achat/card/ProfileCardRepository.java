package com.achat.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface ProfileCardRepository extends JpaRepository<ProfileCard, Long> {
    boolean existsByOwnerIdAndDrawnDate(String ownerId, LocalDate drawnDate);
    List<ProfileCard> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    long countByOwnerId(String ownerId);
}
