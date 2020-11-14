package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.bot.evaluation.Evaluation;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.Table;
import com.etherblood.etherchess.engine.table.TableEntry;
import com.etherblood.etherchess.engine.util.SquareSet;
import java.util.ArrayList;
import java.util.List;

public class BotImpl {

    private static final boolean VERBOSE = true;
    // http://talkchess.com/forum3/viewtopic.php?f=7&t=74769
    private static final boolean INTERNAL_ITERATIVE_REDUCTIONS = true;

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

    public Move findBest(State state, HashHistory history, int depth) {
        if (history.lastHash() != state.hash()) {
            throw new IllegalArgumentException("Last hash of history must match current state");
        }
        nodes = 0;
        int score = 0;
        long startNanos = System.nanoTime();
        for (int i = 1; i <= depth; i++) {
            score = alphaBeta(state, history, i, -MATE_SCORE - 1, MATE_SCORE + 1);
        }
        long durationNanos = System.nanoTime() - startNanos;
        if (VERBOSE) {
            long durationMillis = durationNanos / 1_000_000;
            System.out.println("depth: " + depth);
            System.out.println("score: " + score);
            System.out.println(nodes + " nodes in " + durationMillis + " ms (" + Math.round((double) nodes / durationMillis) + "knps)");
            System.out.println("branching: " + Math.log(nodes) / Math.log(depth));
        }
        TableEntry entry = new TableEntry();
        if (!table.load(state.hash(), entry)) {
            if (VERBOSE) {
                System.out.println("Move was not stored after search, returning first possible move.");
            }
            return moveGen.generateLegalMoves(state).get(0);
        }
        return unpackMove(entry.raw);
    }

    private int alphaBeta(State state, HashHistory history, int depth, int alpha, int beta) {
        assert history.lastHash() == state.hash();
        nodes++;
        List<Move> moves = new ArrayList<>();
        moveGen.generateLegalMoves(state, moves::add);
        if (moves.isEmpty()) {
            if (moveGen.findOpponentCheckers(state) != 0) {
                return clamp(-MATE_SCORE, alpha, beta);
            }
            return clamp(0, alpha, beta);
        }
        if (history.isDraw(state.fiftyMovesCounter)) {
            return clamp(0, alpha, beta);
        }
        if (insufficientMatingMaterial(state)) {
            return clamp(0, alpha, beta);
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
                    hashMove = unpackMove(entry.raw);
                    break;
                }
                case EXACT_BOUND: {
                    if (unpackDepth(entry.raw) >= depth) {
                        return clamp(unpackScore(entry.raw), alpha, beta);
                    }
                    hashMove = unpackMove(entry.raw);
                    break;
                }
                default:
                    // do nothing
                    break;
            }
        }

        if (INTERNAL_ITERATIVE_REDUCTIONS && hashMove == null) {
            depth--;
        }
        if (depth <= 0) {
            return clamp(eval.evaluate(state) + moves.size(), alpha, beta);
        }
        moves.sort(new SimpleMoveComparator(state, hashMove));

        entry.raw = packRaw(depth, alpha, UPPER_BOUND);
        State child = new State(state.zobrist);
        for (int moveIndex = 0; moveIndex < moves.size(); moveIndex++) {
            Move move = moves.get(moveIndex);
            child.copyFrom(state);
            move.applyTo(child);
            history.add(child.hash());
            int score = -alphaBeta(child, history, depth - 1, -beta, -alpha);
            history.removeLast();
            assert alpha <= score && score <= beta;
            if (score > alpha) {
                if (score >= beta) {
                    alpha = beta;
                    entry.raw = packRaw(depth, alpha, LOWER_BOUND, move);
                    break;
                }
                alpha = score;
                entry.raw = packRaw(depth, alpha, EXACT_BOUND, move);
            }
        }
        table.store(hash, entry);
        return alpha;
    }

    private boolean insufficientMatingMaterial(State state) {
        if ((state.pawns() | state.rooks() | state.queens()) == 0) {
            if (SquareSet.count(state.bishops() | state.knights()) <= 1) {
                return true;
            }

            if (state.knights() == 0) {
                long whiteSquareBishops = state.bishops() & SquareSet.WHITE_SQUARES;
                if (whiteSquareBishops == 0 || whiteSquareBishops == state.bishops()) {
                    return true;
                }
            }
        }
        return false;
    }

    private long packRaw(int depth, int score, int bounds, Move move) {
        return ((Move.pack32(move) & 0xffffffffL) << 32) | packRaw(depth, score, bounds);
    }

    private long packRaw(int depth, int score, int bounds) {
        assert (bounds & 0xf) == bounds;
        assert (short) score == score;
        assert (depth & 0xfff) == depth;
        return ((score & 0xffffL) << 16) | ((bounds & 0xfL) << 12) | (depth & 0xfffL);
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

    private Move unpackMove(long raw) {
        return Move.unpack32((int) (raw >>> 32));
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
