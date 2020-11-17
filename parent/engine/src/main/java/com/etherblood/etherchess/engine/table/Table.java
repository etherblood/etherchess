package com.etherblood.etherchess.engine.table;

public interface Table {

    boolean load(long hash, TableEntry entry);

    void store(long hash, TableEntry entry);

    void remove(long hash);

    void clear();

    int fillPermill();
}
