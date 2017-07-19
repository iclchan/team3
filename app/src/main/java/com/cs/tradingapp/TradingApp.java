package main.java.com.cs.tradingapp;

public class TradingApp {
    public static void main(String[] app){
        DecisionMaker decisionMaker = new DecisionMaker();
        Market market = null;
        while(true){
            if(market == null){
                market = TradingAppUtil.getInstrumentData();
            }else{
                Market newMarket = TradingAppUtil.getInstrumentData();
                System.out.println("--------------- Checking Orders ---------------");
                OrderUtil.checkPendingOrders();
                
                if(!market.equals(newMarket)){
                    System.out.println("--------------- Run Decision ---------------");
                    Runnable decision = decisionMaker.getDecision(newMarket.getInstruments());
                    decision.run();
                }
            }
        }
        
    }
}
