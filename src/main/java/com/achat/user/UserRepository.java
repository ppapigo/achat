package com.achat.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameKey(String usernameKey);
    boolean existsByEmail(String email);
    boolean existsByUsernameKey(String usernameKey);
    List<User> findByUsernameContainingIgnoreCase(String keyword);
}
