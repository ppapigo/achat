package com.achat.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface UserInventoryItemRepository extends JpaRepository<UserInventoryItem, Long> {
    Optional<UserInventoryItem> findByUserIdAndItemId(String userId, String itemId);
    List<UserInventoryItem> findByUserIdOrderByAcquiredAtAsc(String userId);
}
