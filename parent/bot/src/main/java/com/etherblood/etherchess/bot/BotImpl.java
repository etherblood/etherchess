package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.bot.evaluation.Evaluation;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.Table;
import com.etherblood.etherchess.engine.table.TableEntry;
import com.etherblood.etherchess.engine.util.SquareSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BotImpl {

    private static final boolean VERBOSE = false;
    // http://talkchess.com/forum3/viewtopic.php?f=7&t=74769
    private static final boolean INTERNAL_ITERATIVE_REDUCTIONS = true;
    private static final boolean PRINCIPAL_VARIATION_SEARCH = true;
    private static final boolean ITERATIVE_DEEPENING = true;

    private static final int UNKNOWN_BOUND = 0;
    private static final int LOWER_BOUND = 1;
    private static final int UPPER_BOUND = 2;
    private static final int EXACT_BOUND = 3;
    private static final int MATE_SCORE = 15_000;

    private final Table table;
    private final Evaluation eval;
    private final MoveGenerator moveGen;

    private long nodes;
    private int selDepth;

    public BotImpl(Table table, Evaluation eval, MoveGenerator moveGen) {
        this.table = table;
        this.eval = eval;
        this.moveGen = moveGen;
    }

    public Move findBest(State state, HashHistory history, int depth, SearchResult result) {
        if (history.lastHash() != state.hash()) {
            throw new IllegalArgumentException("Last hash of history must match current state");
        }
        if (VERBOSE) {
            List<String> flags = new ArrayList<>();
            flags.add("verbose");
            if (INTERNAL_ITERATIVE_REDUCTIONS) {
                flags.add("iir");
            }
            if (PRINCIPAL_VARIATION_SEARCH) {
                flags.add("pvs");
            }
            if (ITERATIVE_DEEPENING) {
                flags.add("id");
            }
            flags.sort(Comparator.naturalOrder());
            result.string("config: " + flags.stream().collect(Collectors.joining(", ")));
        }
        nodes = 0;
        selDepth = 0;
        int score = 0;
        Move best = null;
        long startNanos = System.nanoTime();
        TableEntry entry = new TableEntry();
        try {
            for (int i = ITERATIVE_DEEPENING ? 1 : depth; i <= depth; i++) {
                score = alphaBeta(state, history, i, -MATE_SCORE - 1, MATE_SCORE + 1);
                if (table.load(state.hash(), entry)) {
                    best = unpackMove(entry.raw);
                } else if (VERBOSE) {
                    System.out.println("move was not stored in table");
                }
                long durationNanos = System.nanoTime() - startNanos;
                SearchStats stats = new SearchStats();
                stats.depth = i;
                stats.seldepth = selDepth - history.size();
                stats.millis = durationNanos / 1_000_000;
                stats.nodes = nodes;
                stats.scoreCp = score;
                stats.pv = collectPv(state, i);
                result.stats(stats);
            }
        } catch (InterruptedException e) {
            if (VERBOSE) {
                System.out.println("search interrupted");
            }
        }
        result.bestMove(best);
        return best;
    }

    private List<Move> collectPv(State state, int depth) {
        List<Move> pv = new ArrayList<>();
        TableEntry entry = new TableEntry();
        if (depth > 0 && table.load(state.hash(), entry)) {
            int bounds = unpackBounds(entry.raw);
            if (bounds == EXACT_BOUND || bounds == LOWER_BOUND) {
                Move move = unpackMove(entry.raw);
                pv.add(move);
                State child = new State(state.zobrist);
                child.copyFrom(state);
                move.applyTo(child);
                pv.addAll(collectPv(child, depth - 1));
            }
        }
        return pv;
    }

    private int alphaBeta(State state, HashHistory history, int depth, int alpha, int beta) throws InterruptedException {
        assert history.lastHash() == state.hash();
        nodes++;
        boolean isPvNode = alpha + 1 < beta;
        if (isPvNode) {
            selDepth = Math.max(selDepth, history.size());
        }
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
        if (depth >= 4 && Thread.interrupted()) {
            throw new InterruptedException();
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
        if (INTERNAL_ITERATIVE_REDUCTIONS && !isPvNode && hashMove == null) {
            depth--;
        }
        if (depth <= 0) {
            return clamp(eval.evaluate(state) + moves.size(), alpha, beta);
        }

        moves.sort(new SimpleMoveComparator(state, hashMove));
        int bounds = UPPER_BOUND;
        Move bestMove = null;
        State child = new State(state.zobrist);
        for (int moveIndex = 0; moveIndex < moves.size(); moveIndex++) {
            Move move = moves.get(moveIndex);
            child.copyFrom(state);
            move.applyTo(child);
            history.add(child.hash());
            int score;
            if (PRINCIPAL_VARIATION_SEARCH && isPvNode && bounds != UPPER_BOUND) {
                score = -alphaBeta(child, history, depth - 1, -alpha - 1, -alpha);
                if (alpha < score) {
                    score = -alphaBeta(child, history, depth - 1, -beta, -alpha);
                }
            } else {
                score = -alphaBeta(child, history, depth - 1, -beta, -alpha);
            }
            history.removeLast();
            assert alpha <= score && score <= beta;
            if (score > alpha) {
                if (score >= beta) {
                    alpha = beta;
                    bounds = LOWER_BOUND;
                    bestMove = move;
                    break;
                }
                alpha = score;
                bounds = EXACT_BOUND;
                bestMove = move;
            }
        }
        entry.raw = packRaw(depth, alpha, bounds, bestMove);
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
        long raw = packRaw(depth, score, bounds);
        if (move != null) {
            raw |= (Move.pack32(move) & 0xffffffffL) << 32;
        }
        return raw;
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
