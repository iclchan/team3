package main.java.com.cs.tradingapp;

import com.jayway.jsonpath.JsonPath;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.minidev.json.JSONObject;

public class TradingAppUtil {

    public static Market getInstrumentData(){
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
    
    public static Team getTeamInfo(){
        String teamInfo = "";
        Team team = null;
        try {
            URL url = new URL("https://cis2017-teamtracker.herokuapp.com/api/teams/tOqZFjL4DLle_Kyaotpttg");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String teamInfoLine;
            while ((teamInfoLine = br.readLine()) != null) {
                teamInfo += teamInfoLine;
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(!teamInfo.isEmpty()){
            String uid = JsonPath.read(teamInfo, "$.uid");
            double cash = Double.parseDouble(JsonPath.read(teamInfo, "$.cash"));
            double reservedCash = Double.parseDouble(JsonPath.read(teamInfo, "$.reserved_cash"));
            List<String> instrumentsList = Arrays.asList("0001", "0005", "0386", "0388", "3988");
            HashMap<String, Double> instrumentMap = new HashMap<>();
            for(String instrument: instrumentsList){
                double instrumentQty = 0.0;
                try{
                    instrumentQty = Double.parseDouble(JsonPath.read(teamInfo, "$." + instrument));
                }catch (Exception e){}
                instrumentMap.put(instrument, instrumentQty);
            };
            
            HashMap<String, Double> reservedInstrumentMap = new HashMap<>();
            for(String instrument: instrumentsList){
                double instrumentQty = 0.0;
                try{
                    instrumentQty = Double.parseDouble(JsonPath.read(teamInfo, "$." + instrument + "_reserved"));
                }catch (Exception e){}
                reservedInstrumentMap.put(instrument, instrumentQty);
            };
            
            team = new Team(uid, cash, reservedCash, instrumentMap, reservedInstrumentMap);
        }
        
        return team;
    }
    
    public static String executeLimitOrder(JSONObject jsonParam){
        jsonParam.put("team_uid", "tOqZFjL4DLle_Kyaotpttg");
        jsonParam.put("order_type", "limit");
        return executeOrder(jsonParam);
    }
    
    public static String executeMarketOrder(JSONObject jsonParam){
        jsonParam.put("team_uid", "tOqZFjL4DLle_Kyaotpttg");
        jsonParam.put("order_type", "market");
        return executeOrder(jsonParam);
    }
    
    public static boolean checkLimitOrder(String orderId){
        String response = "";
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/orders/" + orderId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                System.out.println("Checking order: " + responseLine);
                response += responseLine;
            }
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if(!response.isEmpty()){
            double qty = (int) JsonPath.read(response, "$.qty");
            double filledQty = (int) JsonPath.read(response, "$.filled_qty");
            if(filledQty == qty){
                cancelLimitOrder(orderId);
                return true;
            }
        }
        return false;
    }
    
    public static String cancelLimitOrder(String orderId){
        String response = "";
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/orders/" + orderId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Accept", "application/json");
            
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response += responseLine;
            }
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
      
    private static String getMarketData() {
        String marketData = "";
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/market_data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String marketDataLine;
            while ((marketDataLine = br.readLine()) != null) {
                marketData += marketDataLine;
            }
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return marketData;
    }
    
    private static String getInstrumentInfo(String symbol){
        String instrumentInfo = "";
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/market_data/" + symbol);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String instrumentInfoLine;
            while ((instrumentInfoLine = br.readLine()) != null) {
                instrumentInfo += instrumentInfoLine;
            }
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return instrumentInfo;
    }
    
    private static String executeOrder(JSONObject jsonParam){
        String response = "";
        System.out.println("--------------- Executing Orders ---------------");
        System.out.println(jsonParam.toString());
        try {
            URL url = new URL("https://cis2017-exchange.herokuapp.com/api/orders");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type","application/json"); 
            try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
               wr.write(jsonParam.toString().getBytes(StandardCharsets.UTF_8));
            }
            
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream()))); 
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                System.out.println("Executing order: " + responseLine);
                response += responseLine;
            }
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
