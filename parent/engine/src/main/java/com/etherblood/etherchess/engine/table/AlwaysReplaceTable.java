package com.etherblood.etherchess.engine.table;

import com.etherblood.etherchess.engine.stats.StatUtil;
import java.util.Arrays;

public class AlwaysReplaceTable implements Table {

    private final long[] table;
    private final int indexMask;
    private long hits, misses, stores;

    public AlwaysReplaceTable(int logSize) {
        this.table = new long[2 << logSize];
        this.indexMask = (2 << logSize) - 2;
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
        int index = indexMask & (int) hash;
        table[index] = hash;
        table[index + 1] = entry.raw;
        stores++;
    }

    public void printStats() {
        long size = 2 * table.length;
        System.out.println(" size: " + size + " - " + StatUtil.humanReadableByteCountBin(size * Integer.BYTES));
        System.out.println(" hits: " + hits + " - " + StatUtil.toPercentage(hits, hits + misses, 1));
        System.out.println(" misses: " + misses + " - " + StatUtil.toPercentage(misses, hits + misses, 1));
        System.out.println(" loads: " + (hits + misses));
        System.out.println(" stores: " + stores);
        System.out.println(" stores/size: " + StatUtil.toPercentage(stores, size, 0) + "%");
    }

    public final void clear() {
        Arrays.fill(table, 0);
        hits = 0;
        misses = 0;
        stores = 0;
    }
}
