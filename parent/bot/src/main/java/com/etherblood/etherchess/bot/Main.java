package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.engine.FenConverter;
import com.etherblood.etherchess.engine.MirrorZobrist;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.AlwaysReplaceTable;
import java.util.Random;

public class Main {
    public static void main(String... args) {
        BotImpl bot = new BotImpl(new AlwaysReplaceTable(25), new WoodCount(), new MoveGenerator());
        State state = new State(new MirrorZobrist(new Random(12)::nextLong));
        new FenConverter().fromFen(state, FenConverter.DEFAULT_STARTPOSITION);

        long startNanos = System.nanoTime();
        Move move = bot.findBest(state, 7);
        long durationNanos = System.nanoTime() - startNanos;
        long durationMillis = durationNanos / 1_000_000;
        System.out.println("in " + durationMillis + " ms");
        System.out.println(move);
    }
}
