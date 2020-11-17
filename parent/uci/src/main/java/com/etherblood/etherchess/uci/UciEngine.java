package com.etherblood.etherchess.uci;

import java.util.List;

public interface UciEngine {

    String getName();

    String getAuthor();

    void newGame();

    void setPosition(String fen, List<String> moves);

    void think(SearchParams params, SearchResult result);

    void setTableSize(int mib);

    void setDebug(boolean value);
}
