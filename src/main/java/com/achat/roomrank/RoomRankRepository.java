package com.achat.roomrank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface RoomRankRepository extends JpaRepository<RoomRank, String> {

}
