package com.achat.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface PostRepository extends JpaRepository<Post, String> {
    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);
    List<Post> findByAuthorIdInOrderByCreatedAtDesc(Set<String> authorIds, Pageable pageable);
    List<Post> findByAuthorIdInOrderByCreatedAtDescIdDesc(Set<String> authorIds, Pageable pageable);

    @Query("select p from Post p where p.authorId in :authors and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id)) order by p.createdAt desc, p.id desc")
    List<Post> findFeedBefore(@Param("authors") Set<String> authors, @Param("createdAt") Instant createdAt, @Param("id") String id, Pageable pageable);
}
