package com.achat.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface PostRepository extends JpaRepository<Post, String> {
    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);
    List<Post> findByAuthorIdInOrderByCreatedAtDesc(Set<String> authorIds, Pageable pageable);
}
