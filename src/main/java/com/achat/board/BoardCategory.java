package com.achat.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum BoardCategory {
    FREE("자유"),
    GUIDE("공략"),
    QUESTION("질문"),
    TRADE("거래"),
    NOTICE("공지");

    private final String displayName;
}
