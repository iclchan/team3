package com.cs.tradingapp;

public class TradingApp {

    public static void main(String[] app) {
        System.out.println("----------------------------------------------");
        System.out.println("  _   _ _____ _     ____   _____   __ \n" +
                            " | | | | ____| |   | __ ) / _ \\ \\ / / \n" +
                            " | |_| |  _| | |   |  _ \\| | | \\ V /  \n" +
                            " |  _  | |___| |___| |_) | |_| || |   \n" +
                            " |_| |_|_____|_____|____/ \\___/ |_|   \n" +
                            "                                      ");
        System.out.println("----------------------------------------------");
        
        System.out.println("Initializing variables...");
        DecisionMaker decisionMaker = new DecisionMaker();
        OrderUtil orderUtil = new OrderUtil();
        Market market = null;
        System.out.println("Initializing variables done!");
        
        System.out.println("Start running trading algorithm...");
        while (true) {
            if (market == null) {
                market = TradingAppUtil.getInstrumentData();
            } else {
                //System.out.println("Market change detected!");
                Market newMarket = TradingAppUtil.getInstrumentData();
                orderUtil.checkPendingOrders();

                if (!market.equals(newMarket)) {
                    Runnable decision = decisionMaker.getDecision(newMarket.getInstruments());
                    decision.run();
                }
            }
        }

    }
}
