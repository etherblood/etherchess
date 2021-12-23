package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.util.Castling;
import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.Square;
import com.etherblood.etherchess.engine.util.SquareSet;

public class State {

    public final static int NO_EN_PASSANT = 0;

    public final MirrorZobrist zobrist;

    private long own;
    private long opp;
    private long pawns;
    private long kings;
    private long knights;
    private long bishops;
    private long rooks;
    private long queens;

    public int availableCastlings;
    public int enPassantSquare;
    public int fiftyMovesCounter;
    public boolean isWhite;

    private long piecesHash;

    public State(MirrorZobrist zobrist) {
        this.zobrist = zobrist;
    }

    public void clear() {
        own = 0;
        opp = 0;
        pawns = 0;
        kings = 0;
        knights = 0;
        bishops = 0;
        rooks = 0;
        queens = 0;
        piecesHash = 0;
        availableCastlings = Castling.OWN | Castling.OPP;
        enPassantSquare = NO_EN_PASSANT;
        fiftyMovesCounter = 0;
        isWhite = true;
    }

    public void togglePiece(boolean isOwn, int piece, int square) {
        long squareSet = SquareSet.of(square);
        if (isOwn) {
            own ^= squareSet;
        } else {
            opp ^= squareSet;
        }
        togglePieceSquareSet(piece, squareSet);
        piecesHash ^= zobrist.pieceHash(isOwn, piece, square);
    }

    public void togglePiece(boolean isOwn, int piece, int from, int to) {
        assert from != to;
        long squareSet = SquareSet.of(from) ^ SquareSet.of(to);
        if (isOwn) {
            own ^= squareSet;
        } else {
            opp ^= squareSet;
        }
        togglePieceSquareSet(piece, squareSet);
        piecesHash ^= zobrist.pieceHash(isOwn, piece, from) ^ zobrist.pieceHash(isOwn, piece, to);
    }

    public int getSquarePiece(int square) {
        long squares = SquareSet.of(square);
        for (int piece = Piece.PAWN; piece <= Piece.QUEEN; piece++) {
            if ((getPieceSquareSet(piece) & squares) != 0) {
                return piece;
            }
        }
        return Piece.EMPTY;
    }

    public void copyFrom(State other) {
        assert other.assertValid();
        own = other.own;
        opp = other.opp;
        pawns = other.pawns;
        kings = other.kings;
        knights = other.knights;
        bishops = other.bishops;
        rooks = other.rooks;
        queens = other.queens;

        availableCastlings = other.availableCastlings;
        enPassantSquare = other.enPassantSquare;
        fiftyMovesCounter = other.fiftyMovesCounter;
        isWhite = other.isWhite;

        piecesHash = other.piecesHash;
        assert assertValid();
    }

    public void mirrorSides() {
        assert assertValid();
        long tmp = own;
        own = SquareSet.mirrorY(opp);
        opp = SquareSet.mirrorY(tmp);
        pawns = SquareSet.mirrorY(pawns);
        kings = SquareSet.mirrorY(kings);
        knights = SquareSet.mirrorY(knights);
        bishops = SquareSet.mirrorY(bishops);
        rooks = SquareSet.mirrorY(rooks);
        queens = SquareSet.mirrorY(queens);

        availableCastlings = Castling.mirrorY(availableCastlings);
        if (enPassantSquare != NO_EN_PASSANT) {
            enPassantSquare = Square.mirrorY(enPassantSquare);
        }
        isWhite = !isWhite;
        piecesHash = MirrorZobrist.mirror(piecesHash);
        assert assertValid();
    }

    public long occupied() {
        return own | opp;
    }

    public long hash() {
        return piecesHash ^ zobrist.metaHash(enPassantSquare, availableCastlings);
    }

    private long getPieceSquareSet(int piece) {
        return switch (piece) {
            case Piece.PAWN -> pawns;
            case Piece.KING -> kings;
            case Piece.KNIGHT -> knights;
            case Piece.BISHOP -> bishops;
            case Piece.ROOK -> rooks;
            case Piece.QUEEN -> queens;
            default -> throw new AssertionError(piece);
        };
    }

    private void togglePieceSquareSet(int piece, long squareSet) {
        switch (piece) {
            case Piece.PAWN -> pawns ^= squareSet;
            case Piece.KING -> kings ^= squareSet;
            case Piece.KNIGHT -> knights ^= squareSet;
            case Piece.BISHOP -> bishops ^= squareSet;
            case Piece.ROOK -> rooks ^= squareSet;
            case Piece.QUEEN -> queens ^= squareSet;
            default -> throw new AssertionError(piece);
        }
    }

    public boolean assertValid() {
        assert SquareSet.count(own & kings) == 1;
        assert SquareSet.count(opp & kings) == 1;
        assert (own & opp) == 0;
        assert (own | opp) == (pawns | kings | knights | bishops | rooks | queens);
        assert (own ^ opp) == (pawns ^ kings ^ knights ^ bishops ^ rooks ^ queens);
        assert enPassantSquare == NO_EN_PASSANT || Square.isValid(enPassantSquare);
        long expectedPiecesHash = 0;
        for (int square = 0; square < 64; square++) {
            int piece = getSquarePiece(square);
            if (piece != Piece.EMPTY) {
                expectedPiecesHash ^= zobrist.pieceHash((SquareSet.of(square) & own) != 0, piece, square);
            }
        }
        assert expectedPiecesHash == piecesHash;
        return true;
    }

    public String toBoardString() {
        StringBuilder builder = new StringBuilder();
        for (int y = 7; y >= 0; y--) {
            for (int x = 0; x < 8; x++) {
                int square = Square.square(x, y);
                long single = SquareSet.of(square);
                int piece = getSquarePiece(square);
                boolean isOwn = (single & own) != 0;
                builder.append(piece == Piece.EMPTY ? '.' : Piece.toCharacter(piece, isWhite == isOwn));
                if (x < 7) {
                    builder.append(' ');
                }
            }
            if (y != 0) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    public long own() {
        return own;
    }

    public long opp() {
        return opp;
    }

    public long pawns() {
        return pawns;
    }

    public long kings() {
        return kings;
    }

    public long knights() {
        return knights;
    }

    public long bishops() {
        return bishops;
    }

    public long rooks() {
        return rooks;
    }

    public long queens() {
        return queens;
    }

}
