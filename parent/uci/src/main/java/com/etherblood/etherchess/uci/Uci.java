package com.etherblood.etherchess.uci;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Uci implements Runnable {
    // https://www.shredderchess.com/chess-features/uci-universal-chess-interface.html

    private static final Logger LOG = LoggerFactory.getLogger(Uci.class);

    public static final int DEFAULT_HASH_MIB = 64;
    public static final int MIN_HASH_MIB = 0;
    public static final int MAX_HASH_MIB = 512;

    private final UciEngine engine;
    private final Supplier<String> in;
    private final Consumer<String> out;
    private final AtomicReference<Thread> botThread = new AtomicReference<>(null);

    public Uci(UciEngine engine, Supplier<String> in, Consumer<String> out) {
        this.engine = engine;
        this.in = in;
        this.out = out;
        setTableSize(DEFAULT_HASH_MIB);
    }

    private void setTableSize(int mib) {
        assert botThread.get() == null;
        engine.setTableSize(mib);
    }

    @Override
    public void run() {
        LOG.info("started");
        while (true) {
            try {
                String line = in.get();
                LOG.info("> " + line);
                List<String> tokens = tokenize(line);
                Iterator<String> it = tokens.iterator();
                if (it.hasNext()) {
                    switch (it.next()) {
                        case "quit": {
                            stopThread();
                            LOG.info("ended");
                            return;
                        }
                        case "stop": {
                            stopThread();
                            break;
                        }
                        case "uci":
                            send("id name " + engine.getName());
                            send("id author " + engine.getAuthor());
                            // TODO: re-add option support
//                            send("option name Hash type spin default " + DEFAULT_HASH_MIB + " min " + MIN_HASH_MIB + " max " + MAX_HASH_MIB);
                            send("uciok");
                            break;
                        case "isready":
                            send("readyok");
                            break;
                        case "setoption":
                            setOption(it);
                            break;
                        case "ucinewgame":
                            engine.newGame();
                            break;
                        case "position":
                            position(it);
                            break;
                        case "go":
                            go(it);
                            break;
                        case "debug":
                            engine.setDebug("on".equals(it.next()));
                            break;
                    }
                }
            } catch (Throwable e) {
                LOG.error("Error when processing commands.", e);
            }
        }
    }

    private void stopThread() {
        Thread thread = botThread.getAndSet(null);
        if (thread != null) {
            thread.interrupt();
        }
    }

    private synchronized void send(String line) {
        LOG.debug("< " + line);
        out.accept(line);
        LOG.info("< " + line);
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
                fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
                break;
            default:
                throw new AssertionError(token);
        }
        List<String> lanMoves = new ArrayList<>();
        if (it.hasNext()) {
            token = it.next();
            if (!"moves".equals(token)) {
                throw new IllegalStateException();
            }
            while (it.hasNext()) {
                lanMoves.add(it.next());
            }
        }
        engine.setPosition(fen, lanMoves);
    }

    private void go(Iterator<String> it) {
        SearchParams.SearchParamsBuilder builder = SearchParams.builder();
        switch (it.next()) {
            case "depth":
                builder.depth(Integer.parseInt(it.next()));
                break;
        }
        Thread thread = new Thread(() -> {
            try {
                LOG.info("thinking...");
                engine.think(builder.build(), new SearchResult() {

                    @Override
                    public void stats(SearchStats stats) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("info");
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
                                builder.append(' ');
                                builder.append(stats.pv.get(i));
                            }
                        }
                        if (stats.hashPermill != null) {
                            builder.append(" hashfull " + stats.hashPermill);
                        }
                        send(builder.toString());
                    }

                    @Override
                    public void bestMove(String move) {
                        send("bestmove " + move);
                    }

                    @Override
                    public void string(String string) {
                        send("info string " + string);
                    }
                });
                botThread.set(null);
                LOG.info("done thinking.");
            } catch (Throwable e) {
                LOG.error("Error while thinking.", e);
            }
        });
        botThread.set(thread);
        thread.start();
    }

    private List<String> tokenize(String line) {
        return Arrays.asList(line.trim().split("\\s+"));
    }
}
