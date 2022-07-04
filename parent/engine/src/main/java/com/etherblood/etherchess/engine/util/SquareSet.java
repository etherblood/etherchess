package com.etherblood.etherchess.engine.util;

public class SquareSet {

    public static final long A1 = 0x1L;
    public static final long B1 = 0x2L;
    public static final long C1 = 0x4L;
    public static final long D1 = 0x8L;
    public static final long E1 = 0x10L;
    public static final long F1 = 0x20L;
    public static final long G1 = 0x40L;
    public static final long H1 = 0x80L;

    public static final long A2 = 0x100L;
    public static final long B2 = 0x200L;
    public static final long C2 = 0x400L;
    public static final long D2 = 0x800L;
    public static final long E2 = 0x1000L;
    public static final long F2 = 0x2000L;
    public static final long G2 = 0x4000L;
    public static final long H2 = 0x8000L;

    public static final long A3 = 0x10000L;
    public static final long B3 = 0x20000L;
    public static final long C3 = 0x40000L;
    public static final long D3 = 0x80000L;
    public static final long E3 = 0x100000L;
    public static final long F3 = 0x200000L;
    public static final long G3 = 0x400000L;
    public static final long H3 = 0x800000L;

    public static final long A4 = 0x1000000L;
    public static final long B4 = 0x2000000L;
    public static final long C4 = 0x4000000L;
    public static final long D4 = 0x8000000L;
    public static final long E4 = 0x10000000L;
    public static final long F4 = 0x20000000L;
    public static final long G4 = 0x40000000L;
    public static final long H4 = 0x80000000L;

    public static final long A5 = 0x100000000L;
    public static final long B5 = 0x200000000L;
    public static final long C5 = 0x400000000L;
    public static final long D5 = 0x800000000L;
    public static final long E5 = 0x1000000000L;
    public static final long F5 = 0x2000000000L;
    public static final long G5 = 0x4000000000L;
    public static final long H5 = 0x8000000000L;

    public static final long A6 = 0x10000000000L;
    public static final long B6 = 0x20000000000L;
    public static final long C6 = 0x40000000000L;
    public static final long D6 = 0x80000000000L;
    public static final long E6 = 0x100000000000L;
    public static final long F6 = 0x200000000000L;
    public static final long G6 = 0x400000000000L;
    public static final long H6 = 0x800000000000L;

    public static final long A7 = 0x1000000000000L;
    public static final long B7 = 0x2000000000000L;
    public static final long C7 = 0x4000000000000L;
    public static final long D7 = 0x8000000000000L;
    public static final long E7 = 0x10000000000000L;
    public static final long F7 = 0x20000000000000L;
    public static final long G7 = 0x40000000000000L;
    public static final long H7 = 0x80000000000000L;

    public static final long A8 = 0x100000000000000L;
    public static final long B8 = 0x200000000000000L;
    public static final long C8 = 0x400000000000000L;
    public static final long D8 = 0x800000000000000L;
    public static final long E8 = 0x1000000000000000L;
    public static final long F8 = 0x2000000000000000L;
    public static final long G8 = 0x4000000000000000L;
    public static final long H8 = 0x8000000000000000L;

    public static final long RANK_1 = 0xffL;
    public static final long RANK_2 = 0xff00L;
    public static final long RANK_3 = 0xff0000L;
    public static final long RANK_4 = 0xff000000L;
    public static final long RANK_5 = 0xff00000000L;
    public static final long RANK_6 = 0xff0000000000L;
    public static final long RANK_7 = 0xff000000000000L;
    public static final long RANK_8 = 0xff00000000000000L;

    public static final long FILE_A = 0x101010101010101L;
    public static final long FILE_B = 0x202020202020202L;
    public static final long FILE_C = 0x404040404040404L;
    public static final long FILE_D = 0x808080808080808L;
    public static final long FILE_E = 0x1010101010101010L;
    public static final long FILE_F = 0x2020202020202020L;
    public static final long FILE_G = 0x4040404040404040L;
    public static final long FILE_H = 0x8080808080808080L;

    public static final long OUTER = FILE_A | FILE_H | RANK_1 | RANK_8;
    public static final long INNER = ~OUTER;
    public static final long MAIN_DIAGONAL = 0x8040201008040201L;
    public static final long MAIN_ANTIDIAGONAL = 0x102040810204080L;
    public static final long BLACK_SQUARES = 0x5555555555555555L;
    public static final long WHITE_SQUARES = ~BLACK_SQUARES;

    private static final long[] DIAGONAL = new long[64];
    private static final long[] ANTIDIAGONAL = new long[64];

    static final long[] NORTH_RAY = new long[64];
    static final long[] NORTHEAST_RAY = new long[64];
    static final long[] EAST_RAY = new long[64];
    static final long[] SOUTHEAST_RAY = new long[64];
    static final long[] SOUTH_RAY = new long[64];
    static final long[] SOUTHWEST_RAY = new long[64];
    static final long[] WEST_RAY = new long[64];
    static final long[] NORTHWEST_RAY = new long[64];

    static {
        precomputeDiagonals();
        precomputeRays();
    }

