package com.cs.tradingapp;

import net.minidev.json.JSONObject;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DecisionMaker {

    private HashMap<String, Double> estimatedPriceBuy_A = new HashMap<>();
    private HashMap<String, Double> trendBuy_T = new HashMap<>();
    private HashMap<String, Double> estimatedPriceSell_A = new HashMap<>();
    private HashMap<String, Double> trendSell_T = new HashMap<>();
    private static final long startTime = System.currentTimeMillis();
    private boolean tradeFreeze = true;
    private static int period_t = 0;
    private static final long TRADE_FREEZE_DURATION = 300000; // 5 minutes in milliseconds
    private static final double SEED_MONEY = 1_000_000;
    private static final double AVG_SMOOTHING_CONSTANT = 0.7;
    private static final double TREND_SMOOTHING_CONSTANT = 0.3;
    private static final double BUY_LIMIT = 1000;
    private static final double BUY_EXPOSURE_MODIFIER = 1.0; // 0.0 to 1.0
    private static final double BUY_RISK_MODIFIER = 1.0; // 0.0 to 1.0
    private static final double BUY_EXPOSURE_RISK_RATIO = 0.5; // favor exposure <=> 0.5 <=> favor risk
    private static NormalDistribution BUY_CURVE = new NormalDistribution(BUY_LIMIT/2, BUY_LIMIT/3);
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
        double currentQuantity = team.getInstrumentQty(symbol);
        double staggeredQuantity;
        switch(position) {
            case -1:
                staggeredQuantity = getStaggeredQuantity(symbol, position, currentQuantity, buyWA);
                if (staggeredQuantity > 0.0) {
                    result = generateRecommendationResult("buy", buyWA, staggeredQuantity);
                }
                break;
            case 1:
                staggeredQuantity = getStaggeredQuantity(symbol, position, currentQuantity, sellWA);
                if (staggeredQuantity > 0.0) {
                    result = generateRecommendationResult("sell", sellWA, staggeredQuantity);
                }
                break;
            case 0:
                // hold
                break;
            default:
                System.out.println("Invalid position: " + position);
        }
        return result;
    }

    private HashMap<String, String> generateRecommendationResult(String side, double price, double qty) {
        HashMap<String, String> result = new HashMap<>();
        result.put("side", side);
        result.put("price", "" + price);
        result.put("qty", "" + qty);
        return result;
    }

    private double getStaggeredQuantity(String symbol, int position, double currentQuantity, double price) {
        if (position == 0) return 0.0;
        else if (position > 0) { // BUYBUYBUY
            double estimatedPriceBuy = estimatedPriceBuy_A.get(symbol);
            double trendBuy = trendBuy_T.get(symbol);
            double percentageChangeBuy = trendBuy / estimatedPriceBuy;
            if (currentQuantity == 0) {
                return BUY_LIMIT;
            } else {
                double currentExposure = (currentQuantity * price / SEED_MONEY);
                double exposureLimiter = BUY_EXPOSURE_MODIFIER * ( 1 - currentExposure );
                double riskAdversity = BUY_RISK_MODIFIER * ( 1 - percentageChangeBuy );
                double probability = ( ( 1 - BUY_EXPOSURE_RISK_RATIO) * (exposureLimiter) ) + ( (BUY_EXPOSURE_RISK_RATIO) * riskAdversity );
                double quantity = BUY_CURVE.inverseCumulativeProbability(probability);
                // TODO remove this before competition
                // TODO check current cash, can we even buy this amount?
                System.out.println("-----DECISION MAKER-----");
                System.out.println("currentExposure " + currentExposure);
                System.out.println("percentageChangeBuy " + percentageChangeBuy);
                System.out.println("------------------------");
                System.out.println("exposureLimiter: " + exposureLimiter);
                System.out.println("riskAdversity: " + riskAdversity);
                System.out.println("probability: " + probability);
                System.out.println("quantity: " + quantity);
                System.out.println("-----DECISION MAKER-----");
                return quantity;
            }
        } else if (position < 0) { // SELLSELLSELL
            if ( currentQuantity == 0) return 0.0;
            else {
                double estimatedPriceSell = estimatedPriceSell_A.get(symbol);
                double trendSell = trendSell_T.get(symbol);
                double percentageChangeSell = trendSell/estimatedPriceSell;
            }
        }
        
        return 100.0;
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
