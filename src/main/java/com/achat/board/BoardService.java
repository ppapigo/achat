package com.achat.board;

import com.achat.board.dto.*;
import com.achat.like.ContentLike;
import com.achat.like.ContentLikeRepository;
import com.achat.user.User;
import com.achat.user.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final BoardCommentRepository commentRepository;
    private final ContentLikeRepository likeRepository;
    private final UserStore userStore;

    @Transactional(readOnly = true)
    public List<BoardResponse> getBoards(BoardCategory category, String keyword, int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 100 || (keyword != null && keyword.length() > 100))
            throw new IllegalArgumentException("조회 범위를 확인해 주세요");
        return boardRepository.findAll(search(category, keyword), PageRequest.of(page, size,
                Sort.by("createdAt").descending())).stream().map(BoardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public int getTotalCount(BoardCategory category, String keyword) {
        return Math.toIntExact(boardRepository.count(search(category, keyword)));
    }

    private Specification<Board> search(BoardCategory category, String keyword) {
        return (root, query, cb) -> {
            var predicate = cb.isFalse(root.get("hidden"));
            if (category != null) predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicate = cb.and(predicate, cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("content")), pattern, '\\')));
            }
            return predicate;
        };
    }

    public BoardDetailResponse getBoard(Long boardId, String userId) {
        Board board = requireBoard(boardId);
        if (board.isHidden()) throw new IllegalArgumentException("신고로 인해 숨김 처리된 게시글입니다.");
        board.incrementViewCount();
        List<BoardComment> visible = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId)
                .stream().filter(c -> !c.isHidden()).toList();
        List<CommentResponse> comments = visible.stream().filter(c -> c.getParentId() == null)
                .map(parent -> CommentResponse.from(parent, visible.stream()
                        .filter(c -> parent.getId().equals(c.getParentId())).map(CommentResponse::from).toList())).toList();
        boolean liked = userId != null && likeRepository.existsByTargetTypeAndTargetIdAndUserId(
                ContentLike.TargetType.BOARD, boardId.toString(), userId);
        return BoardDetailResponse.from(board, comments, liked);
    }

    public BoardResponse createBoard(String userId, BoardCreateRequest request) {
        Board board = boardRepository.save(new Board(null, userId, nickname(userId),
                request.category(), request.title(), request.content()));
        return BoardResponse.from(board);
    }

    public BoardResponse updateBoard(Long boardId, String userId, BoardUpdateRequest request) {
        Board board = requireBoard(boardId);
        requireAuthor(board.getAuthorId(), userId, "본인의 글만 수정할 수 있습니다.");
        board.update(request.category(), request.title(), request.content());
        return BoardResponse.from(board);
    }

    public void deleteBoard(Long boardId, String userId) {
        Board board = requireBoard(boardId);
        requireAuthor(board.getAuthorId(), userId, "본인의 글만 삭제할 수 있습니다.");
        commentRepository.deleteByBoardId(boardId);
        likeRepository.deleteByTargetTypeAndTargetId(ContentLike.TargetType.BOARD, boardId.toString());
        boardRepository.delete(board);
    }

    public int toggleBoardLike(Long boardId, String userId) {
        Board board = requireBoard(boardId);
        var like = likeRepository.findByTargetTypeAndTargetIdAndUserId(ContentLike.TargetType.BOARD, boardId.toString(), userId);
        if (like.isPresent()) {
            likeRepository.delete(like.get());
            board.decrementLikeCount();
        } else {
            likeRepository.save(new ContentLike(ContentLike.TargetType.BOARD, boardId.toString(), userId));
            board.incrementLikeCount();
        }
        return board.getLikeCount();
    }

    public CommentResponse createComment(Long boardId, String userId, CommentCreateRequest request) {
        Board board = requireBoard(boardId);
        if (request.parentId() != null && requireComment(boardId, request.parentId()).getParentId() != null) {
            throw new IllegalArgumentException("부모 댓글을 찾을 수 없습니다.");
        }
        BoardComment comment = commentRepository.save(new BoardComment(null, boardId, request.parentId(),
                userId, nickname(userId), request.content()));
        board.incrementCommentCount();
        return CommentResponse.from(comment);
    }

    public CommentResponse updateComment(Long boardId, Long commentId, String userId, CommentUpdateRequest request) {
        requireBoard(boardId);
        BoardComment comment = requireComment(boardId, commentId);
        requireAuthor(comment.getAuthorId(), userId, "본인의 댓글만 수정할 수 있습니다.");
        comment.updateContent(request.content());
        return CommentResponse.from(comment);
    }

    public void deleteComment(Long boardId, Long commentId, String userId) {
        Board board = requireBoard(boardId);
        BoardComment comment = requireComment(boardId, commentId);
        requireAuthor(comment.getAuthorId(), userId, "본인의 댓글만 삭제할 수 있습니다.");
        List<BoardComment> deleted = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
                .filter(c -> c.getId().equals(commentId) || commentId.equals(c.getParentId())).toList();
        commentRepository.deleteAll(deleted);
        deleted.forEach(c -> board.decrementCommentCount());
    }

    public void hideBoard(Long boardId, String reason) { boardRepository.findById(boardId).ifPresent(b -> b.hide(reason)); }
    public void unhideBoard(Long boardId) { boardRepository.findById(boardId).ifPresent(Board::unhide); }
    public void hideComment(Long commentId, String reason) { commentRepository.findById(commentId).ifPresent(c -> c.hide(reason)); }
    public void unhideComment(Long commentId) { commentRepository.findById(commentId).ifPresent(BoardComment::unhide); }

    @Transactional(readOnly = true)
    public List<BoardResponse> getHiddenBoards() {
        return boardRepository.findAll((root, query, cb) -> cb.isTrue(root.get("hidden")),
                Sort.by("updatedAt").descending()).stream().map(BoardResponse::from).toList();
    }

    private Board requireBoard(Long id) {
        return boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }
    private BoardComment requireComment(Long boardId, Long id) {
        return commentRepository.findById(id).filter(c -> c.getBoardId().equals(boardId))
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    }
    private String nickname(String userId) { return userStore.findById(userId).map(User::getUsername).orElse("익명"); }
    private void requireAuthor(String authorId, String userId, String message) {
        if (!authorId.equals(userId)) throw new org.springframework.security.access.AccessDeniedException(message);
    }
}
