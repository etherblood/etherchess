package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.engine.State;

public interface Evaluation {
    int evaluate(State state);
}
