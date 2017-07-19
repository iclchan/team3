package main.java.com.cs.tradingapp;

public class Order {
    private String symbol;
    private String orderId;
    private double qty;
    private double filledQty;
    private double price;

    public Order(String symbol, String orderId, double qty, double filledQty, double price) {
        this.symbol = symbol;
        this.orderId = orderId;
        this.qty = qty;
        this.filledQty = filledQty;
        this.price = price;
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
}
