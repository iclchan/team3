package main.java.com.cs.tradingapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Market {
    private HashMap<String, Instrument> instruments;

    public Market(HashMap<String, Instrument> instruments) {
        this.instruments = instruments;
    }
    
    public Instrument getInstrument(String symbol){
        return instruments.get(symbol);
    }

    public List<Instrument> getInstruments() {
        return instruments.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }
    
    public boolean equals(Market another){
        boolean[] equals = new boolean[]{true};
        instruments.entrySet().forEach(entry -> {
            String symbol = entry.getKey();
            Instrument instrument = entry.getValue();
            
            Instrument anotherInstrument = another.getInstrument(symbol);
            if(instrument.getBuyWA() != anotherInstrument.getBuyWA() || instrument.getSellWA() != anotherInstrument.getSellWA()){
                equals[0] = false;
            }
        });
        
        return equals[0];
    }
}
