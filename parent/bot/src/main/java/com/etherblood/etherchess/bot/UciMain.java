package com.etherblood.etherchess.bot;

import com.etherblood.etherchess.uci.Uci;
import java.util.Scanner;

public class UciMain {
    public static void main(String... args) {
        Uci uci = new Uci(new UciEngineWrapper(), new Scanner(System.in)::nextLine, System.out::println);
        uci.run();
    }
}
