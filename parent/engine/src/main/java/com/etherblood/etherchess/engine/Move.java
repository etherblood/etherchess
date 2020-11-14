package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.util.Castling;
import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.PieceSquareSet;
import com.etherblood.etherchess.engine.util.Square;
import com.etherblood.etherchess.engine.util.SquareSet;
import java.util.Comparator;

public class Move {

    public static final int DEFAULT = 0;
    public static final int DOUBLE = 1;
    public static final int EN_PASSANT = 2;
    public static final int PROMOTION_QUEEN = 3;
    public static final int PROMOTION_ROOK = 4;
    public static final int PROMOTION_BISHOP = 5;
    public static final int PROMOTION_KNIGHT = 6;
    public static final int CASTLING = 7;

    public final int type;
    public final int piece;
    public final int from;
    public final int to;

    public Move(int type, int piece, int from, int to) {
        this.type = type;
        this.piece = piece;
        this.from = from;
        this.to = to;
        assert Square.isValid(from);
        assert Square.isValid(to);
        assert piece != Piece.EMPTY;
        assert Piece.isValid(piece);
        assert from != to;
    }

    public static Move unpack32(int raw) {
        return new Move((raw >>> 24) & 0xff, (raw >>> 16) & 0xff, (raw >>> 8) & 0xff, (raw >>> 0) & 0xff);
    }

    public static int pack32(Move move) {
        return (move.type << 24) | (move.piece << 16) | (move.from << 8) | (move.to << 0);
    }

    public static Move unpack16(State state, int raw) {
        assert (raw & 0xffff) == raw;
        int from = (raw >>> 6) & 0x7f;
        return new Move((raw >>> 12) & 0xf, state.getSquarePiece(from), from, (raw >>> 0) & 0x7f);
    }

    public static int pack16(State state, Move move) {
        return (move.type << 12) | (move.from << 6) | (move.to << 0);
    }

    public static Move defaultMove(int piece, int from, int to) {
        return new Move(Move.DEFAULT, piece, from, to);
    }

    public static Move pawnMove(int from, int to) {
        return new Move(Move.DEFAULT, Piece.PAWN, from, to);
    }

    public static Move enPassant(int from, int to) {
        return new Move(Move.EN_PASSANT, Piece.PAWN, from, to);
    }

    public static Move pawnDouble(int from, int to) {
        return new Move(Move.DOUBLE, Piece.PAWN, from, to);
    }

    public static Move promotion(int promotionType, int from, int to) {
        return new Move(promotionType, Piece.PAWN, from, to);
    }

    public void applyTo(State state) {
        assert state.getSquarePiece(from) == piece;
        assert (state.own() & SquareSet.of(to)) == 0;
        state.fiftyMovesCounter++;
        switch (type) {
            case DEFAULT:
                defaultMove(state);
                assert state.assertValid();
                break;
            case DOUBLE:
                doubleMove(state);
                assert state.assertValid();
                break;
            case EN_PASSANT:
                enPassant(state);
                assert state.assertValid();
                break;
            case PROMOTION_QUEEN:
                promotion(state, Piece.QUEEN);
                assert state.assertValid();
                break;
            case PROMOTION_ROOK:
                promotion(state, Piece.ROOK);
                assert state.assertValid();
                break;
            case PROMOTION_BISHOP:
                promotion(state, Piece.BISHOP);
                assert state.assertValid();
                break;
            case PROMOTION_KNIGHT:
                promotion(state, Piece.KNIGHT);
                assert state.assertValid();
                break;
            case CASTLING:
                castling(state);
                assert state.assertValid();
                break;
        }
        state.mirrorSides();
    }

    private void castling(State state) {
        assert piece == Piece.KING;
        assert from == Square.E1;

        state.togglePiece(true, piece, from, to);

        int rookFrom;
        int rookTo;
        switch (to) {
            case Square.C1: {
                rookFrom = Square.A1;
                rookTo = Square.D1;
                break;
            }
            case Square.G1: {
                rookFrom = Square.H1;
                rookTo = Square.F1;
                break;
            }
            default:
                throw new AssertionError(to);
        }
        state.togglePiece(true, Piece.ROOK, rookFrom, rookTo);

        state.fiftyMovesCounter = 0;
        state.enPassantSquare = 0;
        state.availableCastlings &= ~Castling.OWN;
    }

