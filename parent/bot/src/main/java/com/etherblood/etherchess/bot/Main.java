package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.bot.evaluation.PieceSquareEvaluation;
import com.etherblood.etherchess.engine.FenConverter;
import com.etherblood.etherchess.engine.MirrorZobrist;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.AlwaysReplaceTable;
import java.util.Random;

public class Main {
    public static void main(String... args) {
        String fen = FenConverter.DEFAULT_STARTPOSITION;
        int depth = 9;

        AlwaysReplaceTable table = new AlwaysReplaceTable(25);
        BotImpl bot = new BotImpl(table, new PieceSquareEvaluation(), new MoveGenerator());
        State state = new State(new MirrorZobrist(new Random(12)::nextLong));
        new FenConverter().fromFen(state, fen);
        HashHistory history = new HashHistory(state.hash());

        System.out.println(state.toBoardString());
        Move move = bot.findBest(state, history, depth, new SearchResultPrinter());
        table.printStats();
    }
}
