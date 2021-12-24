package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.table.Table;
import com.etherblood.etherchess.engine.table.TableEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Perft {

    private final Table table;
    private final boolean verbose;

    public Perft(Table table, boolean verbose) {
        this.table = table;
        this.verbose = verbose;
    }

    public long perft(String fen, int depth) {
        State state = new State(zobrist());
        FenConverter converter = new FenConverter();
        converter.fromFen(state, fen);
        if (verbose) {
            System.out.println(fen);
            System.out.println(state.toBoardString());
        }
        long perft = perft(state, depth);
        if (verbose) {
            System.out.println();
        }
        return perft;
    }

    private long perft(State state, int depth) {
        if (depth == 0) {
            return 1;
        }
        TableEntry entry = new TableEntry();
        long hash = state.hash();
        if (table.load(hash, entry) && (entry.raw & 0xff) == depth) {
            return entry.raw >>> 8;
        }
        MoveGenerator moveGen = new MoveGenerator();
        List<Move> legalMoves = new ArrayList<>();
        moveGen.generateLegalMoves(state, legalMoves::add);
        long sum = 0;
        if (depth == 1) {
            sum = legalMoves.size();
        } else {
            State child = new State(state.zobrist);
            for (Move move : legalMoves) {
                try {
                    child.copyFrom(state);
                    move.applyTo(child);
                    assert moveGen.findOwnCheckers(child) == 0;
                    sum += perft(child, depth - 1);
                } catch (AssertionError e) {
                    System.out.println(move);
                    throw e;
                }
            }
        }
        entry.raw = (sum << 8) | depth;
        table.store(hash, entry);
        return sum;
    }

    public Map<Move, Long> divide(String fen, int depth) {
        State state = new State(zobrist());
        FenConverter converter = new FenConverter();
        converter.fromFen(state, fen);
        if (verbose) {
            System.out.println(fen);
            System.out.println(state.toBoardString());
        }
        Map<Move, Long> div = divide(state, depth);
        if (verbose) {
            System.out.println();
        }
        return div;
    }

    private Map<Move, Long> divide(State state, int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException();
        }
        State child = new State(state.zobrist);
        MoveGenerator moveGen = new MoveGenerator();
        List<Move> legalMoves = new ArrayList<>();
        moveGen.generateLegalMoves(state, legalMoves::add);
        legalMoves.sort(Move.defaultComparator());
        Map<Move, Long> result = new LinkedHashMap<>();
        if (depth == 1) {
            for (Move legalMove : legalMoves) {
                result.put(legalMove, 1L);
            }
            return result;
        }
        for (Move move : legalMoves) {
            try {
                child.copyFrom(state);
                move.applyTo(child);
                assert moveGen.findOwnCheckers(child) == 0;
                result.put(move, perft(child, depth - 1));
            } catch (AssertionError e) {
                System.out.println(move);
                throw e;
            }
        }
        return result;
    }

    private MirrorZobrist zobrist() {
        return new MirrorZobrist(new Random(7)::nextLong);
    }
}
