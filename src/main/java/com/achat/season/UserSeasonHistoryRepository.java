package com.achat.season;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface UserSeasonHistoryRepository extends JpaRepository<UserSeasonHistory, Long> {
    List<UserSeasonHistory> findByUserIdOrderByRecordedAtAsc(String userId);
}
