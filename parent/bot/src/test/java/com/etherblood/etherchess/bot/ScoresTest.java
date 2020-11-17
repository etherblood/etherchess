package com.etherblood.etherchess.bot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScoresTest {

    @Test
    public void mateScores() {
        int matePly = 10;
        int score = Scores.mateScore(matePly);
        int ply = 5;
        short tableScore = Scores.toTableScore(score, ply);
        ply++;
        int newScore = Scores.fromTableScore(tableScore, ply);
        Assertions.assertEquals(score - 1, newScore);
    }

    @Test
    public void matedScores() {
        int matePly = 10;
        int score = -Scores.mateScore(matePly);
        int ply = 5;
        short tableScore = Scores.toTableScore(score, ply);
        ply++;
        int newScore = Scores.fromTableScore(tableScore, ply);
        Assertions.assertEquals(score + 1, newScore);
    }

    @Test
    public void matePly() {
        int matePly = 10;
        int ply = Scores.plyFromMateScore(Scores.mateScore(matePly));
        Assertions.assertEquals(matePly, ply);
    }

    @Test
    public void matedPly() {
        int matePly = 10;
        int ply = Scores.plyFromMateScore(-Scores.mateScore(matePly));
        Assertions.assertEquals(matePly, ply);
    }
}
