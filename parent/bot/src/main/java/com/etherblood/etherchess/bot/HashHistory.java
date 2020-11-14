package com.etherblood.etherchess.bot;

import java.util.Arrays;

public class HashHistory {
    private int next;
    private long[] hashes = new long[64];

    public HashHistory(long initialHash) {
        add(initialHash);
    }

    public void add(long hash) {
        if (next == hashes.length) {
            hashes = Arrays.copyOf(hashes, 2 * hashes.length);
        }
        hashes[next++] = hash;
    }

    public boolean isDraw(int fiftyCounter) {
        if (fiftyCounter >= 100) {
            return true;
        }
        long hash = lastHash();
        int limit = next - fiftyCounter;
        for (int i = next - 5; i >= limit; i -= 2) {
            if (hashes[i] == hash) {
                return true;
            }
        }
        return false;
    }

    public void removeLast() {
        next--;
    }

    public long lastHash() {
        return hashes[next - 1];
    }

    public int size() {
        return next;
    }
}