    private void doubleMove(State state) {
        assert piece == Piece.PAWN;

        state.enPassantSquare = 0;
        state.togglePiece(true, piece, from, to);
        state.fiftyMovesCounter = 0;
        long opponentPawnMask = (SquareSet.of(to - 1) | SquareSet.of(to + 1)) & SquareSet.RANK_4;
        long opponentPawns = state.opp() & state.pawns() & opponentPawnMask;
        int enPassantSquare = from + 8;
        int oppKingSquare = Square.firstOf(state.kings() & state.opp());
        while (opponentPawns != 0) {
            long oppPawn = SquareSet.firstOf(opponentPawns);
            opponentPawns ^= oppPawn;
            long occ = state.occupied() ^ (oppPawn | SquareSet.of(enPassantSquare) | SquareSet.of(to));
            long rookRays = PieceSquareSet.rookRays(oppKingSquare, occ);
            long bishopRays = PieceSquareSet.bishopRays(oppKingSquare, occ);
            if ((rookRays & (state.rooks() | state.queens()) & state.own()) != 0
                    || (bishopRays & (state.bishops() | state.queens()) & state.own()) != 0) {
                // en passant is pinned and can't be used
                continue;
            }
            state.enPassantSquare = enPassantSquare;
            break;
        }
    }

    private void enPassant(State state) {
        assert state.enPassantSquare == to;
        assert piece == Piece.PAWN;

        state.togglePiece(true, piece, from, to);
        state.togglePiece(false, Piece.PAWN, to - 8);
        state.fiftyMovesCounter = 0;
        state.enPassantSquare = 0;
    }

    private void promotion(State state, int promotion) {
        assert piece == Piece.PAWN;
        assert Square.y(to) == 7;

        int capture = state.getSquarePiece(to);
        if (capture != Piece.EMPTY) {
            state.togglePiece(false, capture, to);
            state.availableCastlings &= ~Castling.ofSquare(to);
        }
        state.togglePiece(true, piece, from);
        state.togglePiece(true, promotion, to);
        state.fiftyMovesCounter = 0;
        state.enPassantSquare = 0;
    }

    private void defaultMove(State state) {
        int capture = state.getSquarePiece(to);
        assert capture != Piece.KING;
        if (capture != Piece.EMPTY) {
            state.togglePiece(false, capture, to);
            state.availableCastlings &= ~Castling.ofSquare(to);
            state.fiftyMovesCounter = 0;
        }
        state.togglePiece(true, piece, from, to);
        switch (piece) {
            case Piece.PAWN: {
                state.fiftyMovesCounter = 0;
                break;
            }
            case Piece.KING: {
                if ((state.availableCastlings & Castling.OWN) != 0) {
                    state.availableCastlings &= ~Castling.OWN;
                    state.fiftyMovesCounter = 0;
                }
                break;
            }
            case Piece.ROOK: {
                int castling = Castling.ofSquare(from);
                if ((state.availableCastlings & castling) != 0) {
                    state.availableCastlings &= ~castling;
                    state.fiftyMovesCounter = 0;
                }
                break;
            }
        }
        state.enPassantSquare = 0;
    }

    @Override
    public String toString() {
        String s = Piece.toString(piece) + "(" + Square.toString(from) + "->" + Square.toString(to) + ")";
        switch (type) {
            case PROMOTION_QUEEN:
                s += Piece.toString(Piece.QUEEN);
                break;
            case PROMOTION_ROOK:
                s += Piece.toString(Piece.ROOK);
                break;
            case PROMOTION_BISHOP:
                s += Piece.toString(Piece.BISHOP);
                break;
            case PROMOTION_KNIGHT:
                s += Piece.toString(Piece.KNIGHT);
                break;
        }
        return s;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + this.type;
        hash = 29 * hash + this.piece;
        hash = 29 * hash + this.from;
        hash = 29 * hash + this.to;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Move)) {
            return false;
        }
        Move other = (Move) obj;
        return type == other.type && piece == other.piece && from == other.from && to == other.to;
    }

    public static Comparator<Move> defaultComparator() {
        Comparator<Move> comparator = Comparator.comparingInt(m -> m.piece);
        return comparator.thenComparingInt(m -> m.from)
                .thenComparingInt(m -> m.type)
                .thenComparingInt(m -> m.to);
    }
}
