package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.bot.evaluation.PieceSquareTable;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.State;
import java.util.Comparator;

public class SimpleMoveComparator implements Comparator<Move> {

    private final State state;
    private final Move hashMove;

    public SimpleMoveComparator(State state, Move hashMove) {
        this.state = state;
        this.hashMove = hashMove;
    }

    @Override
    public int compare(Move a, Move b) {
        if (a.equals(hashMove)) {
            return -1;
        }
        if (b.equals(hashMove)) {
            return 1;
        }
        int captureA = state.getSquarePiece(a.to);
        int captureB = state.getSquarePiece(b.to);
        if (captureA != captureB) {
            return -Integer.compare(
                    PieceSquareTable.score(captureA, a.to),
                    PieceSquareTable.score(captureB, b.to));
        }
        int pieceA = state.getSquarePiece(a.from);
        int pieceB = state.getSquarePiece(b.from);
        if (pieceA != pieceB) {
            return Integer.compare(
                    PieceSquareTable.score(pieceA, a.from),
                    PieceSquareTable.score(pieceB, b.from));
        }
        return -Integer.compare(
                PieceSquareTable.score(pieceA, a.to) - PieceSquareTable.score(pieceA, a.from),
                PieceSquareTable.score(pieceB, b.to) - PieceSquareTable.score(pieceB, b.from));
    }
}
