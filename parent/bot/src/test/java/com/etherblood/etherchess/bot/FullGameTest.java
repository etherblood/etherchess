package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.bot.evaluation.PieceSquareEvaluation;
import com.etherblood.etherchess.engine.FenConverter;
import com.etherblood.etherchess.engine.MirrorZobrist;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.AlwaysReplaceTable;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class FullGameTest {

    @Test
    public void playFullGame() {
        int depth = 4;
        MoveGenerator moveGen = new MoveGenerator();
        State state = new State(new MirrorZobrist(new Random(12354)::nextLong));
        BotImpl bot = new BotImpl(new AlwaysReplaceTable(22), new PieceSquareEvaluation(), moveGen);

        new FenConverter().fromFen(state, FenConverter.DEFAULT_STARTPOSITION);
        HashHistory history = new HashHistory(state.hash());
        while (!history.isDraw(state.fiftyMovesCounter) && !moveGen.generateLegalMoves(state).isEmpty()) {
            Move best = bot.findBest(state, history, depth, new NoopSearchResult());
            best.applyTo(state);
            history.add(state.hash());
        }
        ((AlwaysReplaceTable) bot.getTable()).printStats(System.out);
    }
}
