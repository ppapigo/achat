package com.achat.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface BoardRepository extends JpaRepository<Board, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Board> {

}
