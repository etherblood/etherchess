package com.etherblood.etherchess.uci;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class UciTest {

    private Consumer<String> toEngine;
    private Supplier<String> fromEngine;
    private Thread thread;
    private List<Throwable> exceptions;
    private UciEngine engine;

    @BeforeEach
    public void init() {
        exceptions = new ArrayList<>();

        BlockingQueue<String> in = new LinkedBlockingQueue<>();
        BlockingQueue<String> out = new LinkedBlockingQueue<>();

        engine = Mockito.mock(UciEngine.class);
        Uci uci = new Uci(engine, asSupply(in), asConsumer(out));
        thread = new Thread(uci);
        thread.start();

        toEngine = asConsumer(in);
        fromEngine = asSupply(out);
    }

    @AfterEach
    public void cleanup() throws Throwable {
        thread.join(100);
        for (Throwable e : exceptions) {
            throw e;
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    public void handshake() {
        String name = "mock engine";
        String author = "mock author";
        Mockito.when(engine.getName()).thenReturn(name);
        Mockito.when(engine.getAuthor()).thenReturn(author);

        toEngine.accept("uci");
        Assertions.assertEquals("id name " + name, fromEngine.get());
        Assertions.assertEquals("id author " + author, fromEngine.get());
        Assertions.assertEquals("uciok", fromEngine.get());

        toEngine.accept("isready");
        Assertions.assertEquals("readyok", fromEngine.get());

        toEngine.accept("quit");
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    public void isReadyWhileThinking() {
        int firstDepth = 1;
        int secondDepth = 2;
        String bestMove = "a1a2";
        Mockito.doAnswer((Answer<Object>) invocationOnMock -> {
            SearchResult result = invocationOnMock.getArgument(1);
            result.stats(SearchStats.builder().depth(firstDepth).build());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // do nothing
            }
            result.stats(SearchStats.builder().depth(secondDepth).build());
            result.bestMove(bestMove);
            return null;
        }).when(engine).think(Mockito.any(), Mockito.any());

        toEngine.accept("position startpos");
        toEngine.accept("isready");
        Assertions.assertEquals("readyok", fromEngine.get());
        toEngine.accept("go depth 1000");
        Assertions.assertEquals("info depth " + firstDepth, fromEngine.get());
        toEngine.accept("isready");
        Assertions.assertEquals("readyok", fromEngine.get());
        toEngine.accept("stop");
        Assertions.assertEquals("info depth " + secondDepth, fromEngine.get());
        Assertions.assertEquals("bestmove " + bestMove, fromEngine.get());
        toEngine.accept("quit");
    }

    private Supplier<String> asSupply(BlockingQueue<String> queue) {
        return () -> {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private Consumer<String> asConsumer(BlockingQueue<String> queue) {
        return queue::add;
    }

}
