package main.java.com.cs.tradingapp;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class OrderUtil {

    private static HashMap<String, List<double[]>> history = new HashMap<>();
    private static List<Order> pendingOrders = new ArrayList<>();

    public static void addPendingOrder(String response) {
        String symbol = JsonPath.read(response, "$.symbol");
        String orderId = JsonPath.read(response, "$.id");
        double qty = (int) JsonPath.read(response, "$.qty");
        double filledQty = (int) JsonPath.read(response, "$.filled_qty");
        double price = JsonPath.read(response, "$.price");
        pendingOrders.add(new Order(symbol, orderId, qty, filledQty, price));
    }

    public static void checkPendingOrders() {
        if (!pendingOrders.isEmpty()) {
            Iterator<Order> orderIter = pendingOrders.iterator();
            while (orderIter.hasNext()) {
                Order order = orderIter.next();
                String orderId = order.getOrderId();
                
                boolean filled = false;
                if(order != null){
                    filled = TradingAppUtil.checkLimitOrder(orderId);
                }
                
                if (filled) {
                    List<double[]> orderHistory = history.get(orderId);
                    if (orderHistory == null) {
                        orderHistory = new ArrayList<double[]>();
                    }
                    orderHistory.add(new double[]{order.getPrice(), order.getQty()});
                    history.put(orderId, orderHistory);
                    orderIter.remove();
                }
            }
        }
    }

    public static HashMap<String, List<double[]>> getHistory() {
        return history;
    }
}
