package com.etherblood.etherchess.uci;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SearchParams {
    private Integer depth;
    private Long nodes;
    private Integer mate;
    private Long millis;
    private List<String> searchMoves;
    private boolean infinite;
    private boolean ponder;
    private Long whiteClockMillis;
    private Long blackClockMillis;
    private Long whiteClockIncrement;
    private Long blackClockIncrement;
    private Integer movesToGo;
}