    private static void precomputeDiagonals() {
        for (int square = 0; square < 64; square++) {
            int x = Square.x(square);
            int y = Square.y(square);
            if (y >= x) {
                DIAGONAL[square] = MAIN_DIAGONAL << (8 * (y - x));
            } else {
                DIAGONAL[square] = MAIN_DIAGONAL >>> (8 * (x - y));
            }
            ANTIDIAGONAL[Square.mirrorY(square)] = SquareSet.mirrorY(DIAGONAL[square]);
        }
    }

    private static void precomputeRays() {
        for (int square = 0; square < 64; square++) {
            SOUTHEAST_RAY[square] = antiDiagonalOf(square) & lower(square);
            SOUTH_RAY[square] = fileOf(square) & lower(square);
            SOUTHWEST_RAY[square] = diagonalOf(square) & lower(square);
            WEST_RAY[square] = rankOf(square) & lower(square);
        }
        for (int square = 0; square < 64; square++) {
            int reversedSquare = Square.reverse(square);
            NORTHWEST_RAY[square] = reverse(SOUTHEAST_RAY[reversedSquare]);
            NORTH_RAY[square] = reverse(SOUTH_RAY[reversedSquare]);
            NORTHEAST_RAY[square] = reverse(SOUTHWEST_RAY[reversedSquare]);
            EAST_RAY[square] = reverse(WEST_RAY[reversedSquare]);
        }
    }

    public static void main(String[] args) {
        // generate code for some of the constants above
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int square = Square.of(x, y);
                String name = Square.toString(square).toUpperCase();
                System.out.println("public static final long " + name + " = 0x" + Long.toHexString(of(square)) + "L;");
            }
            System.out.println();
        }
        for (int y = 0; y < 8; y++) {
            long rank = 0xffL << (8 * y);
            System.out.println("public static final long RANK_" + (y + 1) + " = 0x" + Long.toHexString(rank) + "L;");
        }
        System.out.println();
        for (int x = 0; x < 8; x++) {
            long file = Long.divideUnsigned(~0L, 0xffL) << x;
            System.out.println("public static final long FILE_" + (char) ('A' + x) + " = 0x" + Long.toHexString(file) + "L;");
        }
        long mainDiagonal = 0;
        for (int i = 0; i < 8; i++) {
            mainDiagonal |= of(Square.of(i, i));
        }
        System.out.println("public static final long MAIN_DIAGONAL = 0x" + Long.toHexString(mainDiagonal) + "L;");
        System.out.println("public static final long MAIN_ANTIDIAGONAL = 0x" + Long.toHexString(mirrorY(mainDiagonal)) + "L;");
        System.out.println();
    }

    public static long of(int square) {
        assert Square.isValid(square);
        return 1L << square;
    }

    public static long firstOf(long squareSet) {
        return Long.lowestOneBit(squareSet);
    }

    public static long clearFirst(long squareSet) {
        return squareSet & (squareSet - 1);
    }

    public static long fileOf(int square) {
        return FILE_A << Square.x(square);
    }

    public static long rankOf(int square) {
        return RANK_1 << (Square.y(square) << 3);
    }

    public static long diagonalOf(int square) {
        return DIAGONAL[square];
    }

    public static long antiDiagonalOf(int square) {
        return ANTIDIAGONAL[square];
    }

    public static long mirrorY(long squareSet) {
        return Long.reverseBytes(squareSet);
    }

    public static long reverse(long squareSet) {
        return Long.reverse(squareSet);
    }

    public static int count(long squareSet) {
        return Long.bitCount(squareSet);
    }

    public static String toString(long squareSet) {
        StringBuilder builder = new StringBuilder();
        for (int y = 7; y >= 0; y--) {
            for (int x = 0; x < 8; x++) {
                int square = Square.of(x, y);
                long single = SquareSet.of(square);
                builder.append((squareSet & single) != 0 ? 'X' : '.');
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

    public static long simpleNorthRay(int square) {
        return NORTH_RAY[square];
    }

    public static long simpleSouthRay(int square) {
        return SOUTH_RAY[square];
    }

    public static long simpleWestRay(int square) {
        return WEST_RAY[square];
    }

    public static long simpleEastRay(int square) {
        return EAST_RAY[square];
    }

    public static long simpleNorthWestRay(int square) {
        return NORTHWEST_RAY[square];
    }

    public static long simpleNorthEastRay(int square) {
        return NORTHEAST_RAY[square];
    }

    public static long simpleSouthWestRay(int square) {
        return SOUTHWEST_RAY[square];
    }

    public static long simpleSouthEastRay(int square) {
        return SOUTHEAST_RAY[square];
    }

    public static long simpleDirectionRay(int direction, int square) {
        assert Direction.assertValid(direction);
        switch (direction) {
            case Direction.NORTH:
                return simpleNorthRay(square);
            case Direction.NORTH_EAST:
                return simpleNorthEastRay(square);
            case Direction.EAST:
                return simpleEastRay(square);
            case Direction.SOUTH_EAST:
                return simpleSouthEastRay(square);
            case Direction.SOUTH:
                return simpleSouthRay(square);
            case Direction.SOUTH_WEST:
                return simpleSouthWestRay(square);
            case Direction.WEST:
                return simpleWestRay(square);
            case Direction.NORTH_WEST:
                return simpleNorthWestRay(square);
            default:
                throw new AssertionError(direction);
        }
    }

    public static long lower(int sq) {
        return (1L << sq) - 1;
    }

    public static long upper(int sq) {
        return (~1L << sq);
    }
}
