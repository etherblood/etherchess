package com.etherblood.etherchess.engine.util;

public class Piece {

    public static final int EMPTY = -1;
    public static final int PAWN = 0;
    public static final int KING = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK = 4;
    public static final int QUEEN = 5;

    public static boolean isValid(int piece) {
        return EMPTY <= piece && piece <= QUEEN;
    }

    public static String toString(int piece) {
        return Character.toString(toLowercaseCharacter(piece));
    }

    public static int fromCharacter(char character) {
        switch (character) {
            case 'P':
            case 'p':
                return PAWN;
            case 'K':
            case 'k':
                return KING;
            case 'N':
            case 'n':
                return KNIGHT;
            case 'B':
            case 'b':
                return BISHOP;
            case 'R':
            case 'r':
                return ROOK;
            case 'Q':
            case 'q':
                return QUEEN;
            default:
                return EMPTY;
        }
    }

    public static char toCharacter(int piece, boolean white) {
        char c = toLowercaseCharacter(piece);
        if (white) {
            c = Character.toUpperCase(c);
        }
        return c;
    }

    public static char toLowercaseCharacter(int piece) {
        assert isValid(piece);
        switch (piece) {
            case EMPTY:
                return ' ';
            case PAWN:
                return 'p';
            case KING:
                return 'k';
            case KNIGHT:
                return 'n';
            case BISHOP:
                return 'b';
            case ROOK:
                return 'r';
            case QUEEN:
                return 'q';
            default:
                throw new AssertionError(piece);
        }
    }
}
