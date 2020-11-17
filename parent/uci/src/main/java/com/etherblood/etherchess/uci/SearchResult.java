package com.etherblood.etherchess.uci;

public interface SearchResult {

    void stats(SearchStats stats);

    void bestMove(String lanMove);

    void string(String string);
}
