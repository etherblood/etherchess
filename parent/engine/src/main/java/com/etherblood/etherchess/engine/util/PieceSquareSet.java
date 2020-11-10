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
        // slider masks could further be split by direction
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

    public static long southEastRay(int square, long occupied) {
        return SquareSet.reverse(northWestRay(Square.reverse(square), SquareSet.reverse(occupied)));
    }

    public static long northWestRay(int square, long occupied) {
        return rays(square, SquareSet.antiDiagonalOf(square), occupied);
    }

    public static long southWestRay(int square, long occupied) {
        return SquareSet.reverse(northEastRay(Square.reverse(square), SquareSet.reverse(occupied)));
    }

    public static long northEastRay(int square, long occupied) {
        return rays(square, SquareSet.diagonalOf(square), occupied);
    }

    public static long westRay(int square, long occupied) {
        return SquareSet.reverse(eastRay(Square.reverse(square), SquareSet.reverse(occupied)));
    }

    public static long eastRay(int square, long occupied) {
        return rays(square, SquareSet.rankOf(square), occupied);
    }

    public static long southRay(int square, long occupied) {
        return SquareSet.reverse(northRay(Square.reverse(square), SquareSet.reverse(occupied)));
    }

    public static long northRay(int square, long occupied) {
        return rays(square, SquareSet.fileOf(square), occupied);
    }

    public static long rays(int square, long mask, long occupied) {
        return (((occupied & mask) - 2 * SquareSet.of(square)) ^ occupied) & mask;
    }

    public static long raySquaresBetween(int from, int to) {
        return SQUARES_BETWEEN[from + 64 * to];
    }

    public static long directionRay(int direction, int square, long occupied) {
        assert Direction.assertValid(direction);
        switch (direction) {
            case Direction.NORTH:
                return northRay(square, occupied);
            case Direction.NORTH_EAST:
                return northEastRay(square, occupied);
            case Direction.EAST:
                return eastRay(square, occupied);
            case Direction.SOUTH_EAST:
                return southEastRay(square, occupied);
            case Direction.SOUTH:
                return southRay(square, occupied);
            case Direction.SOUTH_WEST:
                return southWestRay(square, occupied);
            case Direction.WEST:
                return westRay(square, occupied);
            case Direction.NORTH_WEST:
                return northWestRay(square, occupied);
            default:
                throw new AssertionError(direction);
        }
    }
}
