package com.achat.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface UserJobStateRepository extends JpaRepository<UserJobState, String> {

}
