package com.achat.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface RoomInviteRepository extends JpaRepository<RoomInvite, String> {
    List<RoomInvite> findByToUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(String userId, RoomInvite.Status status, Instant now);
    boolean existsByToUserIdAndRoomIdAndStatusAndExpiresAtAfter(String userId, String roomId, RoomInvite.Status status, Instant now);
}
