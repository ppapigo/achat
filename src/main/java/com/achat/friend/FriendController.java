package com.achat.friend;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import com.achat.user.UserStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserStore userStore;

    // ===== 친구 요청 =====

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<?>> sendRequest(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("username이 필요합니다"));
        }

        try {
            FriendRequest request = friendService.sendRequest(userId, username);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "requestId", request.getId()
            )));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingRequests(
            @AuthenticationPrincipal String userId) {

        return ResponseEntity.ok(ApiResponse.ok(
                friendService.getPendingRequests(userId)
        ));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<?>> acceptRequest(
            @AuthenticationPrincipal String userId,
            @PathVariable String requestId) {

        try {
            friendService.acceptRequest(requestId, userId);
            return ResponseEntity.ok(ApiResponse.ok("수락했습니다"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<?>> rejectRequest(
            @AuthenticationPrincipal String userId,
            @PathVariable String requestId) {

        try {
            friendService.rejectRequest(requestId, userId);
            return ResponseEntity.ok(ApiResponse.ok("거절했습니다"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== 친구 목록 =====

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFriends(
            @AuthenticationPrincipal String userId) {

        return ResponseEntity.ok(ApiResponse.ok(
                friendService.getFriendList(userId)
        ));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<?>> removeFriend(
            @AuthenticationPrincipal String userId,
            @PathVariable String friendId) {

        friendService.removeFriend(userId, friendId);
        return ResponseEntity.ok(ApiResponse.ok("삭제했습니다"));
    }

    // ===== 유저 검색 =====

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchUsers(
            @AuthenticationPrincipal String userId,
            @RequestParam String q) {

        List<Map<String, Object>> results = userStore.searchByUsername(q).stream()
                .filter(u -> !u.getId().equals(userId))
                .limit(10)
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "isFriend", friendService.areFriends(userId, u.getId())
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    // ===== 방 초대 =====

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<?>> sendRoomInvite(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> body) {

        String friendId = body.get("friendId");
        String roomId = body.get("roomId");

        if (friendId == null || roomId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("friendId와 roomId가 필요합니다"));
        }

        try {
            RoomInvite invite = friendService.sendRoomInvite(userId, friendId, roomId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "inviteId", invite.getId(),
                    "expiresAt", invite.getExpiresAt().toString()
            )));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/invites")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingInvites(
            @AuthenticationPrincipal String userId) {

        return ResponseEntity.ok(ApiResponse.ok(
                friendService.getPendingInvites(userId)
        ));
    }

    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<ApiResponse<?>> acceptInvite(
            @AuthenticationPrincipal String userId,
            @PathVariable String inviteId) {

        try {
            String roomId = friendService.acceptInvite(inviteId, userId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "roomId", roomId
            )));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/invites/{inviteId}/reject")
    public ResponseEntity<ApiResponse<?>> rejectInvite(
            @AuthenticationPrincipal String userId,
            @PathVariable String inviteId) {

        try {
            friendService.rejectInvite(inviteId, userId);
            return ResponseEntity.ok(ApiResponse.ok("거절했습니다"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
