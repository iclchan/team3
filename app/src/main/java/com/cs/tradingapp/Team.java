package main.java.com.cs.tradingapp;

import java.util.HashMap;

public class Team {
    private String uid;
    private double cash;
    private double reservedCash;
    private HashMap<String, Double> instruments;
    private HashMap<String, Double> reservedInstruments;

    public Team(String uid, double cash, double reservedCash, HashMap<String, Double> instruments, HashMap<String, Double> reservedInstruments) {
        this.uid = uid;
        this.cash = cash;
        this.reservedCash = reservedCash;
        this.instruments = instruments;
        this.reservedInstruments = reservedInstruments;
    }

    public String getUid() {
        return uid;
    }

    public double getCash() {
        return cash;
    }

    public double getReservedCash() {
        return reservedCash;
    }

    public double getInstrumentQty(String symbol){
        return instruments.get(symbol);
    }
    
    public HashMap<String, Double> getInstruments() {
        return instruments;
    }

    public HashMap<String, Double> getReservedInstruments() {
        return reservedInstruments;
    }
    
    public String toString(){
        return "uid:" + uid + "\ncash:" + cash + "\nreserved cash:" + reservedCash + "\nInstruments:\n" + instruments.toString() + "\nreserved instruments:\n" + reservedInstruments.toString();
    }
}
