package com.etherblood.etherchess.bot.evaluation;

import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.util.SquareSet;

public class WoodCountEvaluation implements Evaluation {

    private static final int QUEEN_SCORE = 900;
    private static final int ROOK_SCORE = 500;
    private static final int BISHOP_SCORE = 310;
    private static final int KNIGHT_SCORE = 290;
    private static final int PAWN_SCORE = 100;

    @Override
    public int evaluate(State state) {
        int sum = 0;
        sum += scores(state, state.own());
        sum -= scores(state, state.opp());
        return sum;
    }

    private int scores(State state, long owner) {
        int sum = 0;
        sum += QUEEN_SCORE * SquareSet.count(state.queens() & owner);
        sum += ROOK_SCORE * SquareSet.count(state.rooks() & owner);
        sum += BISHOP_SCORE * SquareSet.count(state.bishops() & owner);
        sum += KNIGHT_SCORE * SquareSet.count(state.knights() & owner);
        sum += PAWN_SCORE * SquareSet.count(state.pawns() & owner);
        return sum;
    }
}
