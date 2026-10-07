package com.achat;

import com.achat.auth.*;
import com.achat.auth.dto.*;
import com.achat.board.*;
import com.achat.board.dto.*;
import com.achat.card.ProfileCardService;
import com.achat.feed.*;
import com.achat.friend.*;
import com.achat.history.*;
import com.achat.home.*;
import com.achat.job.*;
import com.achat.job.dto.SelectJobRequest;
import com.achat.like.*;
import com.achat.message.*;
import com.achat.message.dto.LikeRequest;
import com.achat.minihome.*;
import com.achat.post.*;
import com.achat.report.*;
import com.achat.room.*;
import com.achat.roomrank.*;
import com.achat.sanction.*;
import com.achat.season.*;
import com.achat.shop.*;
import com.achat.social.InteractionStore;
import com.achat.user.*;
import com.achat.websocket.ChatController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** No test-level transaction: each service call commits and subsequent reads use a new context. */
@SpringBootTest
class JpaPersistenceTests {
    @Autowired UserStore users;
    @Autowired AuthService auth;
    @Autowired AuthController authController;
    @Autowired VerificationCodeStore codes;
    @Autowired RoomStore rooms;
    @Autowired RoomService roomService;
    @Autowired MessageStore messages;
    @Autowired MessageHistoryController messageHistory;
    @Autowired ChatController chat;
    @Autowired ShopService shop;
    @Autowired HomeStore homes;
    @Autowired HomeService homeService;
    @Autowired BoardService boards;
    @Autowired BoardRepository boardRepository;
    @Autowired BoardCommentRepository boardComments;
    @Autowired ContentLikeRepository contentLikes;
    @Autowired FriendStore friends;
    @Autowired FriendService friendService;
    @Autowired PostStore posts;
    @Autowired LikeStore likes;
    @Autowired FeedCommentStore feedComments;
    @Autowired MiniHomeService miniHome;
    @Autowired ProfileCardService cards;
    @Autowired JobService jobs;
    @Autowired JobStore jobStore;
    @Autowired SeasonService seasons;
    @Autowired SeasonStore seasonStore;
    @Autowired SanctionService sanctions;
    @Autowired ReportStore reports;
    @Autowired RoomRankStore roomRanks;
    @Autowired InteractionStore interactions;
    @Autowired GameHistoryStore history;
    @Autowired TransactionTemplate tx;

    private User newUser() {
        User user = new User(UUID.randomUUID() + "@example.com", "password-hash");
        users.save(user);
        return user;
    }

