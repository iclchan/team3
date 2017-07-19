package com.cs.tradingapp;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class OrderUtil {

    private static HashMap<String, List<double[]>> history = new HashMap<>();
    private static List<Order> pendingOrders = new ArrayList<>();
    private TradingAppUtil tradingAppUtil = new TradingAppUtil();

    public void addPendingOrder(String response) {
        String symbol = JsonPath.read(response, "$.symbol");
        String orderId = JsonPath.read(response, "$.id");
        double qty = (int) JsonPath.read(response, "$.qty");
        double filledQty = (int) JsonPath.read(response, "$.filled_qty");
        double price = JsonPath.read(response, "$.price");
        pendingOrders.add(new Order(symbol, orderId, qty, filledQty, price));
    }

    public void checkPendingOrders() {
        if (!pendingOrders.isEmpty()) {
            Iterator<Order> orderIter = pendingOrders.iterator();
            while (orderIter.hasNext()) {
                Order order = orderIter.next();
                String orderId = order.getOrderId();

                int filled = -2;
                filled = tradingAppUtil.checkLimitOrder(orderId);
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
                        history.put(orderId, orderHistory);
                        orderIter.remove();
                        break;
                    case 1:
                        break;
                    default:
                        System.out.println("No order checked");
                }
            }
        }
    }

    public HashMap<String, List<double[]>> getHistory() {
        return history;
    }
}
