package com.achat.season;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface SeasonRepository extends JpaRepository<Season, Integer> {
    Optional<Season> findFirstByActiveTrueOrderByIdDesc();
    Optional<Season> findFirstByOrderByIdDesc();
    List<Season> findAllByOrderByIdAsc();
}
