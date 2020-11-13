package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.Table;
import com.etherblood.etherchess.engine.table.TableEntry;
import java.util.ArrayList;
import java.util.List;

public class BotImpl {

    // http://talkchess.com/forum3/viewtopic.php?f=7&t=74769
    private static final boolean INTERNAL_ITERATIVE_REDUCTIONS = false;

    private static final int UNKNOWN_BOUND = 0;
    private static final int LOWER_BOUND = 1;
    private static final int UPPER_BOUND = 2;
    private static final int EXACT_BOUND = 3;
    private static final int MATE_SCORE = 15_000;

    private final Table table;
    private final Evaluation eval;
    private final MoveGenerator moveGen;

    private long nodes;

    public BotImpl(Table table, Evaluation eval, MoveGenerator moveGen) {
        this.table = table;
        this.eval = eval;
        this.moveGen = moveGen;
    }

    public Move findBest(State state, int depth) {
        nodes = 0;
        int score = 0;
        for (int i = 1; i <= depth; i++) {
            score = alphaBeta(state, i, -MATE_SCORE - 1, MATE_SCORE + 1);
        }
        System.out.println("depth: " + depth);
        System.out.println("score: " + score);
        System.out.println(nodes + " nodes");
        System.out.println("branching: " + Math.log(nodes) / Math.log(depth));
        TableEntry entry = new TableEntry();
        if (!table.load(state.hash(), entry)) {
            throw new AssertionError("Move was not stored after search.");
        }
        return Move.unpack(unpackMove(entry.raw));
    }

    private int alphaBeta(State state, int depth, int alpha, int beta) {
        nodes++;
        List<Move> moves = new ArrayList<>();
        moveGen.generateLegalMoves(state, moves::add);
        if (moves.isEmpty()) {
            if (moveGen.findOpponentCheckers(state) != 0) {
                return clamp(-MATE_SCORE, alpha, beta);
            }
            return clamp(0, alpha, beta);
        }
        if (depth <= 0) {
            return clamp(eval.evaluate(state) + moves.size(), alpha, beta);
        }

        Move hashMove = null;
        long hash = state.hash();
        TableEntry entry = new TableEntry();
        if (table.load(hash, entry)) {
            switch (unpackBounds(entry.raw)) {
                case UPPER_BOUND: {
                    if (unpackDepth(entry.raw) >= depth) {
                        int score = unpackScore(entry.raw);
                        if (score < beta) {
                            if (score <= alpha) {
                                return alpha;
                            }
                            beta = score;
                        }
                    }
                    break;
                }
                case LOWER_BOUND: {
                    if (unpackDepth(entry.raw) >= depth) {
                        int score = unpackScore(entry.raw);
                        if (score > alpha) {
                            if (score >= beta) {
                                return beta;
                            }
                            alpha = score;
                        }
                    }
                    hashMove = Move.unpack(unpackMove(entry.raw));
                    break;
                }
                case EXACT_BOUND: {
                    if (unpackDepth(entry.raw) >= depth) {
                        return clamp(unpackScore(entry.raw), alpha, beta);
                    }
                    hashMove = Move.unpack(unpackMove(entry.raw));
                    break;
                }
                default:
                    // do nothing
                    break;
            }
        }

        if (hashMove != null) {
            int index = moves.indexOf(hashMove);
            assert index >= 0;
            moves.set(0, moves.set(index, moves.get(0)));
        } else if (INTERNAL_ITERATIVE_REDUCTIONS) {
            depth--;
        }

        entry.raw = packRaw(depth, alpha, UPPER_BOUND, 254);
        State child = new State(state.zobrist);
        for (int moveIndex = 0; moveIndex < moves.size(); moveIndex++) {
            Move move = moves.get(moveIndex);
            child.copyFrom(state);
            move.applyTo(child);
            int score;
            score = -alphaBeta(child, depth - 1, -beta, -alpha);
            if (!(alpha <= score && score <= beta)) {
                score = -alphaBeta(child, depth - 1, -beta, -alpha);
                System.out.println(alpha);
                System.out.println(score);
                System.out.println(beta);
            }
            assert alpha <= score && score <= beta;
            if (score > alpha) {
                if (score >= beta) {
                    alpha = beta;
                    entry.raw = packRaw(depth, alpha, LOWER_BOUND, Move.pack(move));
                    break;
                }
                alpha = score;
                entry.raw = packRaw(depth, alpha, EXACT_BOUND, Move.pack(move));
            }
        }
        table.store(hash, entry);
        return alpha;
    }

    private long packRaw(long depth, long score, long bounds, long move) {
        assert (int) move == move;
        assert (bounds & 0xf) == bounds;
        assert (short) score == score;
        assert (depth & 0xfff) == depth;
        return ((move & 0xffffffff) << 32) | ((score & 0xffff) << 16) | ((bounds & 0xf) << 12) | (depth & 0xfff);
    }

    private int unpackDepth(long raw) {
        return (short) raw & 0xfff;
    }

    private int unpackScore(long raw) {
        return (short) (raw >>> 16);
    }

    private int unpackBounds(long raw) {
        return (int) (raw >>> 12) & 0xf;
    }

    private int unpackMove(long raw) {
        return (int) (raw >>> 32);
    }

    private int clamp(int score, int alpha, int beta) {
        if (score <= alpha) {
            return alpha;
        }
        if (score >= beta) {
            return beta;
        }
        return score;
    }
}
