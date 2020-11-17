package com.etherblood.etherchess.bot;

public class Scores {
    private static final int MATE_SCORE = 32_766;
    private static final int MATE_BOUND = 32_000;

    public static short toTableScore(int score, int currentPly) {
        if (score >= MATE_BOUND) {
            int matePly = MATE_SCORE - score;
            int mateDepth = matePly - currentPly;
            assert fromTableScore((short) (MATE_SCORE - mateDepth), currentPly) == score;
            return (short) (MATE_SCORE - mateDepth);

        } else if (score <= -MATE_BOUND) {
            int matePly = score + MATE_SCORE;
            int mateDepth = matePly - currentPly;
            assert fromTableScore((short) (-MATE_SCORE + mateDepth), currentPly) == score;
            return (short) (-MATE_SCORE + mateDepth);

        }
        assert fromTableScore((short) score, currentPly) == score : score;
        assert !isMateScore(score);
        return (short) score;
    }

    public static int fromTableScore(short score, int currentPly) {
        if (score >= MATE_BOUND) {
            int mateDepth = MATE_SCORE - score;
            int matePly = currentPly + mateDepth;
            return MATE_SCORE - matePly;
        } else if (score <= -MATE_BOUND) {
            int mateDepth = score + MATE_SCORE;
            int matePly = currentPly + mateDepth;
            return -MATE_SCORE + matePly;
        }
        assert !isMateScore(score);
        return score;
    }

    public static int mateLossScore(int currentPly) {
        return -mateScore(currentPly);
    }

    public static int mateScore(int currentPly) {
        return MATE_SCORE - currentPly;
    }

    public static int plyFromMateScore(int mateScore) {
        return MATE_SCORE - Math.abs(mateScore);
    }

    public static boolean isMateScore(int score) {
        return Math.abs(score) >= MATE_BOUND;
    }
}
