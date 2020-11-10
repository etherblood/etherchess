package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.util.Castling;
import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.Square;
import com.etherblood.etherchess.engine.util.SquareSet;

public class FenConverter {

    public static final String DEFAULT_STARTPOSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public String toFen(State state) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            if (i != 0) {
                builder.append('/');
            }
            int y;
            if (state.isWhite) {
                y = 7 - i;
            } else {
                y = i;
            }
            int empty = 0;
            for (int x = 0; x < 8; x++) {
                int square = Square.square(x, y);
                long squareSet = SquareSet.of(square);
                int piece = state.getSquarePiece(square);
                if (piece == Piece.EMPTY) {
                    empty++;
                } else {
                    if (empty != 0) {
                        builder.append(empty);
                        empty = 0;
                    }
                    builder.append(Piece.toCharacter(piece, ((state.own() & squareSet) != 0) == state.isWhite));
                }
            }
            if (empty != 0) {
                builder.append(empty);
            }
        }
        builder.append(' ');
        if (state.isWhite) {
            builder.append('w');
        } else {
            builder.append('b');
        }
        builder.append(' ');
        int castling = state.availableCastlings;
        if (!state.isWhite) {
            castling = Castling.mirror(castling);
        }
        if (castling == 0) {
            builder.append('-');
        } else {
            if ((castling & Castling.H1) != 0) {
                builder.append('K');
            }
            if ((castling & Castling.A1) != 0) {
                builder.append('Q');
            }
            if ((castling & Castling.H8) != 0) {
                builder.append('K');
            }
            if ((castling & Castling.A8) != 0) {
                builder.append('Q');
            }
        }
        builder.append(' ');
        int enPassant = state.enPassantSquare;
        if (!state.isWhite && enPassant != 0) {
            enPassant = Square.mirrorY(enPassant);
        }
        if (enPassant != 0) {
            builder.append(Square.toString(enPassant));
        } else {
            builder.append('-');
        }
        builder.append(' ');
        builder.append(state.fiftyMovesCounter);
        builder.append(' ');
        builder.append(1);// TODO: correct ply count
        return builder.toString();
    }

    public void fromFen(State state, String fen) {
        state.clear();

        String[] parts = fen.split(" ");
        fenPieces(state, parts[0].split("/"));
        fenCastling(state, parts[2]);
        fenPassant(state, parts[3]);
        if (parts.length >= 5) {
            state.fiftyMovesCounter = Byte.parseByte(parts[4]);
        }
        fenPlayer(state, parts[1]);
        assert state.assertValid();
    }

    private void fenPassant(State state, String string) {
        if (string.length() == 2) {
            state.enPassantSquare = Square.square(string.charAt(0) - 'a', string.charAt(1) - '1');
        }
    }

    private void fenCastling(State state, String string) {
        int castleIndex = 0;
        if (string.contains("k")) {
            castleIndex |= Castling.H8;
        }
        if (string.contains("K")) {
            castleIndex |= Castling.H1;
        }
        if (string.contains("q")) {
            castleIndex |= Castling.A8;
        }
        if (string.contains("Q")) {
            castleIndex |= Castling.A1;
        }
        state.availableCastlings = castleIndex;
    }

    private void fenPlayer(State state, String part) {
        if (part.equals("b")) {
            state.mirrorSides();
        }
    }

    private void fenPieces(State state, String[] rows) {
        for (int y = 0; y < 8; y++) {
            String row = rows[7 - y];
            int x = 0;
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                int square = Square.square(x, y);
                switch (c) {
                    case 'p':
                        state.togglePiece(false, Piece.PAWN, square);
                        break;
                    case 'k':
                        state.togglePiece(false, Piece.KING, square);
                        break;
                    case 'b':
                        state.togglePiece(false, Piece.BISHOP, square);
                        break;
                    case 'n':
                        state.togglePiece(false, Piece.KNIGHT, square);
                        break;
                    case 'r':
                        state.togglePiece(false, Piece.ROOK, square);
                        break;
                    case 'q':
                        state.togglePiece(false, Piece.QUEEN, square);
                        break;
                    case 'P':
                        state.togglePiece(true, Piece.PAWN, square);
                        break;
                    case 'K':
                        state.togglePiece(true, Piece.KING, square);
                        break;
                    case 'B':
                        state.togglePiece(true, Piece.BISHOP, square);
                        break;
                    case 'N':
                        state.togglePiece(true, Piece.KNIGHT, square);
                        break;
                    case 'R':
                        state.togglePiece(true, Piece.ROOK, square);
                        break;
                    case 'Q':
                        state.togglePiece(true, Piece.QUEEN, square);
                        break;
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                        x += c - '1';
                        break;
                }
                x++;
            }
        }
    }
}
