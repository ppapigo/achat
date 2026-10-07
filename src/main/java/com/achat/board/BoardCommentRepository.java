package com.achat.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {
    List<BoardComment> findByBoardIdOrderByCreatedAtAsc(Long boardId);
    void deleteByBoardId(Long boardId);
}
