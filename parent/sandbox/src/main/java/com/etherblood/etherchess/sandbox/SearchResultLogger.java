package com.etherblood.etherchess.sandbox;


import com.etherblood.etherchess.uci.SearchResult;
import com.etherblood.etherchess.uci.SearchStats;

public class SearchResultLogger implements SearchResult {
    @Override
    public void stats(SearchStats stats) {
        System.out.println("depth: " + stats.depth() + " (seldepth: " + (stats.seldepth()) + ")");
        System.out.println("score: " + stats.scoreCp());
        System.out.println(stats.nodes() + " nodes in " + stats.millis() + " ms (" + Math.round((double) stats.nodes() / stats.millis()) + "knps)");
        System.out.println("branching: " + Math.log(stats.nodes()) / Math.log(stats.depth()));
        System.out.println();
    }

    @Override
    public void bestMove(String move) {
        System.out.println(move);
        System.out.println();
    }

    @Override
    public void string(String string) {
        System.out.println(string);
        System.out.println();
    }
}
