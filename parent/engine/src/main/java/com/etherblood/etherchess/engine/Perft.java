package com.etherblood.etherchess.engine;

import java.util.ArrayList;
import java.util.Random;

public class Perft {

    private final ArrayList<State> statePool = new ArrayList<>();
    private final LegalMoveGenerator moveGen = new LegalMoveGenerator();

    public static void main(String[] args) {
        String fen = FenConverter.DEFAULT_STARTPOSITION;
        int depth = 7;

        FenConverter converter = new FenConverter();
        State state = new State(new MirrorZobrist(new Random(425)::nextLong));
        converter.fromFen(state, fen);
        Perft perft = new Perft();

        System.out.println(fen);
        System.out.println(state.toBoardString());
        long startNanos = System.nanoTime();
        long sum = perft.perft(state, depth);
        long durationNanos = System.nanoTime() - startNanos;
        System.out.println("perft(" + depth + ")=" + sum);
        long durationMillis = durationNanos / 1_000_000;
        System.out.println("In " + durationMillis + " ms (" + Math.round((double) sum / durationMillis) + " knps)");
    }

    public long perft(State state, int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException(Integer.toString(depth));
        }
        if (depth == 0) {
            return 1;
        }
        return innerPerft(state, depth);
    }

    private long innerPerft(State state, int depth) {
        State child = allocCopy(state);
        ArrayList<Move> legalMoves = new ArrayList<>();
        moveGen.generateLegalMoves(state, legalMoves::add);
        long sum = 0;
        if (depth == 1) {
            sum += legalMoves.size();
        } else {
            for (Move move : legalMoves) {
                try {
                    child.copyFrom(state);
                    move.apply(child);
                    assert moveGen.findOwnCheckers(child) == 0;
                    sum += innerPerft(child, depth - 1);
                } catch (AssertionError e) {
                    System.out.println(move);
                    throw e;
                }
            }
        }
        legalMoves.clear();
        return sum;
    }

    private State allocCopy(State parent) {
        State child;
        if (statePool.isEmpty()) {
            child = new State(parent.zobrist);
        } else {
            child = statePool.get(statePool.size() - 1);
        }
        child.copyFrom(parent);
        return child;
    }

    private void free(State state) {
        statePool.add(state);
    }
}
