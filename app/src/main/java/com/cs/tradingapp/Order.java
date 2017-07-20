package com.cs.tradingapp;

public class Order {
    private int cycle;
    private String symbol;
    private String orderId;
    private String side;
    private double qty;
    private double filledQty;
    private double price;

    public Order(String symbol, String side, String orderId, double qty, double filledQty, double price) {
        this.symbol = symbol;
        this.side = side;
        this.orderId = orderId;
        this.qty = qty;
        this.filledQty = filledQty;
        this.price = price;
        this.cycle = 0;
    }

    public String getSide() {
        return side;
    }

    public int getCycle() {
        return cycle;
    }
    
    public void updateCycle(){
        cycle++;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getQty() {
        return qty;
    }

    public double getFilledQty() {
        return filledQty;
    }
    
    public double getPrice(){
        return price;
    }
    
    public String toString(){
        return "Order Id: " + orderId + "\tSymbol: " + symbol;
    }
}
