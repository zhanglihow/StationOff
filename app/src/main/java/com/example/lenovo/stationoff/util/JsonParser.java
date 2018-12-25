package com.example.lenovo.stationoff.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Json结果解析类
 */
public class JsonParser {


    //{"sn":1,"ls":false,"bg":0,"ed":0,"ws":[{"bg":1,"cw":[{"sc":0.0,"w":"原来"}]}]}
    public static String parseIatResult(String json) {
        StringBuffer ret = new StringBuffer();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            int sn=joResult.getInt("sn");
            if(sn!=1){
                return null;
            }
            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
//                JSONObject obj = items.getJSONObject(0);
//                ret.append(obj.getString("w"));
//              如果需要多候选结果，解析数组其他字段
              for(int j = 0; j < items.length(); j++) {
                  JSONObject obj = items.getJSONObject(j);
                  ret.append(obj.getString("w"));
              }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

}
