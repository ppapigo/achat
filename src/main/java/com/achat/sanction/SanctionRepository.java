package com.achat.sanction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface SanctionRepository extends JpaRepository<Sanction, String> {
    List<Sanction> findByUserIdOrderByIssuedAtDesc(String userId);
    List<Sanction> findAllByOrderByIssuedAtDesc();
}
