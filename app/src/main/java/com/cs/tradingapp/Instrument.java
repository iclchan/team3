package main.java.com.cs.tradingapp;

import java.util.HashMap;

public class Instrument{
    private String symbol;
    private double buyWA;
    private double sellWA;
    
    public Instrument(String symbol, HashMap<String, Integer> buyMap, HashMap<String, Integer> sellMap){
        this.symbol = symbol;
        this.buyWA = calculateWeightedAverage(buyMap);
        this.sellWA = calculateWeightedAverage(sellMap);
    }

    private double calculateWeightedAverage(HashMap<String, Integer> map) {
        double[] totalPrice = new double[]{0};
        int[] totalQty = new int[]{0};
        map.entrySet().forEach(entry -> {
            double price = Double.parseDouble(entry.getKey());
            int qty = entry.getValue();
            totalPrice[0] += (price * qty);
            totalQty[0] += qty;
        });
        return totalPrice[0] / totalQty[0];
    }

    public String getSymbol() {
        return symbol;
    }

    public double getBuyWA() {
        return buyWA;
    }

    public double getSellWA() {
        return sellWA;
    }
}
