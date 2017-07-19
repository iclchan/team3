package com.cs.tradingapp;

public class TradingApp {
    public static void main(String[] app){
        DecisionMaker decisionMaker = new DecisionMaker();
        OrderUtil orderUtil = new OrderUtil();
        
        Market market = null;
        while(true){
            if(market == null){
                market = TradingAppUtil.getInstrumentData();
            }else{
                Market newMarket = TradingAppUtil.getInstrumentData();
                System.out.println("--------------- Checking Orders ---------------");
                orderUtil.checkPendingOrders();
                
                if(!market.equals(newMarket)){
                    System.out.println("--------------- Run Decision ---------------");
                    Runnable decision = decisionMaker.getDecision(newMarket.getInstruments());
                    decision.run();
                }
            }
        }
        
    }
}
