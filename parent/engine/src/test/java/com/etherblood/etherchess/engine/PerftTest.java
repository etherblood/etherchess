package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.table.AlwaysReplaceTable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PerftTest {

    private final Perft perft = new Perft(new AlwaysReplaceTable(24));

    @Test
    public void perftFile() throws IOException {
        // https://github.com/elcabesa/vajolet/blob/master/tests/perft.txt

        long maxPerft = 10_000L;// limit count so tests finish reasonably fast
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("perft.txt")) {
            Scanner scanner = new Scanner(is);
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                for (int depth = 1; depth < parts.length; depth++) {
                    String fen = parts[0];
                    long count = Long.parseLong(parts[depth]);
                    if (count <= maxPerft) {
                        assertPerft(fen, depth, count);
                    }
                }
            }
        }
    }

    @Test
    public void perft1() {
        // https://gist.github.com/peterellisjones/8c46c28141c162d1d8a0f0badbc9cff9
        assertPerft("r6r/1b2k1bq/8/8/7B/8/8/R3K2R b QK - 3 2", 1, 8);
    }

    @Test
    public void perft2() {
        // https://gist.github.com/peterellisjones/8c46c28141c162d1d8a0f0badbc9cff9
        assertPerft("8/8/8/2k5/2pP4/8/B7/4K3 b - d3 5 3", 1, 8);
    }

    @Test
    public void perft3() {
        // https://gist.github.com/peterellisjones/8c46c28141c162d1d8a0f0badbc9cff9
        assertPerft("r1bqkbnr/pppppppp/n7/8/8/P7/1PPPPPPP/RNBQKBNR w QqKk - 2 2", 1, 19);
    }

    @Test
    public void perft4() {
        // https://gist.github.com/peterellisjones/8c46c28141c162d1d8a0f0badbc9cff9
        assertPerft("r3k2r/p1pp1pb1/bn2Qnp1/2qPN3/1p2P3/2N5/PPPBBPPP/R3K2R b QqKk - 3 2", 1, 5);
    }

    @Test
    public void perft5() {
        // https://gist.github.com/peterellisjones/8c46c28141c162d1d8a0f0badbc9cff9
        assertPerft("2kr3r/p1ppqpb1/bn2Qnp1/3PN3/1p2P3/2N5/PPPBBPPP/R3K2R b QK - 3 2", 1, 44);
    }

    @Test
    public void perft6() {
        // https://gist.github.com/peterellisjones/8c46c28141c162d1d8a0f0badbc9cff9
        assertPerft("rnb2k1r/pp1Pbppp/2p5/q7/2B5/8/PPPQNnPP/RNB1K2R w QK - 3 9", 1, 39);
    }

    @Test
    public void perft7() {
        // https://gist.github.com/peterellisjones/8c46c28141c162d1d8a0f0badbc9cff9
        assertPerft("2r5/3pk3/8/2P5/8/2K5/8/8 w - - 5 4", 1, 9);
    }

    @Test
    public void startPosition() {
        // https://www.chessprogramming.org/Perft_Results
        String startPositionFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        assertPerft(startPositionFen, 1, 20);
        assertPerft(startPositionFen, 2, 400);
        assertPerft(startPositionFen, 3, 8902);
        assertPerft(startPositionFen, 4, 197281);
        assertPerft(startPositionFen, 5, 4865609);
//        assertPerft(startPositionFen, 6, 119060324);
    }

    @Test
    public void kiwipetePosition() {
        // https://www.chessprogramming.org/Perft_Results
        String kiwipeteFen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
        assertPerft(kiwipeteFen, 1, 48);
        assertPerft(kiwipeteFen, 2, 2039);
        assertPerft(kiwipeteFen, 3, 97862);
        assertPerft(kiwipeteFen, 4, 4085603);
//        assertPerft(kiwipeteFen, 5, 193690690);
//        assertPerft(kiwipeteFen, 6, 8031647685L);
    }

    @Test
    public void position3() {
        // https://www.chessprogramming.org/Perft_Results
        String fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";
        assertPerft(fen, 1, 14);
        assertPerft(fen, 2, 191);
        assertPerft(fen, 3, 2812);
        assertPerft(fen, 4, 43238);
        assertPerft(fen, 5, 674624);
//        assertPerft(fen, 6, 11030083);
//        assertPerft(fen, 7, 178633661);
//        assertPerft(fen, 8, 3009794393L);
    }

    @Test
    public void position4() {
        // https://www.chessprogramming.org/Perft_Results
        String fen = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
        assertPerft(fen, 1, 6);
        assertPerft(fen, 2, 264);
        assertPerft(fen, 3, 9467);
        assertPerft(fen, 4, 422333);
//        assertPerft(fen, 5, 15833292);
//        assertPerft(fen, 6, 706045033);
    }

    @Test
    public void position5() {
        // https://www.chessprogramming.org/Perft_Results
        String fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        assertPerft(fen, 1, 44);
        assertPerft(fen, 2, 1486);
        assertPerft(fen, 3, 62379);
        assertPerft(fen, 4, 2103487);
//        assertPerft(fen, 5, 89941194);
    }

    @Test
    public void position6() {
        // https://www.chessprogramming.org/Perft_Results
        String fen = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10";
        assertPerft(fen, 1, 46);
        assertPerft(fen, 2, 2079);
        assertPerft(fen, 3, 89890);
        assertPerft(fen, 4, 3894594);
//        assertPerft(fen, 5, 164075551);
//        assertPerft(fen, 6, 6923051137L);
//        assertPerft(fen, 7, 287188994746L);
//        assertPerft(fen, 8, 11923589843526L);
//        assertPerft(fen, 9, 490154852788714L);
    }

    @Test
    public void illegalEp1() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("3k4/3p4/8/K1P4r/8/8/8/8 b - - 0 1", 6, 1134888);
    }

    @Test
    public void illegalEp2() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("8/8/4k3/8/2p5/8/B2P2K1/8 w - - 0 1", 6, 1015133);
    }

    @Test
    public void epCheck() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1", 6, 1440467);
    }

    @Test
    public void shortCastleCheck() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("5k2/8/8/8/8/8/8/4K2R w K - 0 1", 6, 661072);
    }

    @Test
    public void longCastleCheck() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("3k4/8/8/8/8/8/8/R3K3 w Q - 0 1", 6, 803711);
    }

    @Test
    public void castleRights() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1", 4, 1274206);
    }

    @Test
    public void castlePrevented() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1", 4, 1720476);
    }

    @Test
    public void promoteOutOfCheck() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1", 6, 3821001);
    }

    @Test
    public void discoveredCheck() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1", 5, 1004658);
    }

    @Test
    public void promoteGiveCheck() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("4k3/1P6/8/8/8/8/K7/8 w - - 0 1", 6, 217342);
    }

    @Test
    public void underpromoteGiveCheck() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("8/P1k5/K7/8/8/8/8/8 w - - 0 1", 6, 92683);
    }

    @Test
    public void selfStalemate() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("K1k5/8/P7/8/8/8/8/8 w - - 0 1", 6, 2217);
    }

    @Test
    public void stalemateAndCheckmate1() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("8/k1P5/8/1K6/8/8/8/8 w - - 0 1", 7, 567584);
    }

    @Test
    public void stalemateAndCheckmate2() {
        // https://www.madchess.net/wp-content/uploads/tests/TestPositions.txt
        assertPerft("8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1", 4, 23527);
    }

    @Test
    public void pawns1() {
        // https://sites.google.com/site/numptychess/perft/position-2
        String fen = "8/p7/8/1P6/K1k3p1/6P1/7P/8 w - -";
        assertPerft(fen, 1, 5);
        assertPerft(fen, 2, 39);
        assertPerft(fen, 3, 237);
        assertPerft(fen, 4, 2002);
        assertPerft(fen, 5, 14062);
        assertPerft(fen, 6, 120995);
        assertPerft(fen, 7, 966152);
//        assertPerft(fen, 8, 8103790);
    }

    @Test
    public void pawns2() {
        // https://sites.google.com/site/numptychess/perft/position-4
        String fen = "8/5p2/8/2k3P1/p3K3/8/1P6/8 b - -";
        assertPerft(fen, 1, 9);
        assertPerft(fen, 2, 85);
        assertPerft(fen, 3, 795);
        assertPerft(fen, 4, 7658);
        assertPerft(fen, 5, 72120); // website has wrong value of '72170' here
        assertPerft(fen, 6, 703851);
//        assertPerft(fen, 7, 6627106);
//        assertPerft(fen, 8, 64451405);
    }

    @Test
    public void position7() {
        String fen = "r3k2r/pb3p2/5npp/n2p4/1p1PPB2/6P1/P2N1PBP/R3K2R b KQkq -";
        // https://sites.google.com/site/numptychess/perft/position-5
        assertPerft(fen, 1, 29);
        assertPerft(fen, 2, 953);
        assertPerft(fen, 3, 27990);
        assertPerft(fen, 4, 909807);
//        assertPerft(fen, 5, 26957954);
    }

    @Test
    public void castlings() {
        // https://sites.google.com/site/numptychess/perft/position-3
        String fen = "r3k2r/p6p/8/B7/1pp1p3/3b4/P6P/R3K2R w KQkq -";
        assertPerft(fen, 1, 17);
        assertPerft(fen, 2, 341);
        assertPerft(fen, 3, 6666);
        assertPerft(fen, 4, 150072);
        assertPerft(fen, 5, 3186478);
//        assertPerft(fen, 6, 77054993);
    }

    @Test
    public void enPassant() {
        // http://talkchess.com/forum3/viewtopic.php?t=40917
        String fen = "8/3k4/8/4b3/1p1p1p2/8/1PPRP2K/8 w - -";
        assertPerft(fen, 1, 12);
        assertPerft(fen, 2, 197);
        assertPerft(fen, 3, 2696);
        assertPerft(fen, 4, 41622);
    }

    @Test
    public void position8() {
        // http://talkchess.com/forum3/viewtopic.php?t=40917
        String fen = "8/4rP2/8/8/4pk2/8/3P2PP/4K2R w K -";
        assertPerft(fen, 1, 17);
        assertPerft(fen, 2, 182);
        assertPerft(fen, 3, 3232);
        assertPerft(fen, 4, 42552);
    }

    @Test
    public void position9() {
        // http://talkchess.com/forum3/viewtopic.php?t=40917
        String fen = "8/4rP2/8/8/4pk2/8/3P2PP/4K2R w K -";
        assertPerft(fen, 1, 17);
        assertPerft(fen, 2, 182);
        assertPerft(fen, 3, 3232);
        assertPerft(fen, 4, 42552);
    }

    @Test
    public void pinnedEnPassant() {
        String fen = "8/3k4/8/4b3/1pPp1p2/8/1P1RP2K/8 b - c3 0 1";
        assertPerft(fen, 1, 18);
    }

    private void assertPerft(String fen, int depth, long count) {
        Assertions.assertEquals(count, perft.perft(fen, depth));
    }
}
