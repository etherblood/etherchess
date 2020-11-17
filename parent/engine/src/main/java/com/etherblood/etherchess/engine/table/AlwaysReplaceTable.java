package com.etherblood.etherchess.engine.table;

import com.etherblood.etherchess.engine.stats.StatUtil;
import java.io.PrintStream;
import java.util.Arrays;

public class AlwaysReplaceTable implements Table {

    public static final int ENTRY_BYTES = 2 * Long.BYTES;
    public static final long EMPTY_RAW = 0;

    private final long[] table;
    private final int indexMask;
    private long hits, misses, stores, overwrites;

    public AlwaysReplaceTable(int logSize) {
        this.table = new long[2 << logSize];
        this.indexMask = (2 << logSize) - 2;
        clear();
    }

    @Override
    public boolean load(long hash, TableEntry entry) {
        int index = indexMask & (int) hash;
        if (table[index] == hash) {
            hits++;
            entry.raw = table[index + 1];
            return true;
        }
        misses++;
        return false;
    }

    @Override
    public void store(long hash, TableEntry entry) {
        assert entry.raw != EMPTY_RAW;
        int index = indexMask & (int) hash;
        if (table[index + 1] != EMPTY_RAW) {
            overwrites++;
        }
        table[index] = hash;
        table[index + 1] = entry.raw;
        stores++;
    }

    @Override
    public void remove(long hash) {
        int index = indexMask & (int) hash;
        if (table[index] == hash) {
            table[index] = ~hash;
            table[index + 1] = EMPTY_RAW;
        }
    }

    public void printStats(PrintStream out) {
        long size = table.length / 2;
        out.println("Table stats");
        out.println(" size: " + size + " (" + StatUtil.humanReadableByteCountBin(size * ENTRY_BYTES) + ")");
        long full = stores - overwrites;
        long empty = size - full;
        out.println("  empty: " + empty + " (" + StatUtil.toPercentage(empty, size, 1) + ")");
        out.println("  full: " + full + " (" + StatUtil.toPercentage(full, size, 1) + ")");
        out.println(" hits: " + hits + " (" + StatUtil.toPercentage(hits, hits + misses, 1) + ")");
        out.println(" misses: " + misses + " (" + StatUtil.toPercentage(misses, hits + misses, 1) + ")");
        out.println(" loads: " + (hits + misses));
        out.println(" stores: " + stores);
        out.println(" overwrites: " + overwrites);
        out.println(" stores/size: " + StatUtil.toPercentage(stores, size, 1));
    }

    @Override
    public final void clear() {
        Arrays.fill(table, EMPTY_RAW);
        hits = 0;
        misses = 0;
        stores = 0;
    }

    @Override
    public int fillPermill() {
        long size = table.length / 2;
        long full = stores - overwrites;
        return (int) (1000 * full / size);
    }
}
