package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.bot.evaluation.PieceSquareEvaluation;
import com.etherblood.etherchess.engine.FenConverter;
import com.etherblood.etherchess.engine.MirrorZobrist;
import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.MoveGenerator;
import com.etherblood.etherchess.engine.State;
import com.etherblood.etherchess.engine.table.AlwaysReplaceTable;
import com.etherblood.etherchess.engine.table.Table;
import com.etherblood.etherchess.engine.util.LongAlgebraicNotation;
import com.etherblood.etherchess.uci.SearchParams;
import com.etherblood.etherchess.uci.SearchResult;
import com.etherblood.etherchess.uci.UciEngine;
import java.util.List;
import java.util.Random;

public class UciEngineWrapper implements UciEngine {

    private final State state = new State(new MirrorZobrist(new Random(12354)::nextLong));
    private final HashHistory history = new HashHistory(0);
    private Table table = new AlwaysReplaceTable(0);

    @Override
    public String getName() {
        return "Etherchess 0.1.0";
    }

    @Override
    public String getAuthor() {
        return "Etherblood";
    }

    @Override
    public void newGame() {
        table.clear();
    }

    @Override
    public void setPosition(String fen, List<String> moves) {
        new FenConverter().fromFen(state, fen);
        history.reset(state.hash());
        for (String lan : moves) {
            Move move = LongAlgebraicNotation.parseLanString(state, lan);
            move.applyTo(state);
            history.add(state.hash());
        }
    }

    @Override
    public void go(SearchParams params, SearchResult result) {
        BotImpl bot = new BotImpl(table, new PieceSquareEvaluation(), new MoveGenerator());
        bot.findBest(state, history, params.depth(), result);
    }

    @Override
    public void setTableSize(int mib) {
        long bytes = mib * 1024L * 1024L;
        int entryCount = Math.toIntExact(bytes / AlwaysReplaceTable.ENTRY_BYTES);
        long roundedDownToPowerOfTwo = Long.highestOneBit(entryCount);
        table = new AlwaysReplaceTable(Long.numberOfTrailingZeros(roundedDownToPowerOfTwo));
    }

    @Override
    public void setDebug(boolean value) {

    }
}
