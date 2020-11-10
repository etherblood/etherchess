package com.etherblood.etherchess.engine.util;

public class Direction {

    public static final int NORTH = 0;
    public static final int NORTH_EAST = 1;
    public static final int EAST = 2;
    public static final int SOUTH_EAST = 3;
    public static final int SOUTH = 4;
    public static final int SOUTH_WEST = 5;
    public static final int WEST = 6;
    public static final int NORTH_WEST = 7;

    public static boolean assertValid(int direction) {
        assert (direction & 0x7) == direction;
        return true;
    }

    public static int inverse(int direction) {
        return 4 ^ direction;
    }

    public static boolean isDiagonal(int direction) {
        return (direction & 1) != 0;
    }
}
