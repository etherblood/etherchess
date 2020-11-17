package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.uci.SearchResult;
import com.etherblood.etherchess.uci.SearchStats;

public class NoopSearchResult implements SearchResult {
    @Override
    public void stats(SearchStats stats) {

    }

    @Override
    public void bestMove(String lanMove) {

    }

    @Override
    public void string(String string) {

    }
}
