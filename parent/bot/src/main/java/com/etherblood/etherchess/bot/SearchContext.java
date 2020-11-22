package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.State;
import java.util.List;

public class SearchContext {
    public State state;
    public HashHistory history;

    public int depth;
    public int alpha;
    public int beta;
    public boolean isRootNode;
    public boolean isPvNode;

    public Move hashMove;
    public List<Move> moves;

    public Move bestMove;
    public int bounds;

    public int ply() {
        return history.size();
    }
}
