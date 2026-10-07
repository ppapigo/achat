package com.achat.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Transactional(readOnly = true)
public interface FriendConnectionRepository extends JpaRepository<FriendConnection, Long> {
    boolean existsByUserIdAndFriendId(String userId, String friendId);
    List<FriendConnection> findByUserId(String userId);
    @Transactional
    void deleteByUserIdAndFriendId(String userId, String friendId);
}
