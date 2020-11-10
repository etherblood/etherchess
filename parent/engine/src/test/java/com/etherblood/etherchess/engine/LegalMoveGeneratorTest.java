package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.Square;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LegalMoveGeneratorTest {

    private Random random;
    private MirrorZobrist zobrist;

    @BeforeEach
    public void resetRandom() {
        random = new Random(7);
        zobrist = new MirrorZobrist(random::nextLong);
    }

    @Test
    public void kingAvoidsAttackedSquares() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "4k3/8/8/5r2/8/8/8/4K3 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertEquals(3, moves.size());
    }

    @Test
    public void kingAvoidsDiscoveredAttackedSquares() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "4k3/8/8/7r/8/7K/8/8 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertEquals(3, moves.size());
    }

    @Test
    public void captureOrBlockChecker() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "4K3/8/6N1/4r3/8/5k2/8/8 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.contains(Move.defaultMove(Piece.KNIGHT, Square.G6, Square.E7)));
        Assertions.assertTrue(moves.contains(Move.defaultMove(Piece.KNIGHT, Square.G6, Square.E5)));
    }

    @Test
    public void enPassantCapturesChecker() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "7k/8/8/3pP3/2K5/8/8/8 w - d6 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.contains(Move.enPassant(Square.E5, Square.D6)));
    }

    @Test
    public void enPassantBlocksCheck() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "4kq2/8/8/3pP3/1K6/8/8/8 w - d6 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.contains(Move.enPassant(Square.E5, Square.D6)));
    }

    @Test
    public void enPassantAlongPinRay() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "4kb2/8/8/2Pp4/8/K7/8/8 w - d6 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.contains(Move.enPassant(Square.C5, Square.D6)));
    }

    @Test
    public void enPassantOnFilePinRay() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "3rk3/8/8/2Pp4/8/3K4/8/8 w - d6 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.contains(Move.enPassant(Square.C5, Square.D6)));
    }

    @Test
    public void pinnedFile() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "4K3/4Q3/8/8/8/4r3/8/2k5 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertEquals(8, moves.size());
    }

    @Test
    public void pinnedRank() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "8/8/8/2r3QK/8/8/8/2k5 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertEquals(8, moves.size());
    }

    @Test
    public void pinnedDiagonal() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "8/8/7K/6Q1/8/4b3/8/2k5 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertEquals(6, moves.size());
    }

    @Test
    public void pinnedAntiDiagonal() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "8/8/K7/1Q6/8/3b4/8/2k5 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertEquals(6, moves.size());
    }

    @Test
    public void pinnedBishop() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "r6r/Rb1k2bq/8/8/8/8/7B/3K3R b - - 1 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.stream().noneMatch(move -> move.from == Square.B2));
    }

    @Test
    public void doubleMoveClearsPreviousEnPassantSquare() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "8/8/8/KPRp3r/5p1k/4P3/6P1/8 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.contains(Move.defaultMove(Piece.PAWN, Square.B5, Square.B6)));
    }

    @Test
    public void pinnedPawnNoMoves() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "rnbqkbnr/pp1ppppp/8/8/QPp5/2P5/P2PPPPP/RNB1KBNR b KQkq b3 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.stream().noneMatch(move -> move.from == Square.D2));
    }

    @Test
    public void pinnedEnPassant1() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "8/1ppr3k/8/1P1PpP2/4B3/8/3K4/8 w - e6 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.stream().noneMatch(move -> move.from == Square.D5 && move.to == Square.E6));
    }

    @Test
    public void pinnedEnPassant2() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "8/1p1rp2k/8/1PpP1P2/4B3/8/3K4/8 w - c6 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertTrue(moves.stream().noneMatch(move -> move.from == Square.D5 && move.to == Square.C6));
    }

    @Test
    public void check() {
        State state = new State(zobrist);
        new FenConverter().fromFen(state, "8/1pp1p2k/8/1P1r1P2/4B3/8/3K4/8 w - - 0 1");
        List<Move> moves = new ArrayList<>();
        LegalMoveGenerator instance = new LegalMoveGenerator();
        instance.generateLegalMoves(state, moves::add);
        Assertions.assertEquals(8, moves.size());
    }

}
