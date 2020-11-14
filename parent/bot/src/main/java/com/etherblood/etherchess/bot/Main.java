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
        int depth = 7;

        BotImpl bot = new BotImpl(new AlwaysReplaceTable(25), new PieceSquareEvaluation(), new MoveGenerator());
        State state = new State(new MirrorZobrist(new Random(12)::nextLong));
        new FenConverter().fromFen(state, fen);
        HashHistory history = new HashHistory(state.hash());

        Move move = bot.findBest(state, history, depth);
        System.out.println(move);
    }
}
