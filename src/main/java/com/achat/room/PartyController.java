package com.achat.room;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.common.ApiResponse;
import com.achat.friend.FriendStore;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Transactional
@RequestMapping("/api/party")
@RequiredArgsConstructor
public class PartyController {

    private final RoomStore roomStore;
    private final UserStore userStore;
    private final FriendStore friendStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.achat.common.RoomExecution execution;
    private final RoomService roomService;

    /**
     * 기존 파티에 친구 초대
     */
    @PostMapping("/{partyId}/invite")
    public ResponseEntity<ApiResponse<?>> inviteToParty(
            @AuthenticationPrincipal String userId,
            @PathVariable String partyId,
            @RequestBody Map<String, String> request) {
        return execution.run(partyId, () -> inviteLocked(userId, partyId, request));
    }
    private ResponseEntity<ApiResponse<?>> inviteLocked(String userId, String partyId, Map<String, String> request) {

        String friendId = request.get("friendId");

        if (friendId == null || friendId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("초대할 친구를 선택하세요"));
        }

        Room party = roomStore.findById(partyId).orElse(null);
        if (party == null || party.getType() != RoomType.PARTY) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("파티를 찾을 수 없습니다"));
        }

        if (!party.hasMember(userId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("파티 멤버만 초대할 수 있습니다"));
        }

        if (!friendStore.areFriends(userId, friendId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("친구만 초대할 수 있습니다"));
        }

        if (party.hasMember(friendId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이미 파티에 있습니다"));
        }

        if (!party.addMember(friendId)) return ResponseEntity.badRequest().body(ApiResponse.error("파티 정원이 가득 찼습니다"));

        // 초대된 친구에게 알림
        User inviter = userStore.findById(userId).orElse(null);
        String inviterName = inviter != null ? inviter.getUsername() : "Unknown";

        messagingTemplate.convertAndSendToUser(friendId, "/queue/friends",
                Map.of(
                        "type", "partyInvited",
                        "partyId", party.getId(),
                        "partyName", party.getName(),
                        "from", Map.of(
                                "id", userId,
                                "username", inviterName
                        )
                ));

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "초대했습니다",
                "partyName", party.getName()
        )));
    }
    /**
     * 파티방 생성 - 친구들 바로 추가 (카톡 방식)
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createParty(
            @AuthenticationPrincipal String userId,
            @RequestBody CreatePartyRequest request) {

        if (request.friendIds() == null || request.friendIds().isEmpty() || request.friendIds().size() > 49
                || (request.name() != null && request.name().length() > 100)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("초대할 친구를 선택하세요"));
        }

        // 친구 관계 확인
        for (String friendId : request.friendIds()) {
            if (!friendStore.areFriends(userId, friendId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("친구만 초대할 수 있습니다"));
            }
        }

        // 파티 이름 생성 (없으면 자동 생성)
        String partyName = request.name();
        if (partyName == null || partyName.isBlank()) {
            // 참여자 이름으로 자동 생성
            User creator = userStore.findById(userId).orElse(null);
            String creatorName = creator != null ? creator.getUsername() : "Unknown";

            if (request.friendIds().size() == 1) {
                User friend = userStore.findById(request.friendIds().get(0)).orElse(null);
                String friendName = friend != null ? friend.getUsername() : "Unknown";
                partyName = creatorName + ", " + friendName;
            } else {
                partyName = creatorName + " 외 " + request.friendIds().size() + "명";
            }
        }

        // 파티방 생성
        int maxMembers = request.friendIds().size() + 1;
        Room party = new Room(partyName, userId, maxMembers, RoomType.PARTY);

        // 친구들 바로 멤버로 추가!
        for (String friendId : request.friendIds()) {
            party.addMember(friendId);
        }

        roomStore.save(party);

        // 친구들에게 알림 (바로 입장됨을 알림)
        User creator = userStore.findById(userId).orElse(null);
        String creatorUsername = creator != null ? creator.getUsername() : "Unknown";

        for (String friendId : request.friendIds()) {
            messagingTemplate.convertAndSendToUser(friendId, "/queue/friends",
                    Map.of(
                            "type", "partyCreated",
                            "partyId", party.getId(),
                            "partyName", party.getName(),
                            "from", Map.of(
                                    "id", userId,
                                    "username", creatorUsername
                            )
                    ));
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "partyId", party.getId(),
                "name", party.getName(),
                "memberCount", party.getMemberIds().size()
        )));
    }

    /**
     * 내 파티 목록
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> myParties(
            @AuthenticationPrincipal String userId) {

        List<Map<String, Object>> parties = roomStore.findAll().stream()
                .filter(r -> r.getType() == RoomType.PARTY)
                .filter(r -> r.hasMember(userId))
                .map(r -> {
                    // 멤버 이름 목록
                    List<String> memberNames = r.getMemberIds().stream()
                            .map(id -> userStore.findById(id)
                                    .map(User::getUsername)
                                    .orElse("Unknown"))
                            .toList();

                    return Map.<String, Object>of(
                            "id", r.getId(),
                            "name", r.getName(),
                            "memberCount", r.getMemberIds().size(),
                            "memberNames", memberNames
                    );
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(parties));
    }

    /**
     * 파티 나가기
     */
    @PostMapping("/{partyId}/leave")
    public ResponseEntity<ApiResponse<?>> leaveParty(
            @AuthenticationPrincipal String userId,
            @PathVariable String partyId) {

        Room party = roomStore.findById(partyId).orElse(null);
        if (party == null || party.getType() != RoomType.PARTY) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("파티를 찾을 수 없습니다"));
        }

        roomService.leaveRoom(partyId, userId);

        return ResponseEntity.ok(ApiResponse.ok("파티를 나갔습니다"));
    }

    public record CreatePartyRequest(String name, List<String> friendIds) {}
}
