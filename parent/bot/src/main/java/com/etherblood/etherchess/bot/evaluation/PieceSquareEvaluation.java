package com.etherblood.etherchess.bot.evaluation;

import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.util.Square;
import com.etherblood.etherchess.engine.util.SquareSet;

public class PieceSquareEvaluation implements Evaluation {
    private static final int[] PAWN_SCORES = {
            0, 0, 0, 0, 0, 0, 0, 0,
            100, 100, 100, 60, 60, 100, 100, 100,
            101, 102, 103, 90, 90, 103, 102, 101,
            102, 104, 106, 108, 108, 106, 104, 102,
            103, 106, 109, 112, 112, 109, 106, 103,
            104, 108, 112, 116, 116, 112, 108, 104,
            105, 110, 115, 120, 120, 115, 110, 105,
            0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final int[] KING_SCORES = {
            0, 20, 40, -20, 0, -20, 40, 20,
            -20, -20, -20, -20, -20, -20, -20, -20,
            -40, -40, -40, -40, -40, -40, -40, -40,
            -40, -40, -40, -40, -40, -40, -40, -40,
            -40, -40, -40, -40, -40, -40, -40, -40,
            -40, -40, -40, -40, -40, -40, -40, -40,
            -40, -40, -40, -40, -40, -40, -40, -40,
            -40, -40, -40, -40, -40, -40, -40, -40
    };
    private static final int[] KNIGHT_SCORES = {
            290, 270, 290, 290, 290, 290, 270, 290,
            290, 300, 300, 300, 300, 300, 300, 290,
            290, 300, 305, 305, 305, 305, 300, 290,
            290, 300, 305, 310, 310, 305, 300, 290,
            290, 300, 305, 310, 310, 305, 300, 290,
            290, 300, 305, 305, 305, 305, 300, 290,
            290, 300, 300, 300, 300, 300, 300, 290,
            290, 290, 290, 290, 290, 290, 290, 290
    };
    private static final int[] BISHOP_SCORES = {
            290, 290, 280, 290, 290, 280, 290, 290,
            290, 300, 300, 300, 300, 300, 300, 290,
            290, 300, 305, 305, 305, 305, 300, 290,
            290, 300, 305, 310, 310, 305, 300, 290,
            290, 300, 305, 310, 310, 305, 300, 290,
            290, 300, 305, 305, 305, 305, 300, 290,
            290, 300, 300, 300, 300, 300, 300, 290,
            290, 290, 290, 290, 290, 290, 290, 290
    };
    private static final int[] ROOK_SCORES = {
            490, 500, 500, 510, 510, 500, 500, 490,
            500, 500, 500, 500, 500, 500, 500, 500,
            500, 500, 500, 500, 500, 500, 500, 500,
            500, 500, 500, 500, 500, 500, 500, 500,
            500, 500, 500, 500, 500, 500, 500, 500,
            500, 500, 500, 500, 500, 500, 500, 500,
            515, 515, 515, 515, 515, 515, 515, 515,
            500, 500, 500, 500, 500, 500, 500, 500
    };
    private static final int[] QUEEN_SCORES = {
            890, 890, 890, 890, 890, 890, 890, 890,
            890, 900, 900, 900, 900, 900, 900, 890,
            890, 900, 905, 905, 905, 905, 900, 890,
            890, 900, 905, 910, 910, 905, 900, 890,
            890, 900, 905, 910, 910, 905, 900, 890,
            890, 900, 905, 905, 905, 905, 900, 890,
            890, 900, 900, 900, 900, 900, 900, 890,
            890, 890, 890, 890, 890, 890, 890, 890
    };

    @Override
    public int evaluate(State state) {
        int sum = 0;
        sum += score(PAWN_SCORES, state.pawns() & state.own());
        sum += score(KING_SCORES, state.kings() & state.own());
        sum += score(KNIGHT_SCORES, state.knights() & state.own());
        sum += score(BISHOP_SCORES, state.bishops() & state.own());
        sum += score(ROOK_SCORES, state.rooks() & state.own());
        sum += score(QUEEN_SCORES, state.queens() & state.own());

        sum -= score(PAWN_SCORES, SquareSet.mirrorY(state.pawns() & state.opp()));
        sum -= score(KING_SCORES, SquareSet.mirrorY(state.kings() & state.opp()));
        sum -= score(KNIGHT_SCORES, SquareSet.mirrorY(state.knights() & state.opp()));
        sum -= score(BISHOP_SCORES, SquareSet.mirrorY(state.bishops() & state.opp()));
        sum -= score(ROOK_SCORES, SquareSet.mirrorY(state.rooks() & state.opp()));
        sum -= score(QUEEN_SCORES, SquareSet.mirrorY(state.queens() & state.opp()));
        return sum;
    }

    private int score(int[] table, long squares) {
        int sum = 0;
        while (squares != 0) {
            int square = Square.firstOf(squares);
            sum += table[square];
            squares = SquareSet.clearFirst(squares);
        }
        return sum;
    }
}
