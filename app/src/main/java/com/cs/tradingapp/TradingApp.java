package com.cs.tradingapp;

public class TradingApp {

    public static void main(String[] app) {
        System.out.println("----------------------------------------------");
        System.out.println("                                              \n"
                + " _____           _ _            _____         \n"
                + "|_   _|___ ___ _| |_|___ ___   |  _  |___ ___ \n"
                + "  | | |  _| .'| . | |   | . |  |     | . | . |\n"
                + "  |_| |_| |__,|___|_|_|_|_  |  |__|__|  _|  _|\n"
                + "                        |___|        |_| |_|  ");
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
                Market newMarket = TradingAppUtil.getInstrumentData();
                orderUtil.checkPendingOrders();

                if (!market.equals(newMarket)) {
                    System.out.println("Market change detected!");
                    System.out.println("Running Decision Maker based on new market...");
                    Runnable decision = decisionMaker.getDecision(newMarket.getInstruments());
                    decision.run();
                }
            }
        }

    }
}