    @Test
    void verificationAndProfileChangesSurviveSeparateTransactions() {
        String email = UUID.randomUUID() + "@Example.com";
        String id = auth.register(new RegisterRequest(email, "password123!")).userId();
        auth.verifyEmail(new VerifyCodeRequest(email, codes.getValidCode(email).orElseThrow()));
        assertThat(users.findById(id).orElseThrow().isEmailVerified()).isTrue();
        assertThat(auth.login(new LoginRequest(email.toUpperCase(), "password123!")).userId()).isEqualTo(id);
        String oldName = users.findById(id).orElseThrow().getUsername();
        String newName = "nick-" + UUID.randomUUID();
        authController.updateProfile(id, Map.of("username", newName));
        assertThat(users.findByUsername(newName.toUpperCase()).orElseThrow().getId()).isEqualTo(id);
        assertThat(users.findByUsername(oldName)).isEmpty();
        assertThatThrownBy(() -> users.save(new User(email.toLowerCase(), "other")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void purchasesStackConsumptionAndEquipmentPersist() {
        String userId = newUser().getId();
        assertThat(shop.purchase(userId, "reroll_ticket").success()).isTrue();
        assertThat(shop.purchase(userId, "reroll_ticket").success()).isTrue();
        assertThat(users.findById(userId).orElseThrow().getCoins()).isEqualTo(900);
        assertThat(shop.getInventory(userId)).hasSize(1);
        assertThat(shop.getInventory(userId).get(0).getQuantity()).isEqualTo(2);
        assertThat(shop.useItem(userId, "reroll_ticket").success()).isTrue();
        assertThat(shop.getInventory(userId).get(0).getQuantity()).isEqualTo(1);
        assertThat(shop.purchase(userId, "title_newbie").success()).isTrue();
        assertThat(shop.purchase(userId, "title_newbie").success()).isFalse();
        assertThat(shop.equip(userId, "title_newbie").success()).isTrue();
        assertThat(shop.getEquipment(userId).getEquipped(EquipSlot.TITLE)).isEqualTo("title_newbie");
        assertThat(homes.findByUserId(userId).orElseThrow().getTitleBadge()).isEqualTo("Newbie");
        assertThat(shop.unequip(userId, EquipSlot.TITLE).success()).isTrue();
        assertThat(shop.getEquipment(userId).getAllEquipped()).isEmpty();
        assertThat(homes.findByUserId(userId).orElseThrow().getTitleBadge()).isNull();
    }

    @Test
    void failedTransactionRollsBackBothCoinsAndInventory() {
        String userId = newUser().getId();
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            shop.purchase(userId, "title_newbie");
            throw new IllegalStateException("simulated failure after item grant");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(users.findById(userId).orElseThrow().getCoins()).isEqualTo(1000);
        assertThat(shop.getInventory(userId)).isEmpty();
    }

    @Test
    void boardCommentsLikesVisibilityAndGeneratedIdsPersist() {
        String author = newUser().getId();
        Long boardId = boards.createBoard(author, new BoardCreateRequest(BoardCategory.FREE, "JPA 100%", "long enough content")).id();
        Long parentId = boards.createComment(boardId, author, new CommentCreateRequest(null, "parent")).id();
        boards.createComment(boardId, author, new CommentCreateRequest(parentId, "reply"));
        assertThat(boards.toggleBoardLike(boardId, author)).isEqualTo(1);
        var detail = boards.getBoard(boardId, author);
        assertThat(detail.liked()).isTrue();
        assertThat(detail.comments()).hasSize(1);
        assertThat(detail.comments().get(0).replies()).hasSize(1);
        assertThat(boardRepository.findById(boardId).orElseThrow().getViewCount()).isEqualTo(1);
        assertThat(boards.getBoards(BoardCategory.FREE, "100%", 0, 20)).extracting(BoardResponse::id).contains(boardId);
        boards.hideBoard(boardId, "review");
        assertThat(boards.getBoards(null, "JPA 100%", 0, 20)).extracting(BoardResponse::id).doesNotContain(boardId);
        boards.unhideBoard(boardId);
        boards.updateComment(boardId, parentId, author, new CommentUpdateRequest("edited"));
        assertThat(boardComments.findById(parentId).orElseThrow().getContent()).isEqualTo("edited");
        boards.deleteComment(boardId, parentId, author);
        assertThat(boardComments.findByBoardIdOrderByCreatedAtAsc(boardId)).isEmpty();
        assertThat(boardRepository.findById(boardId).orElseThrow().getCommentCount()).isZero();
        boards.deleteBoard(boardId, author);
        assertThat(boardRepository.findById(boardId)).isEmpty();
        assertThat(contentLikes.countByTargetTypeAndTargetId(ContentLike.TargetType.BOARD, boardId.toString())).isZero();
        assertThat(boards.createBoard(author, new BoardCreateRequest(BoardCategory.FREE, "next", "long enough content")).id()).isGreaterThan(boardId);
    }

    @Test
    void roomMembersSubRoomsAndMessageLikesPersist() {
        String owner = newUser().getId();
        String guest = newUser().getId();
        Room room = new Room("test room", owner, 10);
        rooms.save(room);
        roomService.joinRoom(room.getId(), guest);
        String childId = roomService.createSubRoom(room.getId(), "child", owner).id();
        assertThat(rooms.findById(room.getId()).orElseThrow().getSubRoomIds()).contains(childId);
        assertThat(rooms.findById(childId).orElseThrow().getMemberIds()).containsExactlyInAnyOrder(owner, guest);
        Message message = new Message(room.getId(), owner, null, "hello", "hello", null);
        messages.save(message);
        chat.toggleLike(room.getId(), new LikeRequest(message.getId()), () -> guest);
        assertThat(messages.findById(message.getId()).orElseThrow().isLikedBy(guest)).isTrue();
        assertThat(messages.findByRoomId(room.getId())).extracting(Message::getId).contains(message.getId());
        roomService.deleteSubRoom(room.getId(), childId, owner);
        roomService.leaveRoom(room.getId(), guest);
        assertThat(rooms.findById(room.getId()).orElseThrow().getMemberIds()).doesNotContain(guest);
        assertThat(rooms.findById(childId)).isEmpty();
    }

    @Test
    void recentChatHistoryIsBoundedOrderedAndRestrictedToRoomMembers() {
        String owner = newUser().getId();
        Room room = new Room("history room", owner, 10);
        rooms.save(room);
        Message first = new Message(room.getId(), owner, null, "first", "first", null);
        messages.save(first);
        Message second = new Message(room.getId(), owner, null, "second", "second", null);
        messages.save(second);
        messages.save(new Message("different-room", owner, null, "private", "private", null));
        assertThat(messageHistory.recentMessages(room.getId(), owner, 1).getBody().data())
                .extracting(com.achat.message.dto.MessageResponse::id).containsExactly(second.getId());
        assertThat(messageHistory.recentMessages(room.getId(), owner, 200).getBody().data())
                .extracting(com.achat.message.dto.MessageResponse::id).containsExactly(first.getId(), second.getId());
        assertThat(messageHistory.recentMessages(room.getId(), "outsider", 100).getStatusCode().value()).isEqualTo(403);
        assertThat(messageHistory.recentMessages("missing-room", owner, 100).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void homeLayoutVisitsAndJobStatePersist() {
        String userId = newUser().getId();
        homeService.getMyHome(userId, "nickname");
        tx.executeWithoutResult(status -> homes.findOrCreate(userId).updateFurniturePosition("bed", 23, 45));
        homeService.getHome(userId, "visitor", "nickname");
        Home home = homes.findByUserId(userId).orElseThrow();
        assertThat(home.getVisitCount()).isEqualTo(1);
        assertThat(home.getFurnitureLayout().get("bed").getX()).isEqualTo(23);
        jobs.selectJob(userId, new SelectJobRequest("GUARDIAN"));
        jobs.addExp(userId, 50);
        assertThat(jobStore.find(userId).orElseThrow().getCurrentJob()).isEqualTo(JobType.GUARDIAN);
        assertThat(jobStore.find(userId).orElseThrow().getExp()).isEqualTo(50);
    }

    @Test
    void friendRequestsInvitesAndSymmetricRelationshipsPersist() {
        String first = newUser().getId();
        String second = newUser().getId();
        FriendRequest request = new FriendRequest(first, second);
        friends.saveRequest(request);
        friendService.acceptRequest(request.getId(), second);
        assertThat(friends.findRequestById(request.getId()).orElseThrow().getStatus()).isEqualTo(FriendRequest.Status.ACCEPTED);
        assertThat(friends.getFriendIds(first)).contains(second);
        assertThat(friends.getFriendIds(second)).contains(first);
        RoomInvite invite = new RoomInvite(first, second, "room");
        friends.saveInvite(invite);
        assertThat(friends.hasPendingInvite(second, "room")).isTrue();
        assertThat(friendService.acceptInvite(invite.getId(), second)).isEqualTo("room");
        assertThat(friends.findPendingInvitesTo(second)).isEmpty();
        friends.removeFriend(first, second);
        assertThat(friends.areFriends(first, second)).isFalse();
        assertThat(friends.areFriends(second, first)).isFalse();
    }

    @Test
    void feedUpdatesDoNotDuplicatePostsAndDeletionPreservesPostingLimits() {
        String author = newUser().getId();
        Post post = new Post(author, PostType.FREE, "feed content");
        posts.save(post);
        tx.executeWithoutResult(status -> {
            Post stored = posts.findById(post.getId()).orElseThrow();
            stored.setContent("edited feed");
            stored.setTags(java.util.List.of("first", "second"));
            posts.save(stored);
        });
        assertThat(posts.findByAuthorId(author, 20)).hasSize(1);
        assertThat(posts.getDailyCount(author)).isEqualTo(1);
        assertThat(posts.findById(post.getId()).orElseThrow().getTags()).containsExactly("first", "second");
        likes.add(post.getId(), author);
        feedComments.save(new FeedComment(post.getId(), author, "comment"));
        assertThat(likes.count(post.getId())).isEqualTo(1);
        assertThat(feedComments.countByPostId(post.getId())).isEqualTo(1);
        posts.delete(post.getId());
        assertThat(likes.count(post.getId())).isZero();
        assertThat(feedComments.countByPostId(post.getId())).isZero();
        assertThat(posts.getDailyCount(author)).isEqualTo(1);
        assertThat(posts.getLastPostTime(author)).isPresent();
    }

    @Test
    void miniHomePostDeletionRemovesItsFeedCopyAndLikes() {
        String author = newUser().getId();
        String postId = miniHome.createPost(author, "mini home content").getId();
        assertThat(posts.findById(postId)).isPresent();
        assertThat(miniHome.toggleLike(postId, author)).isTrue();
        assertThat(miniHome.getPostsByUser(author, 10).get(0).getLikeCount()).isEqualTo(1);
        assertThat(miniHome.isLiked(postId, author)).isTrue();
        miniHome.deletePost(postId, author);
        assertThat(posts.findById(postId)).isEmpty();
        assertThat(miniHome.getPostsByUser(author, 10)).isEmpty();
        assertThat(miniHome.isLiked(postId, author)).isFalse();
    }

    @Test
    void cardDrawLimitPersists() {
        String owner = newUser().getId();
        assertThat(cards.drawCard(owner).success()).isTrue();
        assertThat(cards.hasDrawnToday(owner)).isTrue();
        assertThat(cards.getMyCards(owner)).hasSize(1);
        assertThat(cards.drawCard(owner).alreadyDrawnToday()).isTrue();
    }

    @Test
    void sanctionsReportsInteractionsRankingAndHistoryPersist() {
        String userId = newUser().getId();
        String otherId = newUser().getId();
        var sanction = sanctions.applySanction(userId, SanctionType.MUTE, "test", null, "SYSTEM", 60);
        assertThat(sanctions.canChat(userId)).isFalse();
        sanctions.revokeSanction(sanction.getId(), "SYSTEM");
        assertThat(sanctions.canChat(userId)).isTrue();
        Report report = new Report(otherId, ReportType.USER, userId, ReportReason.SPAM, "test report", null);
        reports.save(report);
        reports.save(reports.findById(report.getId()).orElseThrow());
        assertThat(reports.getUserReportCount(userId)).isEqualTo(1);
        assertThat(reports.hasRecentReport(otherId, userId)).isTrue();
        interactions.record(userId, otherId);
        assertThat(interactions.getRecent(userId, 7)).contains(otherId);
        assertThat(interactions.getRecent(otherId, 7)).contains(userId);
        tx.executeWithoutResult(status -> roomRanks.getOrCreate("test-rank").recordActivity());
        assertThat(roomRanks.findByRoomId("test-rank").orElseThrow().getLastActivityAt()).isNotNull();
        history.save(new GameHistory(userId, "room", "name", true, 4, 1, 100, 10, 20, 50, 10, null));
        assertThat(history.findRecentByUserId(userId, 5)).hasSize(1);
        assertThat(history.calculateStats(userId).getWins()).isEqualTo(1);
    }

    @Test
    void seasonInitializationDoesNotResetExistingSeasonAndRotationPersists() {
        String userId = newUser().getId();
        int currentId = seasonStore.getCurrentSeason().getId();
        seasons.initializeFirstSeason();
        assertThat(seasonStore.getCurrentSeason().getId()).isEqualTo(currentId);
        var result = seasons.endCurrentSeason();
        assertThat(seasonStore.findById(currentId).orElseThrow().isActive()).isFalse();
        assertThat(seasonStore.getCurrentSeason().getId()).isEqualTo(result.newSeasonId());
        assertThat(seasonStore.getUserHistory(userId)).hasSize(1);
        assertThat(users.findById(userId).orElseThrow().getSeasonId()).isEqualTo(result.newSeasonId());
    }
}
