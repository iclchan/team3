package com.cs.tradingapp;

import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DecisionMaker {

    private HashMap<String, Double> estimatedPriceBuy_A = new HashMap<>();
    private HashMap<String, Double> trendBuy_T = new HashMap<>();
    private HashMap<String, Double> estimatedPriceSell_A = new HashMap<>();
    private HashMap<String, Double> trendSell_T = new HashMap<>();
    private static final long startTime = System.currentTimeMillis();
    private boolean tradeFreeze = false;
    private static int period_t = 0;
    private static final long TRADE_FREEZE_DURATION = 300000; // 5 minutes in milliseconds
    private static final double AVG_SMOOTHING_CONSTANT = 0.7;
    private static final double TREND_SMOOTHING_CONSTANT = 0.3;
    private Team team;
            
    public Runnable getDecision(List<Instrument> instruments) {
        Runnable decisionMakerRunnable = () -> {
            period_t++;
            if ( tradeFreeze ) updateTradeFreeze(System.currentTimeMillis());
            List<JSONObject> tradingActions = new ArrayList<>();
            TradingAppUtil tradingAppUtil = new TradingAppUtil();
            OrderUtil orderUtil = new OrderUtil();
            team = tradingAppUtil.getTeamInfo();
            instruments.parallelStream().forEach(instrument -> {
                tradingActions.add(getAction(instrument));
            });
            //TODO for each recommendation, fire request! (JSON)
            for(JSONObject jsonParam: tradingActions){
                if(jsonParam != null){
                    System.out.println("----------------------------------------------");
                    String response = tradingAppUtil.executeLimitOrder(jsonParam);
                    System.out.println("----------------------------------------------");
                    orderUtil.addPendingOrder(response);
                }
            }
        };
        return decisionMakerRunnable;
    }

    private void updateTradeFreeze(long currentTime) {
        tradeFreeze = ( startTime + TRADE_FREEZE_DURATION ) >= currentTime;
    }

    private JSONObject getAction(Instrument instrument) {
        JSONObject result = new JSONObject();

        HashMap<String, String> recommendation =  getRecommendation(instrument);

        if( !tradeFreeze && recommendation != null) {
            result.put("symbol", instrument.getSymbol());
            result.put("side", recommendation.get("side"));
            result.put("price", recommendation.get("price"));
            result.put("qty", recommendation.get("qty"));
            return result;
        }
        else {
            return null;
        }
    }

    private HashMap<String,String> getRecommendation(Instrument instrument) {
        String symbol = instrument.getSymbol();
        Double buyWA = instrument.getBuyWA();
        Double sellWA = instrument.getSellWA();
        HashMap<String, String> result = null;
        
        updateCalculationVariables(symbol, buyWA, sellWA);
        
        int position = getPosition(symbol); // negative is sell, positive is buy;
        switch(position) {
            case -1:
            case 1:
                result = new HashMap<>();
                result.put("side", position > 0 ? "buy" : "sell");
                result.put("price", position > 0 ? "" + buyWA : "" + sellWA ); // buy at current price
                result.put("qty", getStaggeredQuantity(symbol)); // TODO remove magic number!
                break;
            case 0:
                // hold
                break;
            default:
                System.out.println("Invalid position: " + position);
        }
        return result;
    }

    private String getStaggeredQuantity(String symbol) {
        double estimatedPriceBuy = estimatedPriceBuy_A.get(symbol);
        double trendBuy = trendBuy_T.get(symbol);
        double estimatedPriceSell = estimatedPriceSell_A.get(symbol);
        double trendSell = trendSell_T.get(symbol);

        double percentageChangeBuy = trendBuy/estimatedPriceBuy;
        double percentageChangeSell = trendSell/estimatedPriceSell;
        
        return "100";
    }

    private int getPosition(String symbol) {
        int buyPosition = (int) Math.signum(trendBuy_T.get(symbol));
        int sellPosition = (int) Math.signum(trendSell_T.get(symbol));
        if (buyPosition > 0 && sellPosition > 0) {
            return 1;
        } else if (buyPosition < 0 && sellPosition < 0 && team.getInstrumentQty(symbol) > 0) {
            return -1;
        } else return 0;
    }

    private void updateCalculationVariables(String symbol, Double buyWA, Double sellWA) {
        if (period_t != 1) {
            Double previousBuyAt = estimatedPriceBuy_A.get(symbol);
            Double previousBuyTt = trendBuy_T.get(symbol);
            Double previousSellAt = estimatedPriceSell_A.get(symbol);
            Double previousSellTt = trendSell_T.get(symbol);

            Double currentBuyAt = calculateEstimatedPrice(buyWA, previousBuyAt, previousBuyTt);
            Double currentBuyTt = calculateTrend(currentBuyAt, previousBuyAt, previousBuyTt);
            Double currentSellAt = calculateEstimatedPrice(sellWA, previousSellAt, previousSellTt);
            Double currentSellTt = calculateTrend(currentSellAt, previousSellAt, previousSellTt);

            estimatedPriceBuy_A.put(symbol, currentBuyAt);
            trendBuy_T.put(symbol, currentBuyTt);
            estimatedPriceSell_A.put(symbol, currentSellAt);
            trendSell_T.put(symbol, currentSellTt);
        } else {
            estimatedPriceBuy_A.put(symbol, buyWA);
            trendBuy_T.put(symbol, 0.0);
            estimatedPriceSell_A.put(symbol, sellWA);
            trendSell_T.put(symbol, 0.0);
        }
    }

    private static Double calculateEstimatedPrice(Double actualPrice, Double previousEstimatedPrice, Double previousTrend) {
        return ( AVG_SMOOTHING_CONSTANT * actualPrice ) + ( (1-AVG_SMOOTHING_CONSTANT) * (previousEstimatedPrice + previousTrend));
    }

    private static Double calculateTrend(Double currentEstimatedPrice, Double previousEstimatedPrice, Double previousTrend) {
        return ( TREND_SMOOTHING_CONSTANT * (currentEstimatedPrice - previousEstimatedPrice) ) + ( (1-TREND_SMOOTHING_CONSTANT) * previousTrend );
    }

    private Double getForecast(String symbol) {
        return estimatedPriceBuy_A.get(symbol) + trendBuy_T.get(symbol);
    }

}
