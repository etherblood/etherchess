package com.etherblood.etherchess.bot.evaluation;

import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.util.Square;
import com.etherblood.etherchess.engine.util.SquareSet;

public class PieceSquareEvaluation implements Evaluation {

    @Override
    public int evaluate(State state) {
        int sum = 0;
        sum += score(PieceSquareTable.PAWN_SCORES, state.pawns() & state.own());
        sum += score(PieceSquareTable.KING_SCORES, state.kings() & state.own());
        sum += score(PieceSquareTable.KNIGHT_SCORES, state.knights() & state.own());
        sum += score(PieceSquareTable.BISHOP_SCORES, state.bishops() & state.own());
        sum += score(PieceSquareTable.ROOK_SCORES, state.rooks() & state.own());
        sum += score(PieceSquareTable.QUEEN_SCORES, state.queens() & state.own());

        sum -= score(PieceSquareTable.PAWN_SCORES, SquareSet.mirrorY(state.pawns() & state.opp()));
        sum -= score(PieceSquareTable.KING_SCORES, SquareSet.mirrorY(state.kings() & state.opp()));
        sum -= score(PieceSquareTable.KNIGHT_SCORES, SquareSet.mirrorY(state.knights() & state.opp()));
        sum -= score(PieceSquareTable.BISHOP_SCORES, SquareSet.mirrorY(state.bishops() & state.opp()));
        sum -= score(PieceSquareTable.ROOK_SCORES, SquareSet.mirrorY(state.rooks() & state.opp()));
        sum -= score(PieceSquareTable.QUEEN_SCORES, SquareSet.mirrorY(state.queens() & state.opp()));
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
