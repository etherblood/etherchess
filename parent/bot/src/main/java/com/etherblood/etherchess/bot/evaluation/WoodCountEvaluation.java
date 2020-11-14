package com.etherblood.etherchess.bot.evaluation;

import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.SquareSet;

public class WoodCountEvaluation implements Evaluation {

    public static final int KING_SCORE = 15_000;
    public static final int QUEEN_SCORE = 900;
    public static final int ROOK_SCORE = 500;
    public static final int BISHOP_SCORE = 310;
    public static final int KNIGHT_SCORE = 290;
    public static final int PAWN_SCORE = 100;

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

    public static int score(int piece) {
        switch (piece) {
            case Piece.EMPTY:
                return 0;
            case Piece.PAWN:
                return PAWN_SCORE;
            case Piece.KING:
                return KING_SCORE;
            case Piece.KNIGHT:
                return KNIGHT_SCORE;
            case Piece.BISHOP:
                return BISHOP_SCORE;
            case Piece.ROOK:
                return ROOK_SCORE;
            case Piece.QUEEN:
                return QUEEN_SCORE;
            default:
                throw new AssertionError(piece);
        }
    }
}
