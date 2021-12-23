package com.etherblood.etherchess.uci;

import java.util.List;

public record SearchParams(
        Integer depth,
        Long nodes,
        Integer mate,
        Long millis,
        List<String> searchMoves,
        boolean infinite,
        boolean ponder,
        Long whiteClockMillis,
        Long blackClockMillis,
        Long whiteClockIncrement,
        Long blackClockIncrement,
        Integer movesToGo
) {

    public static SearchParamsBuilder builder() {
        return new SearchParamsBuilder();
    }
}
