package com.achat.friend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class FriendStore {
    private final FriendRequestRepository requestRepository;
    private final RoomInviteRepository inviteRepository;
    private final FriendConnectionRepository connectionRepository;

    public void saveRequest(FriendRequest request) { requestRepository.save(request); }
    public Optional<FriendRequest> findRequestById(String id) { return requestRepository.findById(id); }
    public List<FriendRequest> findPendingRequestsTo(String userId) {
        return requestRepository.findByToUserIdAndStatusOrderByCreatedAtDesc(userId, FriendRequest.Status.PENDING);
    }
    public boolean hasPendingRequest(String fromUserId, String toUserId) {
        return requestRepository.existsByFromUserIdAndToUserIdAndStatus(fromUserId, toUserId, FriendRequest.Status.PENDING);
    }
    public void addFriend(String userId1, String userId2) {
        addConnection(userId1, userId2);
        addConnection(userId2, userId1);
    }
    private void addConnection(String userId, String friendId) {
        if (!areFriends(userId, friendId)) connectionRepository.save(new FriendConnection(userId, friendId));
    }
    public void removeFriend(String userId1, String userId2) {
        connectionRepository.deleteByUserIdAndFriendId(userId1, userId2);
        connectionRepository.deleteByUserIdAndFriendId(userId2, userId1);
    }
    public boolean areFriends(String userId1, String userId2) { return connectionRepository.existsByUserIdAndFriendId(userId1, userId2); }
    public Set<String> getFriendIds(String userId) {
        return connectionRepository.findByUserId(userId).stream().map(FriendConnection::getFriendId).collect(java.util.stream.Collectors.toSet());
    }
    public void saveInvite(RoomInvite invite) { inviteRepository.save(invite); }
    public Optional<RoomInvite> findInviteById(String id) { return inviteRepository.findById(id); }
    public List<RoomInvite> findPendingInvitesTo(String userId) {
        return inviteRepository.findByToUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(userId, RoomInvite.Status.PENDING, Instant.now());
    }
    public boolean hasPendingInvite(String toUserId, String roomId) {
        return inviteRepository.existsByToUserIdAndRoomIdAndStatusAndExpiresAtAfter(toUserId, roomId, RoomInvite.Status.PENDING, Instant.now());
    }
}
