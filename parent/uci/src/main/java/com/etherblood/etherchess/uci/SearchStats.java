package com.etherblood.etherchess.uci;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SearchStats {

    public Integer depth;
    public Integer seldepth;
    public Integer scoreCp;
    public Integer scoreMate;
    public Integer hashPermill;
    public Long nodes;
    public Long millis;
    public List<String> pv;
}
