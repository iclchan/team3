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
    private boolean tradeFreeze = false;
    private static int period_t = 0;
    private static final long TRADE_FREEZE_DURATION = 60000; // 1 minute in milliseconds
    private static final double SEED_MONEY = 1_000_000;
    private static final double AVG_SMOOTHING_CONSTANT = 0.7;
    private static final double TREND_SMOOTHING_CONSTANT = 0.3;
    private static final double BUY_LIMIT = 1000;
    private static final double BUY_EXPOSURE_MODIFIER = 0.7; // 0.0 to 1.0
    private static final double BUY_RISK_MODIFIER = 0.7; // 0.0 to 1.0
    private static final double BUY_EXPOSURE_RISK_RATIO = 0.5; // favor exposure <=> 0.5 <=> favor risk
    private static NormalDistribution BUY_CURVE = new NormalDistribution(BUY_LIMIT/2, BUY_LIMIT/4);
    private static final double SELL_LIMIT = 1000;
    private static final double SELL_EXPOSURE_MODIFIER = 0.7; // 0.0 to 1.0
    private static final double SELL_RISK_MODIFIER = 0.7; // 0.0 to 1.0
    private static final double SELL_EXPOSURE_RISK_RATIO = 0.5; // favor exposure <=> 0.5 <=> favor risk
    private static NormalDistribution SELL_CURVE = new NormalDistribution(SELL_LIMIT/2, SELL_LIMIT/4);
    private static final double LOSS_TOLERANCE = 0.5; // sells stock when it hits LOSS_TOLERANCE of original buying price
    private static final double MAX_INSTRUMENT_HOLDINGS = 150_000;
    private Team team;
    private OrderUtil orderUtil;

    public Runnable getDecision(List<Instrument> instruments) {
        Runnable decisionMakerRunnable = () -> {
            period_t++;
            if ( tradeFreeze ) updateTradeFreeze(System.currentTimeMillis());
            List<JSONObject> tradingActions = new ArrayList<>();
            TradingAppUtil tradingAppUtil = new TradingAppUtil();
            orderUtil = new OrderUtil();
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
                staggeredQuantity = getSellStaggeredQuantity(symbol, currentQuantity, buyWA);
                if (staggeredQuantity > 0.0) {
                    result = generateRecommendationResult("sell", buyWA, staggeredQuantity);
                }
                break;
            case 1:
                staggeredQuantity = getBuyStaggeredQuantity(symbol, currentQuantity, sellWA);
                if (staggeredQuantity > 0.0) {
                    result = generateRecommendationResult("buy", sellWA, staggeredQuantity);
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

    private double getSellStaggeredQuantity(String symbol, double currentQuantity, double price) {
        double estimatedPriceSell = estimatedPriceSell_A.get(symbol);
        double trendSell = trendSell_T.get(symbol);
        double percentageChangeSell = trendSell / estimatedPriceSell;
        if (currentQuantity == 0) {
            return SELL_LIMIT;
        } else {
            double currentExposure = (currentQuantity * price / SEED_MONEY);
            double exposureLimiter = SELL_EXPOSURE_MODIFIER * ( 1 - currentExposure );
            double riskAdversity = SELL_RISK_MODIFIER * ( 1 - percentageChangeSell );
            double probability = ( ( 1 - SELL_EXPOSURE_RISK_RATIO) * (exposureLimiter) ) + ( (SELL_EXPOSURE_RISK_RATIO) * riskAdversity );
            double suggestedSellQuantity = SELL_CURVE.inverseCumulativeProbability(probability);
            List<double[]> history = orderUtil.getInstrumentHistory(symbol);
            double profitableQuantity = 0;
            if (history != null) {
                for( int i = 0; i < history.size(); i++ ) {
                    double buyPrice = history.get(i)[0];
                    double buyQuantity = history.get(i)[1];
                    if (buyPrice < price || buyPrice <= price * LOSS_TOLERANCE) {
                        profitableQuantity += buyQuantity;
                    }
                    if (profitableQuantity >= suggestedSellQuantity) {
                        break;
                    }
                }
            }
            // TODO remove this before competition
//            System.out.println("-----DECISION MAKER-----");
//            System.out.println("currentExposure " + currentExposure);
//            System.out.println("percentageChangeBuy " + percentageChangeSell);
//            System.out.println("------------------------");
//            System.out.println("exposureLimiter: " + exposureLimiter);
//            System.out.println("riskAdversity: " + riskAdversity);
//            System.out.println("probability: " + probability);
//            System.out.println("-----DECISION MAKER-----");
            return Math.min(profitableQuantity, suggestedSellQuantity);
        }
    }

    private double getBuyStaggeredQuantity(String symbol, double currentQuantity, double price) {
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
            int maxQuantity = (int) Math.min((team.getCash() / price), ( MAX_INSTRUMENT_HOLDINGS / price ) - currentQuantity);
            // TODO remove this before competition
//            System.out.println("-----DECISION MAKER-----");
//            System.out.println("currentExposure " + currentExposure);
//            System.out.println("percentageChangeBuy " + percentageChangeBuy);
//            System.out.println("------------------------");
//            System.out.println("exposureLimiter: " + exposureLimiter);
//            System.out.println("riskAdversity: " + riskAdversity);
//            System.out.println("probability: " + probability);
//            System.out.println("-----DECISION MAKER-----");
            return Math.min(BUY_CURVE.inverseCumulativeProbability(probability), maxQuantity);
        }
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
