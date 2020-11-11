package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.Square;
import java.util.function.LongSupplier;

public class MirrorZobrist {

    private final long[][] pieceSquareHashes = new long[6][64];
    private final long castlingHash;
    private final long enPassantHash;

    public MirrorZobrist(LongSupplier random) {
        for (long[] hashes : pieceSquareHashes) {
            for (int i = 0; i < hashes.length; i++) {
                hashes[i] = random.getAsLong();
            }
        }
        castlingHash = random.getAsLong();
        enPassantHash = random.getAsLong();
    }

    public static long mirror(long hash) {
        return Long.reverseBytes(hash);
    }

    public long metaHash(int enPassant, int castling) {
        long hash = Long.rotateLeft(castlingHash, castling);
        if (enPassant != State.NO_EN_PASSANT) {
            hash ^= Long.rotateLeft(enPassantHash, enPassant);
        }
        return hash;
    }

    public long pieceHash(boolean isOwn, int piece, int square) {
        long[] squareHashes = pieceSquareHashes[piece - Piece.PAWN];
        return squareHash(squareHashes, isOwn, square);
    }

    private static long squareHash(long[] squareHashes, boolean isOwn, int square) {
        if (isOwn) {
            return squareHashes[square];
        }
        return mirror(squareHashes[Square.mirrorY(square)]);
    }
}
