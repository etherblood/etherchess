package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.util.Castling;
import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.Square;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoveTest {

    private Random random;
    private MirrorZobrist zobrist;

    @BeforeEach
    public void resetRandom() {
        random = new Random(7);
        zobrist = new MirrorZobrist(random::nextLong);
    }

    @Test
    public void pinnedEnPassant() {
        // enPassantSquare must not be set if en passant move is not possible for whatever reason
        // a set enPassant square would result in a different hash, breaking repetition detection

        State state = new State(zobrist);
        new FenConverter().fromFen(state, "2k5/3p4/8/K1P4r/8/8/8/8 b - - 0 1");

        Move move = Move.pawnDouble(Square.D2, Square.D4);
        move.apply(state);
        Assertions.assertEquals(0, state.enPassantSquare, "Illegal en passant square due to pin.");
    }

    @Test
    public void noAttackerThenNoEnPassant() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "8/8/8/2k2pP1/8/p2K4/1P6/8 w - f4 0 1");

        Move move = Move.pawnDouble(Square.B2, Square.B4);
        move.apply(state);
        Assertions.assertEquals(0, state.enPassantSquare);
    }

    @Test
    public void castlingFlagsUpdatedAfterLargeCastling() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "r3k2r/p6p/8/B7/1pp1p3/3b4/P6P/R3K2R w KQkq -");
        MoveGenerator.LARGE_CASTLING.apply(state);
        Assertions.assertEquals(Castling.OWN, state.availableCastlings);
    }

    @Test
    public void castlingFlagsUpdatedAfterRookCaptureWithRook() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        Move move = Move.defaultMove(Piece.ROOK, Square.A1, Square.A8);
        move.apply(state);
        Assertions.assertEquals(Castling.H1 | Castling.H8, state.availableCastlings);
    }

    @Test
    public void castlingFlagsUpdatedAfterRookCaptureWithPromotion() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "r3k2r/6P1/8/8/8/8/8/R3K2R w KQkq - 0 1");
        Move move = Move.promotion(Piece.ROOK, Square.G7, Square.H8);
        move.apply(state);
        Assertions.assertEquals(Castling.A1 | Castling.A8 | Castling.H8, state.availableCastlings);
    }

}
