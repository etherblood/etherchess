package com.etherblood.etherchess.engine.table;

public class NoopTable implements Table {
    @Override
    public boolean load(long hash, TableEntry entry) {
        return false;
    }

    @Override
    public void store(long hash, TableEntry entry) {
        // do nothing
    }

    @Override
    public void remove(long hash) {
        // do nothing
    }

    @Override
    public void clear() {
        // do nothing
    }

    @Override
    public int fillPermill() {
        return 0;
    }
}
