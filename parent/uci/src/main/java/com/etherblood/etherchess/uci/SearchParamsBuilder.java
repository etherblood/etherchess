package com.etherblood.etherchess.uci;

import java.util.List;

public class SearchParamsBuilder {
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

    public SearchParamsBuilder depth(Integer depth) {
        this.depth = depth;
        return this;
    }

    public SearchParamsBuilder nodes(Long nodes) {
        this.nodes = nodes;
        return this;
    }

    public SearchParamsBuilder mate(Integer mate) {
        this.mate = mate;
        return this;
    }

    public SearchParamsBuilder millis(Long millis) {
        this.millis = millis;
        return this;
    }

    public SearchParamsBuilder searchMoves(List<String> searchMoves) {
        this.searchMoves = searchMoves;
        return this;
    }

    public SearchParamsBuilder infinite(boolean infinite) {
        this.infinite = infinite;
        return this;
    }

    public SearchParamsBuilder ponder(boolean ponder) {
        this.ponder = ponder;
        return this;
    }

    public SearchParamsBuilder whiteClockMillis(Long whiteClockMillis) {
        this.whiteClockMillis = whiteClockMillis;
        return this;
    }

    public SearchParamsBuilder nlackClockMillis(Long blackClockMillis) {
        this.blackClockMillis = blackClockMillis;
        return this;
    }

    public SearchParamsBuilder whiteClockIncrement(Long whiteClockIncrement) {
        this.whiteClockIncrement = whiteClockIncrement;
        return this;
    }

    public SearchParamsBuilder blackClockIncrement(Long blackClockIncrement) {
        this.blackClockIncrement = blackClockIncrement;
        return this;
    }

    public SearchParamsBuilder movesToGo(Integer movesToGo) {
        this.movesToGo = movesToGo;
        return this;
    }

    public SearchParams build() {
        return new SearchParams(
                depth,
                nodes,
                mate,
                millis,
                searchMoves,
                infinite,
                ponder,
                whiteClockMillis,
                blackClockMillis,
                whiteClockIncrement,
                blackClockIncrement,
                movesToGo
        );
    }
}
