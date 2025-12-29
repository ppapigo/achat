package com.achat.friend;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class FriendStore {

    private final Map<String, FriendRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> friends = new ConcurrentHashMap<>();
    private final Map<String, RoomInvite> invites = new ConcurrentHashMap<>();

    // ===== 친구 요청 =====

    public void saveRequest(FriendRequest request) {
        requests.put(request.getId(), request);
    }

    public Optional<FriendRequest> findRequestById(String id) {
        return Optional.ofNullable(requests.get(id));
    }

    public List<FriendRequest> findPendingRequestsTo(String userId) {
        return requests.values().stream()
                .filter(r -> r.getToUserId().equals(userId))
                .filter(r -> r.getStatus() == FriendRequest.Status.PENDING)
                .sorted(Comparator.comparing(FriendRequest::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public boolean hasPendingRequest(String fromUserId, String toUserId) {
        return requests.values().stream()
                .anyMatch(r -> r.getFromUserId().equals(fromUserId)
                        && r.getToUserId().equals(toUserId)
                        && r.getStatus() == FriendRequest.Status.PENDING);
    }

    // ===== 친구 관계 =====

    public void addFriend(String userId1, String userId2) {
        friends.computeIfAbsent(userId1, k -> ConcurrentHashMap.newKeySet()).add(userId2);
        friends.computeIfAbsent(userId2, k -> ConcurrentHashMap.newKeySet()).add(userId1);
    }

    public void removeFriend(String userId1, String userId2) {
        Set<String> set1 = friends.get(userId1);
        Set<String> set2 = friends.get(userId2);
        if (set1 != null) set1.remove(userId2);
        if (set2 != null) set2.remove(userId1);
    }

    public boolean areFriends(String userId1, String userId2) {
        Set<String> set = friends.get(userId1);
        return set != null && set.contains(userId2);
    }

    public Set<String> getFriendIds(String userId) {
        return friends.getOrDefault(userId, Set.of());
    }

    // ===== 방 초대 =====

    public void saveInvite(RoomInvite invite) {
        invites.put(invite.getId(), invite);
    }

    public Optional<RoomInvite> findInviteById(String id) {
        return Optional.ofNullable(invites.get(id));
    }

    public List<RoomInvite> findPendingInvitesTo(String userId) {
        return invites.values().stream()
                .filter(i -> i.getToUserId().equals(userId))
                .filter(i -> i.getStatus() == RoomInvite.Status.PENDING)
                .filter(i -> !i.isExpired())
                .sorted(Comparator.comparing(RoomInvite::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public boolean hasPendingInvite(String toUserId, String roomId) {
        return invites.values().stream()
                .anyMatch(i -> i.getToUserId().equals(toUserId)
                        && i.getRoomId().equals(roomId)
                        && i.getStatus() == RoomInvite.Status.PENDING
                        && !i.isExpired());
    }
}