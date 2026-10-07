package com.achat.minihome;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface MiniHomePostRepository extends JpaRepository<MiniHomePost, String> {
    List<MiniHomePost> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);
}
