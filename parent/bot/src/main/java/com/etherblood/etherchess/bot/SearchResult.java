package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.engine.Move;

public interface SearchResult {

    void stats(SearchStats stats);

    void bestMove(Move move);

    void string(String string);
}
