package com.etherblood.etherchess.uci;

import java.util.List;

public record SearchStats(
        Integer depth,
        Integer seldepth,
        Integer scoreCp,
        Integer scoreMate,
        Integer hashPermill,
        Long nodes,
        Long millis,
        List<String> pv
) {

    public static SearchStatsBuilder builder() {
        return new SearchStatsBuilder();
    }
}
