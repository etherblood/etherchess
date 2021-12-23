package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.bot.evaluation.Evaluation;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.Table;
import com.etherblood.etherchess.engine.table.TableEntry;
import com.etherblood.etherchess.engine.util.LongAlgebraicNotation;
import com.etherblood.etherchess.engine.util.SquareSet;
import com.etherblood.etherchess.uci.SearchResult;
import com.etherblood.etherchess.uci.SearchStats;
import com.etherblood.etherchess.uci.SearchStatsBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotImpl {

    private static final Logger LOG = LoggerFactory.getLogger(BotImpl.class);

    private static final boolean PRINCIPAL_VARIATION_SEARCH = true;
    private static final boolean ITERATIVE_DEEPENING = true;
    // http://talkchess.com/forum3/viewtopic.php?f=7&t=74769
    private static final boolean INTERNAL_ITERATIVE_REDUCTIONS = true;

    private static final int UNKNOWN_BOUND = 0;
    private static final int LOWER_BOUND = 1;
    private static final int UPPER_BOUND = 2;
    private static final int EXACT_BOUND = 3;

    private final Table table;
    private final Evaluation eval;
    private final MoveGenerator moveGen;

    private long nodes;
    private int selDepth;
    private int startPly;

    public BotImpl(Table table, Evaluation eval, MoveGenerator moveGen) {
        this.table = table;
        this.eval = eval;
        this.moveGen = moveGen;
        List<String> flags = new ArrayList<>();
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
        LOG.info("config: " + flags.stream().collect(Collectors.joining(", ")));
    }

    public Move findBest(State state, HashHistory history, int depth, SearchResult result) {
        if (history.lastHash() != state.hash()) {
            throw new IllegalArgumentException("Last hash of history must match current state");
        }
        nodes = 0;
        selDepth = 0;
        startPly = history.size();
        Move best = null;
        long startNanos = System.nanoTime();
        TableEntry entry = new TableEntry();
        try {
            for (int i = ITERATIVE_DEEPENING ? 1 : depth; i <= depth; i++) {
                table.remove(state.hash());// clear root entry to ensure new entry is stored
                int score = alphaBeta(state, history, i, -Short.MAX_VALUE, Short.MAX_VALUE);
                if (table.load(state.hash(), entry)) {
                    int bounds = unpackBounds(entry.raw);
                    assert bounds == EXACT_BOUND : bounds;
                    best = unpackMove(entry.raw);
                } else {
                    LOG.warn("move was not stored in table");
                }
                long durationNanos = System.nanoTime() - startNanos;
                SearchStatsBuilder stats = SearchStats.builder();
                stats.depth(i);
                stats.seldepth(selDepth - history.size());
                stats.millis(durationNanos / 1_000_000);
                stats.nodes(nodes);
                if (Scores.isMateScore(score)) {
                    if (score < 0) {
                        stats.scoreMate((Scores.plyFromMateScore(score) - history.size()) / -2 - 1);
                    } else {
                        stats.scoreMate((Scores.plyFromMateScore(score) - history.size()) / 2 + 1);
                    }
                } else {
                    stats.scoreCp(score);
                }
                stats.pv(collectPv(state, i));
                stats.hashPermill(table.fillPermill());
                result.stats(stats.build());
            }
        } catch (InterruptedException e) {
            LOG.info("search interrupted", e);
        }
        result.bestMove(LongAlgebraicNotation.toLanString(state.isWhite, best));
        return best;
    }

    private List<String> collectPv(State state, int depth) {
        List<String> pv = new ArrayList<>();
        TableEntry entry = new TableEntry();
        if (depth > 0 && table.load(state.hash(), entry)) {
            int bounds = unpackBounds(entry.raw);
            if (bounds == EXACT_BOUND || bounds == LOWER_BOUND) {
                Move move = unpackMove(entry.raw);
                pv.add(LongAlgebraicNotation.toLanString(state.isWhite, move));
                State child = new State(state.zobrist);
                child.copyFrom(state);
                move.applyTo(child);
                pv.addAll(collectPv(child, depth - 1));
            }
        }
        return pv;
    }

    private int alphaBeta(State state, HashHistory history, int depth, int alpha, int beta) throws InterruptedException {
        SearchContext context = new SearchContext();
        context.state = state;
        context.history = history;
        context.depth = depth;
        context.alpha = alpha;
        context.beta = beta;
        context.moves = new ArrayList<>();
        return alphaBeta(context);
    }

    private int alphaBeta(SearchContext context) throws InterruptedException {
        assert context.history.lastHash() == context.state.hash();
        nodes++;
        context.isRootNode = startPly == context.ply();
        context.isPvNode = context.alpha + 1 < context.beta;
        if (context.isPvNode) {
            selDepth = Math.max(selDepth, context.ply());
        }
        moveGen.generateLegalMoves(context.state, context.moves::add);
        if (isTerminal(context)) {
            return context.alpha;
        }
        if (loadTable(context)) {
            return context.alpha;
        }
        if (depthReductions(context)) {
            return context.alpha;
        }
        if (context.depth >= 4 && Thread.interrupted()) {
            throw new InterruptedException();
        }
        searchChilds(context);
        storeTable(context);
        return context.alpha;
    }

    private boolean isTerminal(SearchContext context) {
        if (context.moves.isEmpty()) {
            assert context.ply() != startPly;
            if (moveGen.findOpponentCheckers(context.state) != 0) {
                context.alpha = clamp(Scores.mateLossScore(context.ply()), context.alpha, context.beta);
                return true;
            }
            context.alpha = clamp(0, context.alpha, context.beta);
            return true;
        }
        if (context.history.isDraw(context.state.fiftyMovesCounter)) {
            assert context.ply() != startPly;
            context.alpha = clamp(0, context.alpha, context.beta);
            return true;
        }
        if (!context.isRootNode && insufficientMatingMaterial(context.state)) {
            assert context.ply() != startPly;
            context.alpha = clamp(0, context.alpha, context.beta);
            return true;
        }
        return false;
    }

    private boolean loadTable(SearchContext context) {
        long hash = context.state.hash();
        TableEntry entry = new TableEntry();
        if (table.load(hash, entry)) {
            switch (unpackBounds(entry.raw)) {
                case UPPER_BOUND: {
                    int entryDepth = unpackDepth(entry.raw);
                    if (!context.isRootNode && entryDepth >= context.depth) {
                        int score = unpackScore(entry.raw, context.ply());
                        if (score < context.beta) {
                            if (score <= context.alpha) {
                                assert context.ply() != startPly;
                                return true;
                            }
                            context.beta = score;
                        }
                    }
                    context.hashMove = unpackMove(entry.raw);
                    break;
                }
                case LOWER_BOUND: {
                    int entryDepth = unpackDepth(entry.raw);
                    if (!context.isRootNode && entryDepth >= context.depth) {
                        int score = unpackScore(entry.raw, context.ply());
                        if (score > context.alpha) {
                            if (score >= context.beta) {
                                assert context.ply() != startPly;
                                context.alpha = context.beta;
                                return true;
                            }
                            context.alpha = score;
                        }
                    }
                    context.hashMove = unpackMove(entry.raw);
                    assert context.hashMove != null;
                    break;
                }
                case EXACT_BOUND: {
                    int entryDepth = unpackDepth(entry.raw);
                    if (!context.isRootNode && entryDepth >= context.depth) {
                        context.alpha = clamp(unpackScore(entry.raw, context.ply()), context.alpha, context.beta);
                        return true;
                    }
                    context.hashMove = unpackMove(entry.raw);
                    assert context.hashMove != null;
                    break;
                }
                default:
                    // do nothing
                    break;
            }
        }
        return false;
    }

    private boolean depthReductions(SearchContext context) {
        if (INTERNAL_ITERATIVE_REDUCTIONS && !context.isPvNode && context.hashMove == null) {
            context.depth--;
        }
        if (context.depth <= 0) {
            assert context.ply() != startPly;
            context.alpha = clamp(eval.evaluate(context.state) + context.moves.size(), context.alpha, context.beta);
            return true;
        }
        return false;
    }

    private void searchChilds(SearchContext context) throws InterruptedException {
        context.moves.sort(new SimpleMoveComparator(context.state, context.hashMove));
        context.bounds = UPPER_BOUND;
        context.bestMove = context.hashMove;
        State child = new State(context.state.zobrist);
        for (int moveIndex = 0; moveIndex < context.moves.size(); moveIndex++) {
            Move move = context.moves.get(moveIndex);
            child.copyFrom(context.state);
            move.applyTo(child);
            context.history.add(child.hash());
            int score;
            if (PRINCIPAL_VARIATION_SEARCH && context.isPvNode && context.bounds != UPPER_BOUND) {
                score = -alphaBeta(child, context.history, context.depth - 1, -context.alpha - 1, -context.alpha);
                if (context.alpha < score) {
                    score = -alphaBeta(child, context.history, context.depth - 1, -context.beta, -context.alpha);
                }
            } else {
                score = -alphaBeta(child, context.history, context.depth - 1, -context.beta, -context.alpha);
            }
            context.history.removeLast();
            assert context.alpha <= score && score <= context.beta;
            if (score > context.alpha) {
                if (score >= context.beta) {
                    context.alpha = context.beta;
                    context.bounds = LOWER_BOUND;
                    context.bestMove = move;
                    break;
                }
                context.alpha = score;
                context.bounds = EXACT_BOUND;
                context.bestMove = move;
            }
        }
        assert context.ply() != startPly || context.bounds != UPPER_BOUND;
    }

    private void storeTable(SearchContext context) {
        assert context.ply() != startPly || context.bounds != UPPER_BOUND;
        TableEntry entry = new TableEntry();
        entry.raw = packRaw(context.depth, context.alpha, context.bounds, context.bestMove, context.ply());
        table.store(context.state.hash(), entry);
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

    private long packRaw(int depth, int score, int bounds, Move move, int ply) {
        short tableScore = Scores.toTableScore(score, ply);
        assert Scores.fromTableScore(tableScore, ply) == score;
        assert (bounds & 0xf) == bounds;
        assert (depth & 0xfff) == depth;
        long raw = ((tableScore & 0xffffL) << 16) | ((bounds & 0xfL) << 12) | (depth & 0xfffL);
        if (move != null) {
            raw |= (Move.pack32(move) & 0xffffffffL) << 32;
            assert unpackMove(raw).equals(move);
        }
        assert unpackBounds(raw) == bounds;
        assert unpackDepth(raw) == depth;
        assert unpackScore(raw, ply) == score;
        return raw;
    }

    private int unpackDepth(long raw) {
        return (short) raw & 0xfff;
    }

    private int unpackScore(long raw, int ply) {
        return Scores.fromTableScore((short) (raw >>> 16), ply);
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

    public Table getTable() {
        return table;
    }
}
