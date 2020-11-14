package com.etherblood.etherchess.bot.evaluation;

import com.etherblood.etherchess.engine.State;

public interface Evaluation {
    int evaluate(State state);
}
