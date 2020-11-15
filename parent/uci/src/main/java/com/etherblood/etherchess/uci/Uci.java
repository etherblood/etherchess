package com.etherblood.etherchess.uci;

import com.etherblood.etherchess.bot.BotImpl;
import com.etherblood.etherchess.bot.HashHistory;
import com.etherblood.etherchess.bot.SearchResult;
import com.etherblood.etherchess.bot.SearchStats;
import com.etherblood.etherchess.bot.evaluation.PieceSquareEvaluation;
import com.etherblood.etherchess.engine.FenConverter;
import com.etherblood.etherchess.engine.MirrorZobrist;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.AlwaysReplaceTable;
import com.etherblood.etherchess.engine.util.LongAlgebraicNotation;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class Uci {
    // https://www.shredderchess.com/chess-features/uci-universal-chess-interface.html

    public static final int DEFAULT_HASH_MIB = 64;
    public static final int ENTRIES_PER_MIB = (1024 * 1024) / AlwaysReplaceTable.ENTRY_BYTES;

    private final InputStream in;
    private final PrintStream out;
    private BotImpl bot;
    private State state;
    private HashHistory history;
    private boolean isDebug = false;
    private final AtomicReference<Thread> botThread = new AtomicReference<>(null);

    public Uci(InputStream in, PrintStream out) {
        this.in = in;
        this.out = out;
        setTableSize(DEFAULT_HASH_MIB);
    }

    private void setTableSize(int mib) {
        assert botThread.get() == null;
        bot = new BotImpl(new AlwaysReplaceTable(Long.numberOfTrailingZeros(mib * ENTRIES_PER_MIB)), new PieceSquareEvaluation(), new MoveGenerator());
    }

    public void run() {
        debugLog("STARTED");
        Scanner scanner = new Scanner(in);
        while (true) {
            try {
                String line = scanner.nextLine();
                List<String> tokens = tokenize(line);
                Iterator<String> it = tokens.iterator();
                if (it.hasNext()) {
                    switch (it.next()) {
                        case "quit":
                            return;
                        case "stop":
                            Thread thread = botThread.get();
                            if (thread != null) {
                                thread.interrupt();
                                botThread.compareAndSet(thread, null);
                            }
                            break;
                        case "uci":
                            send("id name Etherchess 0.1.0");
                            send("id author Etherblood");
                            send("option name Hash type spin default " + DEFAULT_HASH_MIB);
                            send("uciok");
                            break;
                        case "isready":
                            send("readyok");
                            break;
                        case "setoption":
                            setOption(it);
                            break;
                        case "ucinewgame":
                            break;
                        case "position":
                            position(it);
                            break;
                        case "go":
                            go(it);
                            break;
                        case "debug":
                            isDebug = "on".equals(it.next());
                            break;
                    }
                }
                debugLog("");
                out.flush();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String sStackTrace = sw.toString();
                debugLog(sStackTrace);
                if (state != null) {
                    debugLog(state.toBoardString());
                }
                out.flush();
                throw e;
            }
        }
    }

    private void debugLog(String log) {
        if (isDebug) {
            send("info string " + log);
        }
    }

    private synchronized void send(String line) {
        out.println(line);
    }

    private void setOption(Iterator<String> it) {
        String token = it.next();
        if (!"name".equals(token)) {
            throw new IllegalStateException();
        }
        token = it.next();
        switch (token) {
            case "Hash":
                token = it.next();
                if (!"value".equals(token)) {
                    throw new IllegalStateException();
                }
                int mib = Integer.parseInt(it.next());
                setTableSize(mib);
                break;
        }
    }

    private void position(Iterator<String> it) {
        String fen;
        String token = it.next();
        switch (token) {
            case "fen":
                fen = it.next();
                break;
            case "startpos":
                fen = FenConverter.DEFAULT_STARTPOSITION;
                break;
            default:
                throw new AssertionError(token);
        }
        state = new State(new MirrorZobrist(new Random(13)::nextLong));
        new FenConverter().fromFen(state, fen);
        history = new HashHistory(state.hash());
        if (it.hasNext()) {
            token = it.next();
            if (!"moves".equals(token)) {
                throw new IllegalStateException();
            }
            while (it.hasNext()) {
                Move move = LongAlgebraicNotation.parseLanString(state, it.next());
                move.applyTo(state);
                history.add(state.hash());
            }
        }
    }

    private void go(Iterator<String> it) {
        int depth = 5;
        switch (it.next()) {
            case "depth":
                depth = Integer.parseInt(it.next());
                break;
        }
        int botDepth = depth;
        State botState = state;
        HashHistory botHistory = history;
        boolean white = botState.isWhite;
        Thread thread = new Thread(() -> {
            bot.findBest(botState, botHistory, botDepth, new SearchResult() {

                @Override
                public void stats(SearchStats stats) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("info score");
                    if (stats.depth != null) {
                        builder.append(" depth " + stats.depth);
                        if (stats.seldepth != null) {
                            builder.append(" seldepth " + stats.seldepth);
                        }
                    }
                    if (stats.scoreCp != null) {
                        builder.append(" score cp " + stats.scoreCp);
                    } else if (stats.scoreMate != null) {
                        builder.append(" score mate " + stats.scoreMate);
                    }
                    if (stats.nodes != null) {
                        builder.append(" nodes " + stats.nodes);
                    }
                    if (stats.nodes != null && stats.millis != null && stats.millis > 0) {
                        builder.append(" nps " + (1000 * stats.nodes / stats.millis));
                    }
                    if (stats.pv != null) {
                        if (stats.millis != null) {
                            builder.append(" time " + stats.millis);
                        }
                        builder.append(" pv");
                        for (int i = 0; i < stats.pv.size(); i++) {
                            boolean isWhite = white == ((i & 1) == 0);
                            builder.append(' ');
                            builder.append(LongAlgebraicNotation.toLanString(isWhite, stats.pv.get(i)));
                        }
                    }
                    send(builder.toString());
                }

                @Override
                public void bestMove(Move move) {
                    send("bestmove " + LongAlgebraicNotation.toLanString(white, move));
                }

                @Override
                public void string(String string) {
                    send("info string " + string);
                }
            });
            botThread.set(null);
        });
        botThread.set(thread);
        thread.start();
        state = null;
        history = null;
    }

    private List<String> tokenize(String line) {
        List<String> result = new ArrayList<>();
        for (String token : line.split(" ")) {
            if (!token.trim().isEmpty()) {
                result.add(token.trim());
            }
        }
        return result;
    }
}
