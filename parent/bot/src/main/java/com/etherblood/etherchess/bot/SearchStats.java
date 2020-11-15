package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.engine.Move;
import java.util.List;

public class SearchStats {

    public Integer depth;
    public Integer seldepth;
    public Integer scoreCp;
    public Integer scoreMate;
    public Long nodes;
    public Long millis;
    public List<Move> pv;
}
