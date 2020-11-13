package com.etherblood.etherchess.engine.sandbox;

import com.etherblood.etherchess.engine.FenConverter;
import com.etherblood.etherchess.engine.MirrorZobrist;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.AlwaysReplaceTable;
import com.etherblood.etherchess.engine.table.NoopTable;
import com.etherblood.etherchess.engine.table.Table;
import com.etherblood.etherchess.engine.table.TableEntry;
import java.util.ArrayList;
import java.util.Random;

public class Perft {

    //    private final Table table = new AlwaysReplaceTable(25);
    private final Table table = new NoopTable();
    private final MoveGenerator moveGen = new MoveGenerator();

    public static void main(String[] args) {
        String fen = FenConverter.DEFAULT_STARTPOSITION;
        int depth = 7;

        FenConverter converter = new FenConverter();
        State state = new State(new MirrorZobrist(new Random(425)::nextLong));
        converter.fromFen(state, fen);
        Perft perft = new Perft();

        System.out.println(fen);
        System.out.println(state.toBoardString());
        System.out.println("warmup...");
        for (int i = 0; i < depth; i++) {
            perft.perft(state, i);
        }
        System.out.println("calculating...");
        long startNanos = System.nanoTime();
        long sum = perft.perft(state, depth);
        long durationNanos = System.nanoTime() - startNanos;
        System.out.println("perft(" + depth + ")=" + sum);
        long durationMillis = durationNanos / 1_000_000;
        System.out.println("in " + durationMillis + " ms (" + Math.round((double) sum / durationMillis) + " knps)");
        System.out.println();
        if (perft.table instanceof AlwaysReplaceTable) {
            ((AlwaysReplaceTable) perft.table).printStats();
        }
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
        TableEntry entry = new TableEntry();
        long hash = state.hash();
        if (table.load(hash, entry) && (entry.raw & 0xff) == depth) {
            return entry.raw >>> 8;
        }
        ArrayList<Move> legalMoves = new ArrayList<>();
        moveGen.generateLegalMoves(state, legalMoves::add);
        long sum = 0;
        if (depth == 1) {
            sum += legalMoves.size();
        } else {
            State child = new State(state.zobrist);
            for (Move move : legalMoves) {
                child.copyFrom(state);
                move.applyTo(child);
                assert moveGen.findOwnCheckers(child) == 0;
                sum += innerPerft(child, depth - 1);
            }
        }
        entry.raw = (sum << 8) | depth;
        table.store(hash, entry);
        return sum;
    }
}
