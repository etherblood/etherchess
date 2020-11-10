package com.etherblood.etherchess.engine.util;

public class Castling {

    public static final int NONE = 0;
    public static final int A1 = 1;
    public static final int H1 = 2;
    public static final int A8 = 4;
    public static final int H8 = 8;

    public static final int OWN = A1 | H1;
    public static final int OPP = A8 | H8;

    public static int ofSquare(int square) {
        switch (square) {
            case Square.A1:
                return A1;
            case Square.H1:
                return H1;
            case Square.A8:
                return A8;
            case Square.H8:
                return H8;
            default:
                return NONE;
        }
    }

    public static int mirror(int availableCastlings) {
        return ((availableCastlings << 2) | (availableCastlings >>> 2)) & 15;
    }
}
