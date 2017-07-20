package com.cs.tradingapp;

import com.jayway.jsonpath.JsonPath;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.minidev.json.JSONObject;

public class TradingAppUtil {

    public static Market getInstrumentData() {
//        String marketData = getMarketData();
//        List<String> symbols = JsonPath.read(marketData, "$..symbol");
        List<String> symbols = Arrays.asList("0001", "0005", "0386", "0388", "3988");
        HashMap<String, Instrument> instruments = new HashMap<>();
        symbols.parallelStream().forEach(symbol -> {
            String instrumentJson = getInstrumentInfo(symbol);
            HashMap<String, Integer> buyMap = JsonPath.read(instrumentJson, "$.buy");
            HashMap<String, Integer> sellMap = JsonPath.read(instrumentJson, "$.sell");
            instruments.put(symbol, new Instrument(symbol, buyMap, sellMap));
        });

        return new Market(instruments);
    }

    public Team getTeamInfo() {
        String teamInfo = "";
        Team team = null;
        try {
            URL url = new URL("https://cis2017-teamtracker.herokuapp.com/api/teams/tOqZFjL4DLle_Kyaotpttg");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("Failed : HTTP error code : " + conn.getResponseCode());
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                String teamInfoLine;
                while ((teamInfoLine = br.readLine()) != null) {
                    teamInfo += teamInfoLine;
                }
            }

            conn.disconnect();

            if (!teamInfo.isEmpty()) {
                String uid = JsonPath.read(teamInfo, "$.uid");
                double cash = Double.parseDouble(JsonPath.read(teamInfo, "$.cash"));
                double reservedCash = Double.parseDouble(JsonPath.read(teamInfo, "$.reserved_cash"));
                List<String> instrumentsList = Arrays.asList("0001", "0005", "0386", "0388", "3988");
                HashMap<String, Double> instrumentMap = new HashMap<>();
                for (String instrument : instrumentsList) {
                    double instrumentQty = 0.0;
                    try {
                        instrumentQty = Double.parseDouble(JsonPath.read(teamInfo, "$." + instrument));
                    } catch (Exception e) {
                    }
                    instrumentMap.put(instrument, instrumentQty);
                };

                HashMap<String, Double> reservedInstrumentMap = new HashMap<>();
                for (String instrument : instrumentsList) {
                    double instrumentQty = 0.0;
                    try {
                        instrumentQty = Double.parseDouble(JsonPath.read(teamInfo, "$." + instrument + "_reserved"));
                    } catch (Exception e) {
                    }
                    reservedInstrumentMap.put(instrument, instrumentQty);
                };

                team = new Team(uid, cash, reservedCash, instrumentMap, reservedInstrumentMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return team;
    }

    public String executeLimitOrder(JSONObject jsonParam) {
        String symbol = (String) jsonParam.get("symbol");
        System.out.println("Executing limit order for " + symbol + "...");
        jsonParam.put("team_uid", "tOqZFjL4DLle_Kyaotpttg");
        jsonParam.put("order_type", "limit");
        return executeOrder(jsonParam);
    }

    public String executeMarketOrder(JSONObject jsonParam) {
        String symbol = (String) jsonParam.get("symbol");
        System.out.println("Executing market order for " + symbol + "...");
        jsonParam.put("team_uid", "tOqZFjL4DLle_Kyaotpttg");
        jsonParam.put("order_type", "market");
        return executeOrder(jsonParam);
    }

    public int checkLimitOrder(Order order) {
        String response = "";
        String orderId = order.getOrderId();
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/orders/" + orderId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("Checking failed for Order Id " + orderId + ": HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response += responseLine;
            }

            conn.disconnect();

            if (!response.isEmpty()) {
                if (response.equals("null")) {
                    System.out.println("null");
                    return -1;
                } else {
                    String status = JsonPath.read(response, "$.status");
                    String side = JsonPath.read(response, "$.side");
                    
                    if (status.equals("FILLED")) {
                        System.out.println("Status for " + side + " Order Id " + orderId + " : " + status);
                        return 0;
                    } else if (status.equals("NEW")) {
                        if(side.equals("sell")){
                            order.updateCycle();
                            int cycle = order.getCycle();
                            if(cycle < 2){
                                return 1;
                            }
                        }
                        
                        cancelLimitOrder(orderId);
                        System.out.println("Status for " + side + " Order Id " + orderId + " : CANCELED");
                        return -1;
                    } else {
                        System.out.println("Status for " + side + " Order Id " + orderId + " : " + status);
                        return 1;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -2;
    }

    private String cancelLimitOrder(String orderId) {
        String response = "";
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/orders/" + orderId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("Failed : HTTP error code : " + conn.getResponseCode());
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response += responseLine;
                }
            }
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public String getMarketData() {
        String marketData = "";
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/market_data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("Failed : HTTP error code : " + conn.getResponseCode());
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                String marketDataLine;
                while ((marketDataLine = br.readLine()) != null) {
                    marketData += marketDataLine;
                }
            }
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return marketData;
    }

    private static String getInstrumentInfo(String symbol) {
        String instrumentInfo = "";
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/market_data/" + symbol);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("Failed : HTTP error code : " + conn.getResponseCode());
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                String instrumentInfoLine;
                while ((instrumentInfoLine = br.readLine()) != null) {
                    instrumentInfo += instrumentInfoLine;
                }
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return instrumentInfo;
    }

    private String executeOrder(JSONObject jsonParam) {
        String response = "";
        System.out.println(jsonParam);
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/orders");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(jsonParam.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                System.out.println("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br;
            try {
                br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            } catch (Exception e) {
                br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
            }

            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response += responseLine;
            }

            conn.disconnect();

            if (!response.isEmpty()) {
                String symbol = (String) jsonParam.get("symbol");
                String errorMessage = null;
                try {
                    errorMessage = JsonPath.read(response, "$.message");
                    System.out.println("Executing limit order for " + symbol + " failed!");
                    System.out.println("Reason: " + errorMessage);
                } catch (Exception e) {
                    System.out.println("Executing limit order for " + symbol + " done!");
                    String orderId = JsonPath.read(response, "$.id");
                    String side = JsonPath.read(response, "$.side");
                    int qty = JsonPath.read(response, "$.qty");
                    double price = JsonPath.read(response, "$.price");
                    System.out.println("Order Id: " + orderId);
                    System.out.println("Side: " + side);
                    System.out.println("Quantity: " + qty);
                    System.out.println("Price: " + price);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }
}
