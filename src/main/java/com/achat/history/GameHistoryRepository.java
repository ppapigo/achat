package com.achat.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface GameHistoryRepository extends JpaRepository<GameHistory, String> {
    List<GameHistory> findByUserIdOrderByPlayedAtAsc(String userId);
    List<GameHistory> findByUserIdOrderByPlayedAtDesc(String userId, Pageable pageable);
}
