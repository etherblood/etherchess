package com.etherblood.etherchess.engine.util;

public class Square {

    public static final int A1 = 0;
    public static final int B1 = 1;
    public static final int C1 = 2;
    public static final int D1 = 3;
    public static final int E1 = 4;
    public static final int F1 = 5;
    public static final int G1 = 6;
    public static final int H1 = 7;

    public static final int A2 = 8;
    public static final int B2 = 9;
    public static final int C2 = 10;
    public static final int D2 = 11;
    public static final int E2 = 12;
    public static final int F2 = 13;
    public static final int G2 = 14;
    public static final int H2 = 15;

    public static final int A3 = 16;
    public static final int B3 = 17;
    public static final int C3 = 18;
    public static final int D3 = 19;
    public static final int E3 = 20;
    public static final int F3 = 21;
    public static final int G3 = 22;
    public static final int H3 = 23;

    public static final int A4 = 24;
    public static final int B4 = 25;
    public static final int C4 = 26;
    public static final int D4 = 27;
    public static final int E4 = 28;
    public static final int F4 = 29;
    public static final int G4 = 30;
    public static final int H4 = 31;

    public static final int A5 = 32;
    public static final int B5 = 33;
    public static final int C5 = 34;
    public static final int D5 = 35;
    public static final int E5 = 36;
    public static final int F5 = 37;
    public static final int G5 = 38;
    public static final int H5 = 39;

    public static final int A6 = 40;
    public static final int B6 = 41;
    public static final int C6 = 42;
    public static final int D6 = 43;
    public static final int E6 = 44;
    public static final int F6 = 45;
    public static final int G6 = 46;
    public static final int H6 = 47;

    public static final int A7 = 48;
    public static final int B7 = 49;
    public static final int C7 = 50;
    public static final int D7 = 51;
    public static final int E7 = 52;
    public static final int F7 = 53;
    public static final int G7 = 54;
    public static final int H7 = 55;

    public static final int A8 = 56;
    public static final int B8 = 57;
    public static final int C8 = 58;
    public static final int D8 = 59;
    public static final int E8 = 60;
    public static final int F8 = 61;
    public static final int G8 = 62;
    public static final int H8 = 63;

    public static void main(String[] args) {
        // generate code for some of the constants above
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int square = square(x, y);
                String name = toString(square).toUpperCase();
                System.out.println("public static final int " + name + " = " + square + ";");
            }
            System.out.println();
        }
    }

    public static int firstOf(long squareSet) {
        assert squareSet != 0;
        return Long.numberOfTrailingZeros(squareSet);
    }

    public static int lastOf(long squareSet) {
        assert squareSet != 0;
        return 63 - Long.numberOfLeadingZeros(squareSet);
    }

    public static int mirrorY(int square) {
        assert isValid(square);
        return 0b111000 ^ square;
    }

    public static int reverse(int square) {
        assert isValid(square);
        return 63 - square;
    }

    public static int x(int square) {
        assert isValid(square);
        return square & 0b111;
    }

    public static int y(int square) {
        assert isValid(square);
        return square >>> 3;
    }

    public static int square(int x, int y) {
        assert (x & 0b111) == x;
        assert (y & 0b111) == y;
        return (y << 3) | x;
    }

    public static boolean isValid(int square) {
        return (square & 0b111111) == square;
    }

    public static String toString(int square) {
        assert isValid(square);
        return (char) ('a' + x(square)) + "" + ((1 + y(square)));
    }
}
