package com.achat.augment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AugmentAwardRepository extends JpaRepository<AugmentAward, String> {
    @Query("select a.userId, sum(a.points) from AugmentAward a where a.gameId = :gameId group by a.userId")
    List<Object[]> totals(String gameId);
}
