package com.etherblood.etherchess.engine.util;

public class PieceSquareSet {

    private static final long[] KING_ATTACKS = new long[64];
    private static final long[] KNIGHT_ATTACKS = new long[64];
    private static final long[] SQUARES_BETWEEN = new long[64 * 64];

    private static final long[] KING_DANGER_ROOKS_MASK = new long[64];
    private static final long[] KING_DANGER_BISHOPS_MASK = new long[64];
    private static final long[] KING_DANGER_KNIGHTS_MASK = new long[64];

    static {
        precomputeKingAttacks();
        precomputeKnightAttacks();
        precomputeRaySquaresBetween();
        precomputeKingDangerMasks();
    }

    private static void precomputeRaySquaresBetween() {
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                if (from == to) {
                    continue;
                }
                int index = from + 64 * to;

                int fromX = Square.x(from);
                int fromY = Square.y(from);

                int toX = Square.x(to);
                int toY = Square.y(to);
                int diffX = toX - fromX;
                int diffY = toY - fromY;

                if (diffX == 0 || diffY == 0 || Math.abs(diffX) == Math.abs(diffY)) {
                    long mask = SquareSet.of(from);
                    int direction = Integer.signum(diffX) + 8 * Integer.signum(diffY);
                    for (int i = from; i != to; i += direction) {
                        mask ^= SquareSet.of(i);
                    }
                    SQUARES_BETWEEN[index] = mask;
                } else {
                    SQUARES_BETWEEN[index] = 0;
                }
            }
        }
    }

    private static void precomputeKingAttacks() {
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                int x = Square.x(from) - Square.x(to);
                int y = Square.y(from) - Square.y(to);
                if (x * x + y * y <= 2) {
                    KING_ATTACKS[from] |= SquareSet.of(to);
                }
            }
            KING_ATTACKS[from] ^= SquareSet.of(from);
        }
    }

    private static void precomputeKnightAttacks() {
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                int x = Square.x(from) - Square.x(to);
                int y = Square.y(from) - Square.y(to);
                if (x * x + y * y == 5) {
                    KNIGHT_ATTACKS[from] |= SquareSet.of(to);
                }
            }
        }
    }

    private static void precomputeKingDangerMasks() {
        for (int kingSquare = 0; kingSquare < 64; kingSquare++) {
            long kingZone = KING_ATTACKS[kingSquare] | SquareSet.of(kingSquare);
            if (kingSquare == Square.E1) {
                kingZone |= SquareSet.C1 | SquareSet.G1;
            }

            while (kingZone != 0) {
                int dangerSquare = Square.firstOf(kingZone);
                KING_DANGER_ROOKS_MASK[kingSquare] |= SquareSet.rankOf(dangerSquare) | SquareSet.fileOf(dangerSquare);
                KING_DANGER_BISHOPS_MASK[kingSquare] |= SquareSet.diagonalOf(dangerSquare) | SquareSet.antiDiagonalOf(dangerSquare);
                KING_DANGER_KNIGHTS_MASK[kingSquare] |= knightMoves(dangerSquare);

                kingZone = SquareSet.clearFirst(kingZone);
            }
        }
    }

    public static long kingMoves(int from) {
        return KING_ATTACKS[from];
    }

    public static long knightMoves(int from) {
        return KNIGHT_ATTACKS[from];
    }

    public static long kingDangerRooksMask(int kingSquare) {
        return KING_DANGER_ROOKS_MASK[kingSquare];
    }

    public static long kingDangerBishopsMask(int kingSquare) {
        return KING_DANGER_BISHOPS_MASK[kingSquare];
    }

    public static long kingDangerKnightsMask(int kingSquare) {
        return KING_DANGER_KNIGHTS_MASK[kingSquare];
    }

    public static long queenRays(int from, long occupied) {
        return rookRays(from, occupied) | bishopRays(from, occupied);
    }

    public static long rookRays(int square, long occupied) {
        return northRay(square, occupied)
                | southRay(square, occupied)
                | eastRay(square, occupied)
                | westRay(square, occupied);
    }

    public static long bishopRays(int square, long occupied) {
        return northWestRay(square, occupied)
                | southEastRay(square, occupied)
                | northEastRay(square, occupied)
                | southWestRay(square, occupied);
    }

    public static long northRay(int square, long occupied) {
        return positiveRay(square, occupied, SquareSet.NORTH_RAY);
    }

    public static long southRay(int square, long occupied) {
        return negativeRay(square, occupied, SquareSet.SOUTH_RAY);
    }

    public static long eastRay(int square, long occupied) {
        return positiveRay(square, occupied, SquareSet.EAST_RAY);
    }

    public static long westRay(int square, long occupied) {
        return negativeRay(square, occupied, SquareSet.WEST_RAY);
    }


    public static long northWestRay(int square, long occupied) {
        return positiveRay(square, occupied, SquareSet.NORTHWEST_RAY);
    }

    public static long southWestRay(int square, long occupied) {
        return negativeRay(square, occupied, SquareSet.SOUTHWEST_RAY);
    }

    public static long northEastRay(int square, long occupied) {
        return positiveRay(square, occupied, SquareSet.NORTHEAST_RAY);
    }

    public static long southEastRay(int square, long occupied) {
        return negativeRay(square, occupied, SquareSet.SOUTHEAST_RAY);
    }


    private static long positiveRay(int square, long occupied, long[] squareToRay) {
        // https://talkchess.com/forum3/viewtopic.php?t=78693
        long ray = squareToRay[square];
        int obstacle = Square.firstOf((ray & occupied) | SquareSet.H8);
        return ray ^ squareToRay[obstacle];
    }

    private static long negativeRay(int square, long occupied, long[] squareToRay) {
        // https://talkchess.com/forum3/viewtopic.php?t=78693
        long ray = squareToRay[square];
        int obstacle = Square.lastOf((ray & occupied) | SquareSet.A1);
        return ray ^ squareToRay[obstacle];
    }


    public static long raySquaresBetween(int from, int to) {
        assert Square.isValid(from);
        assert Square.isValid(to);
        return SQUARES_BETWEEN[from + 64 * to];
    }

    public static long directionRay(int direction, int square, long occupied) {
        assert Direction.assertValid(direction);
        return switch (direction) {
            case Direction.NORTH -> northRay(square, occupied);
            case Direction.NORTH_EAST -> northEastRay(square, occupied);
            case Direction.EAST -> eastRay(square, occupied);
            case Direction.SOUTH_EAST -> southEastRay(square, occupied);
            case Direction.SOUTH -> southRay(square, occupied);
            case Direction.SOUTH_WEST -> southWestRay(square, occupied);
            case Direction.WEST -> westRay(square, occupied);
            case Direction.NORTH_WEST -> northWestRay(square, occupied);
            default -> throw new AssertionError(direction);
        };
    }
}
