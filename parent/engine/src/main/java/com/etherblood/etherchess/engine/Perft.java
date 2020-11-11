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
        ArrayList<Move> legalMoves = new ArrayList<>();
        moveGen.generateLegalMoves(state, legalMoves::add);
        long sum = 0;
        if (depth == 1) {
            sum += legalMoves.size();
        } else {
            State child = alloc(state);
            for (Move move : legalMoves) {
                child.copyFrom(state);
                move.apply(child);
                assert moveGen.findOwnCheckers(child) == 0;
                sum += innerPerft(child, depth - 1);
            }
            free(child);
        }
        return sum;
    }

    private State alloc(State parent) {
        State child;
        if (statePool.isEmpty()) {
            child = new State(parent.zobrist);
        } else {
            child = statePool.remove(statePool.size() - 1);
        }
        return child;
    }

    private void free(State state) {
        statePool.add(state);
    }
}
