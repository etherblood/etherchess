package com.etherblood.etherchess.uci;

public class Main {
    public static void main(String... args) {
        Uci uci = new Uci(System.in, System.out);
        uci.run();
    }

}
