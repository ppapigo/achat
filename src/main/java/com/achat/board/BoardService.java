package com.achat.board;

import com.achat.board.dto.*;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final Map<Long, Board> boards = new ConcurrentHashMap<>();
    private final Map<Long, List<BoardComment>> commentsByBoard = new ConcurrentHashMap<>();
    private final Set<String> boardLikes = ConcurrentHashMap.newKeySet();
    private final AtomicLong boardIdGen = new AtomicLong(1);
    private final AtomicLong commentIdGen = new AtomicLong(1);

    private final UserStore userStore;

    public BoardService(UserStore userStore) {
        this.userStore = userStore;
        initMockData();
    }

    private void initMockData() {
        createMockBoard("mock-1", "운영자", BoardCategory.NOTICE,
                "A.chat 오픈 안내", "안녕하세요! A.chat이 정식 오픈했습니다. 많은 이용 부탁드려요!");
        createMockBoard("mock-2", "초보자123", BoardCategory.QUESTION,
                "증강 시스템이 뭔가요?", "처음 시작했는데 증강이 뭔지 모르겠어요. 설명 부탁드립니다!");
        createMockBoard("mock-3", "프로게이머", BoardCategory.GUIDE,
                "[공략] 협상가 전직 완벽 가이드", "협상가 전직의 모든 스킬과 활용법을 정리했습니다.");
        createMockBoard("mock-4", "자유인", BoardCategory.FREE,
                "오늘 채팅방에서 생긴 일", "ㅋㅋㅋ 오늘 방에서 진짜 웃긴 일 있었는데요...");
        createMockBoard("mock-5", "상인왕", BoardCategory.TRADE,
                "[거래] 레어 칭호 교환 원해요", "희귀 칭호 있으신 분 교환하실 분 구합니다!");
    }

    private void createMockBoard(String authorId, String nickname, BoardCategory category,
                                 String title, String content) {
        Long id = boardIdGen.getAndIncrement();
        Board board = new Board(id, authorId, nickname, category, title, content);
        for (int i = 0; i < (int) (Math.random() * 100); i++) board.incrementViewCount();
        for (int i = 0; i < (int) (Math.random() * 20); i++) board.incrementLikeCount();
        boards.put(id, board);
        commentsByBoard.put(id, new ArrayList<>());

        if (Math.random() > 0.3) {
            addMockComment(id, "댓글러", "좋은 글이네요!");
        }
        if (Math.random() > 0.5) {
            addMockComment(id, "유저A", "도움이 됐어요 감사합니다 👍");
        }
    }

    private void addMockComment(Long boardId, String nickname, String content) {
        Long id = commentIdGen.getAndIncrement();
        BoardComment comment = new BoardComment(id, boardId, "mock-user", nickname, content);
        commentsByBoard.computeIfAbsent(boardId, k -> new ArrayList<>()).add(comment);
        boards.get(boardId).incrementCommentCount();
    }

    private String getNickname(String userId) {
        return userStore.findById(userId)
                .map(User::getUsername)
                .orElse("익명");
    }

    // 키워드 매칭 (제목 + 내용)
    private boolean matchesKeyword(Board board, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase().trim();
        return board.getTitle().toLowerCase().contains(lowerKeyword) ||
                board.getContent().toLowerCase().contains(lowerKeyword);
    }

    // ✅ 게시글 목록 (숨김 제외)
    public List<BoardResponse> getBoards(BoardCategory category, String keyword, int page, int size) {
        return boards.values().stream()
                .filter(b -> !b.isHidden())  // ✅ 숨김 제외
                .filter(b -> category == null || b.getCategory() == category)
                .filter(b -> matchesKeyword(b, keyword))
                .sorted(Comparator.comparing(Board::getCreatedAt).reversed())
                .skip((long) page * size)
                .limit(size)
                .map(BoardResponse::from)
                .collect(Collectors.toList());
    }

    // ✅ 총 게시글 수 (숨김 제외)
    public int getTotalCount(BoardCategory category, String keyword) {
        return (int) boards.values().stream()
                .filter(b -> !b.isHidden())  // ✅ 숨김 제외
                .filter(b -> category == null || b.getCategory() == category)
                .filter(b -> matchesKeyword(b, keyword))
                .count();
    }

    // ✅ 게시글 상세 (숨김 체크)
    public BoardDetailResponse getBoard(Long boardId, String userId) {
        Board board = boards.get(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        // ✅ 숨김 처리된 게시글
        if (board.isHidden()) {
            throw new IllegalArgumentException("신고로 인해 숨김 처리된 게시글입니다.");
        }

        board.incrementViewCount();

        // ✅ 댓글 (숨김 제외)
        List<BoardComment> allComments = commentsByBoard.getOrDefault(boardId, List.of())
                .stream()
                .filter(c -> !c.isHidden())  // ✅ 숨김 댓글 제외
                .toList();
        List<CommentResponse> comments = buildCommentTree(allComments);

        boolean liked = userId != null && boardLikes.contains(boardId + ":" + userId);

        return BoardDetailResponse.from(board, comments, liked);
    }

    // 댓글 계층 구조 빌드
    private List<CommentResponse> buildCommentTree(List<BoardComment> allComments) {
        List<BoardComment> parentComments = allComments.stream()
                .filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparing(BoardComment::getCreatedAt))
                .toList();

        return parentComments.stream()
                .map(parent -> {
                    List<CommentResponse> replies = allComments.stream()
                            .filter(c -> parent.getId().equals(c.getParentId()))
                            .sorted(Comparator.comparing(BoardComment::getCreatedAt))
                            .map(CommentResponse::from)
                            .toList();
                    return CommentResponse.from(parent, replies);
                })
                .toList();
    }

    // 게시글 작성
    public BoardResponse createBoard(String userId, BoardCreateRequest request) {
        String nickname = getNickname(userId);

        Long id = boardIdGen.getAndIncrement();
        Board board = new Board(id, userId, nickname,
                request.category(), request.title(), request.content());

        boards.put(id, board);
        commentsByBoard.put(id, new ArrayList<>());

        return BoardResponse.from(board);
    }

    // 게시글 수정
    public BoardResponse updateBoard(Long boardId, String userId, BoardUpdateRequest request) {
        Board board = boards.get(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        if (!board.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("본인의 글만 수정할 수 있습니다.");
        }

        board.update(request.category(), request.title(), request.content());

        return BoardResponse.from(board);
    }

    // 게시글 삭제
    public void deleteBoard(Long boardId, String userId) {
        Board board = boards.get(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        if (!board.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("본인의 글만 삭제할 수 있습니다.");
        }

        boards.remove(boardId);
        commentsByBoard.remove(boardId);
        boardLikes.removeIf(key -> key.startsWith(boardId + ":"));
    }

    // 좋아요
    public int toggleBoardLike(Long boardId, String userId) {
        Board board = boards.get(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        String key = boardId + ":" + userId;
        if (boardLikes.contains(key)) {
            boardLikes.remove(key);
            board.decrementLikeCount();
        } else {
            boardLikes.add(key);
            board.incrementLikeCount();
        }

        return board.getLikeCount();
    }

    // 댓글 작성
    public CommentResponse createComment(Long boardId, String userId, CommentCreateRequest request) {
        Board board = boards.get(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        if (request.parentId() != null) {
            boolean parentExists = commentsByBoard.getOrDefault(boardId, List.of())
                    .stream()
                    .anyMatch(c -> c.getId().equals(request.parentId()) && c.getParentId() == null);
            if (!parentExists) {
                throw new IllegalArgumentException("부모 댓글을 찾을 수 없습니다.");
            }
        }

        String nickname = getNickname(userId);

        Long id = commentIdGen.getAndIncrement();
        BoardComment comment = new BoardComment(id, boardId, request.parentId(), userId, nickname, request.content());

        commentsByBoard.computeIfAbsent(boardId, k -> new ArrayList<>()).add(comment);
        board.incrementCommentCount();

        return CommentResponse.from(comment);
    }

    // 댓글 수정
    public CommentResponse updateComment(Long boardId, Long commentId, String userId, CommentUpdateRequest request) {
        Board board = boards.get(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        List<BoardComment> comments = commentsByBoard.get(boardId);
        if (comments == null) {
            throw new IllegalArgumentException("댓글을 찾을 수 없습니다.");
        }

        BoardComment comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("본인의 댓글만 수정할 수 있습니다.");
        }

        comment.updateContent(request.content());

        return CommentResponse.from(comment);
    }

    // 댓글 삭제
    public void deleteComment(Long boardId, Long commentId, String userId) {
        Board board = boards.get(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        List<BoardComment> comments = commentsByBoard.get(boardId);
        if (comments == null) {
            throw new IllegalArgumentException("댓글을 찾을 수 없습니다.");
        }

        BoardComment comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("본인의 댓글만 삭제할 수 있습니다.");
        }

        long replyCount = comments.stream()
                .filter(c -> commentId.equals(c.getParentId()))
                .count();

        comments.removeIf(c -> c.getId().equals(commentId) || commentId.equals(c.getParentId()));

        for (int i = 0; i < 1 + replyCount; i++) {
            board.decrementCommentCount();
        }
    }

    // ✅ 게시글 숨김 처리 (신고 시스템에서 호출)
    public void hideBoard(Long boardId, String reason) {
        Board board = boards.get(boardId);
        if (board != null) {
            board.hide(reason);
            System.out.println("🙈 게시글 숨김: " + boardId + " - " + reason);
        }
    }

    // ✅ 게시글 숨김 해제 (관리자용)
    public void unhideBoard(Long boardId) {
        Board board = boards.get(boardId);
        if (board != null) {
            board.unhide();
            System.out.println("👁️ 게시글 숨김 해제: " + boardId);
        }
    }

    // ✅ 댓글 숨김 처리 (신고 시스템에서 호출)
    public void hideComment(Long commentId, String reason) {
        for (List<BoardComment> comments : commentsByBoard.values()) {
            for (BoardComment comment : comments) {
                if (comment.getId().equals(commentId)) {
                    comment.hide(reason);
                    System.out.println("🙈 댓글 숨김: " + commentId + " - " + reason);
                    return;
                }
            }
        }
    }

    // ✅ 댓글 숨김 해제 (관리자용)
    public void unhideComment(Long commentId) {
        for (List<BoardComment> comments : commentsByBoard.values()) {
            for (BoardComment comment : comments) {
                if (comment.getId().equals(commentId)) {
                    comment.unhide();
                    System.out.println("👁️ 댓글 숨김 해제: " + commentId);
                    return;
                }
            }
        }
    }

    // ✅ 숨김된 게시글 목록 (관리자용)
    public List<BoardResponse> getHiddenBoards() {
        return boards.values().stream()
                .filter(Board::isHidden)
                .sorted(Comparator.comparing(Board::getUpdatedAt).reversed())
                .map(BoardResponse::from)
                .collect(Collectors.toList());
    }
}