package com.achat.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface FriendRequestRepository extends JpaRepository<FriendRequest, String> {
    List<FriendRequest> findByToUserIdAndStatusOrderByCreatedAtDesc(String userId, FriendRequest.Status status);
    boolean existsByFromUserIdAndToUserIdAndStatus(String fromUserId, String toUserId, FriendRequest.Status status);
}
