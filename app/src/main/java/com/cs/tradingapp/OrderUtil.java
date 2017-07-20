package com.cs.tradingapp;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class OrderUtil {

    private static HashMap<String, List<double[]>> history = new HashMap<>();
    private static List<Order> pendingOrders = new ArrayList<>();
    private TradingAppUtil tradingAppUtil = new TradingAppUtil();

    public OrderUtil(){
        Team team = tradingAppUtil.getTeamInfo();
        String marketData = tradingAppUtil.getMarketData();
        List<String> instruments = JsonPath.read(marketData, "$..symbol");
        List<Double> prices = JsonPath.read(marketData, "$..ask");
        for(int i=0; i<instruments.size(); i++){
            String symbol = instruments.get(i);
            double qty = team.getInstrumentQty(symbol);
            if(qty == 0){
                history.put(symbol, new ArrayList<>());
            }else{
                double price = prices.get(i);
                history.put(symbol, Arrays.asList(new double[]{price, qty}));
            }
            
            
            
            
        }
    }
    
    public void addPendingOrder(String response) {
        if (response != null && !response.isEmpty()) {
            String symbol = JsonPath.read(response, "$.symbol");
            String orderId = JsonPath.read(response, "$.id");
            double qty = (int) JsonPath.read(response, "$.qty");
            double filledQty = (int) JsonPath.read(response, "$.filled_qty");
            double price = JsonPath.read(response, "$.price");
            pendingOrders.add(new Order(symbol, orderId, qty, filledQty, price));
        }
    }

    public void checkPendingOrders() {
        if (!pendingOrders.isEmpty()) {
            System.out.println("Scanning limit orders...");
            System.out.println("----------------------------------------------");
            Iterator<Order> orderIter = pendingOrders.iterator();
            while (orderIter.hasNext()) {
                Order order = orderIter.next();
                String orderId = order.getOrderId();

                int filled = -2;
                filled = tradingAppUtil.checkLimitOrder(order);
                
                switch (filled) {
                    case -1:
                        orderIter.remove();
                        break;
                    case 0:
                        List<double[]> orderHistory = history.get(orderId);
                        if (orderHistory == null) {
                            orderHistory = new ArrayList<double[]>();
                        }
                        orderHistory.add(new double[]{order.getPrice(), order.getQty()});
                        orderHistory.sort((double[] o1, double[] o2)-> (int) (o2[0] - o1[0]));
                        history.put(orderId, orderHistory);
                        orderIter.remove();
                        break;
                    case 1:
                        break;
                    default:
                        System.out.println("No order checked");
                }
            }
            System.out.println("----------------------------------------------");
            System.out.println("Scanning limit orders done!");
        }
    }

    public HashMap<String, List<double[]>> getHistory() {
        return history;
    }

    public List<double[]> getInstrumentHistory(String symbol) {
        return history.get(symbol);
    }
}
