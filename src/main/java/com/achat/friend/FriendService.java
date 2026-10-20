package com.achat.friend;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.room.Room;
import com.achat.room.RoomStore;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendService {
    private final com.achat.common.RoomExecution execution;
    private final com.achat.security.AccountAccess access;

    private final FriendStore friendStore;
    private final UserStore userStore;
    private final RoomStore roomStore;
    private final SimpMessagingTemplate messagingTemplate;

    // 온라인 상태 추적
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final Map<String, String> userCurrentRoom = new ConcurrentHashMap<>();

    // ===== 온라인 상태 =====

    public void setOnline(String userId, boolean online) {
        if (online) {
            onlineUsers.add(userId);
        } else {
            onlineUsers.remove(userId);
            userCurrentRoom.remove(userId);
        }
        notifyFriendsStatus(userId, online);
    }

    public void setCurrentRoom(String userId, String roomId) {
        if (roomId == null) {
            userCurrentRoom.remove(userId);
        } else {
            userCurrentRoom.put(userId, roomId);
        }
    }

    public boolean isOnline(String userId) {
        return onlineUsers.contains(userId);
    }

    public String getCurrentRoom(String userId) {
        return userCurrentRoom.get(userId);
    }

    private void notifyFriendsStatus(String userId, boolean online) {
        User user = userStore.findById(userId).orElse(null);
        if (user == null) return;

        for (String friendId : friendStore.getFriendIds(userId)) {
            messagingTemplate.convertAndSendToUser(friendId, "/queue/friends",
                    Map.of(
                            "type", "status",
                            "userId", userId,
                            "username", user.getUsername(),
                            "online", online
                    ));
        }
    }

    // ===== 친구 요청 =====

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public FriendRequest sendRequest(String fromUserId, String toUsername) {
        User toUser = userStore.findByUsername(toUsername)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + toUsername));

        String toUserId = toUser.getId();

        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("자신에게 친구 요청을 보낼 수 없습니다");
        }

        if (friendStore.areFriends(fromUserId, toUserId)) {
            throw new IllegalArgumentException("이미 친구입니다");
        }

        if (friendStore.hasPendingRequest(fromUserId, toUserId)) {
            throw new IllegalArgumentException("이미 요청을 보냈습니다");
        }

        // 상대방이 나에게 요청 보낸 경우 -> 바로 수락
        if (friendStore.hasPendingRequest(toUserId, fromUserId)) {
            friendStore.findPendingRequestsTo(fromUserId).stream()
                    .filter(r -> r.getFromUserId().equals(toUserId))
                    .findFirst()
                    .ifPresent(r -> acceptRequest(r.getId(), fromUserId));
            throw new IllegalArgumentException("상대방의 요청을 수락했습니다");
        }

        FriendRequest request = new FriendRequest(fromUserId, toUserId);
        friendStore.saveRequest(request);

        // 실시간 알림
        User fromUser = userStore.findById(fromUserId).orElse(null);
        messagingTemplate.convertAndSendToUser(toUserId, "/queue/friends",
                Map.of(
                        "type", "request",
                        "requestId", request.getId(),
                        "from", Map.of(
                                "id", fromUserId,
                                "username", fromUser != null ? fromUser.getUsername() : "Unknown"
                        )
                ));

        return request;
    }

    public List<Map<String, Object>> getPendingRequests(String userId) {
        return friendStore.findPendingRequestsTo(userId).stream()
                .map(r -> {
                    User from = userStore.findById(r.getFromUserId()).orElse(null);
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("from", Map.of(
                            "id", r.getFromUserId(),
                            "username", from != null ? from.getUsername() : "Unknown"
                    ));
                    map.put("createdAt", r.getCreatedAt().toString());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public void acceptRequest(String requestId, String userId) {
        FriendRequest request = friendStore.findRequestById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다"));

        if (!request.getToUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        if (request.getStatus() != FriendRequest.Status.PENDING) {
            throw new IllegalArgumentException("이미 처리된 요청입니다");
        }

        request.accept();
        friendStore.addFriend(request.getFromUserId(), request.getToUserId());

        // 요청자에게 알림
        User acceptedUser = userStore.findById(userId).orElse(null);
        messagingTemplate.convertAndSendToUser(request.getFromUserId(), "/queue/friends",
                Map.of(
                        "type", "accepted",
                        "friend", Map.of(
                                "id", userId,
                                "username", acceptedUser != null ? acceptedUser.getUsername() : "Unknown"
                        )
                ));
    }

    public void rejectRequest(String requestId, String userId) {
        FriendRequest request = friendStore.findRequestById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다"));

        if (!request.getToUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        request.reject();
    }

    // ===== 친구 목록 =====

    public List<Map<String, Object>> getFriendList(String userId) {
        return friendStore.getFriendIds(userId).stream()
                .map(friendId -> {
                    User friend = userStore.findById(friendId).orElse(null);
                    if (friend == null) return null;

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", friend.getId());
                    map.put("username", friend.getUsername());
                    map.put("online", isOnline(friendId));
                    map.put("currentRoomId", getCurrentRoom(friendId));
                    return map;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    boolean aOnline = (boolean) a.get("online");
                    boolean bOnline = (boolean) b.get("online");
                    if (aOnline != bOnline) return bOnline ? 1 : -1;
                    return ((String) a.get("username")).compareToIgnoreCase((String) b.get("username"));
                })
                .collect(Collectors.toList());
    }

    public void removeFriend(String userId, String friendId) {
        friendStore.removeFriend(userId, friendId);
    }

    public boolean areFriends(String userId1, String userId2) {
        return friendStore.areFriends(userId1, userId2);
    }

    // ===== 방 초대 =====

    public RoomInvite sendRoomInvite(String fromUserId, String toUserId, String roomId) {
        if (!friendStore.areFriends(fromUserId, toUserId)) {
            throw new IllegalArgumentException("친구에게만 초대할 수 있습니다");
        }

        Room room = roomStore.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));
        if (!room.hasMember(fromUserId) || room.isMatchRoom())
            throw new org.springframework.security.access.AccessDeniedException("이 방에 초대할 권한이 없습니다");

        if (room.hasMember(toUserId)) {
            throw new IllegalArgumentException("이미 방에 있습니다");
        }

        if (friendStore.hasPendingInvite(toUserId, roomId)) {
            throw new IllegalArgumentException("이미 초대했습니다");
        }

        RoomInvite invite = new RoomInvite(fromUserId, toUserId, roomId);
        friendStore.saveInvite(invite);

        // 실시간 알림
        User fromUser = userStore.findById(fromUserId).orElse(null);
        messagingTemplate.convertAndSendToUser(toUserId, "/queue/friends",
                Map.of(
                        "type", "roomInvite",
                        "inviteId", invite.getId(),
                        "from", Map.of(
                                "id", fromUserId,
                                "username", fromUser != null ? fromUser.getUsername() : "Unknown"
                        ),
                        "room", Map.of(
                                "id", room.getId(),
                                "name", room.getName(),
                                "isMatchRoom", room.isMatchRoom()  // getType() 대신
                        ),
                        "expiresAt", invite.getExpiresAt().toString()
                ));

        return invite;
    }

    public List<Map<String, Object>> getPendingInvites(String userId) {
        return friendStore.findPendingInvitesTo(userId).stream()
                .map(i -> {
                    User from = userStore.findById(i.getFromUserId()).orElse(null);
                    Room room = roomStore.findById(i.getRoomId()).orElse(null);

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.getId());
                    map.put("from", Map.of(
                            "id", i.getFromUserId(),
                            "username", from != null ? from.getUsername() : "Unknown"
                    ));
                    map.put("room", room != null ? Map.of(
                            "id", room.getId(),
                            "name", room.getName()
                    ) : null);
                    map.put("expiresAt", i.getExpiresAt().toString());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String acceptInvite(String inviteId, String userId) {
        RoomInvite existing = friendStore.findInviteById(inviteId).orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다"));
        return execution.run(existing.getRoomId(), () -> acceptInviteLocked(inviteId, userId));
    }
    private String acceptInviteLocked(String inviteId, String userId) {
        access.active(userId);
        RoomInvite invite = friendStore.findInviteById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다"));

        if (!invite.getToUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("권한이 없습니다");
        }

        if (invite.isExpired()) {
            invite.expire();
            throw new IllegalArgumentException("초대가 만료되었습니다");
        }

        if (invite.getStatus() != RoomInvite.Status.PENDING) {
            throw new IllegalArgumentException("이미 처리된 초대입니다");
        }

        Room room = roomStore.findById(invite.getRoomId()).orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));
        if (room.isMatchRoom() || !room.hasMember(invite.getFromUserId())
                || !friendStore.areFriends(invite.getFromUserId(), userId))
            throw new org.springframework.security.access.AccessDeniedException("유효한 방 초대가 아닙니다");
        if (!room.hasMember(userId) && !room.addMember(userId)) throw new IllegalArgumentException("방이 가득 찼습니다");
        invite.accept();
        return invite.getRoomId();
    }

    public void rejectInvite(String inviteId, String userId) {
        RoomInvite invite = friendStore.findInviteById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다"));

        if (!invite.getToUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        invite.reject();
    }
}
