package com.etherblood.etherchess.uci;

import java.util.List;

public class SearchStatsBuilder {
    private Integer depth;
    private Integer seldepth;
    private Integer scoreCp;
    private Integer scoreMate;
    private Integer hashPermill;
    private Long nodes;
    private Long millis;
    private List<String> pv;

    public SearchStatsBuilder depth(Integer depth) {
        this.depth = depth;
        return this;
    }

    public SearchStatsBuilder seldepth(Integer seldepth) {
        this.seldepth = seldepth;
        return this;
    }

    public SearchStatsBuilder scoreCp(Integer scoreCp) {
        this.scoreCp = scoreCp;
        return this;
    }

    public SearchStatsBuilder scoreMate(Integer scoreMate) {
        this.scoreMate = scoreMate;
        return this;
    }

    public SearchStatsBuilder hashPermill(Integer hashPermill) {
        this.hashPermill = hashPermill;
        return this;
    }

    public SearchStatsBuilder nodes(Long nodes) {
        this.nodes = nodes;
        return this;
    }

    public SearchStatsBuilder millis(Long millis) {
        this.millis = millis;
        return this;
    }

    public SearchStatsBuilder pv(List<String> pv) {
        this.pv = pv;
        return this;
    }

    public SearchStats build() {
        return new SearchStats(
                depth,
                seldepth,
                scoreCp,
                scoreMate,
                hashPermill,
                nodes,
                millis,
                pv
        );
    }
}
