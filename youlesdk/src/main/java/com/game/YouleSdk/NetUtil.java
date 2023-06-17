package com.game.YouleSdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NetUtil{
    public String TAG = "NetUtil";
    private String appkey = "";
    private String model = "";
    public String userCode = "";
    public String choiceId = "";
    public String paymentType = "";

    NetUtil(String _appkey,String _model)
    {
        appkey = _appkey;
        model = _model;
        Log.d(TAG, "NetUtil init:appkey="+appkey+";model="+model);
    }
    public String get(String url1,String state) {
        Log.d(TAG, "NetUtil get:"+url1);
//        System.out.printf("NetUtil get:%s",url1);
        try {
            URL url = new URL("https://j01.easyhope.com/"+url1);
            HttpURLConnection Connection = (HttpURLConnection) url.openConnection();
            Connection.setRequestMethod("GET");
            Connection.setConnectTimeout(5000);
            Connection.setReadTimeout(5000);
            if(state.indexOf("getUserCode") != -1)
            {
                Connection.setRequestProperty("appkey", this.appkey);
                Connection.setRequestProperty("model", this.model);
            }
            else if(state.indexOf("getUserPaymentList") != -1 || state.indexOf("saveSmsPayRecord") != -1)
            {
                Connection.setRequestProperty("appkey", this.appkey);
                Connection.setRequestProperty("model", this.model);
                Connection.setRequestProperty("userCode", this.userCode);
            }
            else if(state.indexOf("sms") != -1)
            {
            }
//            Log.d(TAG, "NetUtil get:"+url1);
            int responseCode = Connection.getResponseCode();
//            Log.d(TAG, "responseCode:"+responseCode);
            if (responseCode == Connection.HTTP_OK) {
                InputStream inputStream = Connection.getInputStream();
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = inputStream.read(bytes)) != -1) {
                    arrayOutputStream.write(bytes, 0, length);
                    arrayOutputStream.flush();//强制释放缓冲区
                }
                String s = arrayOutputStream.toString();
                return s;
            } else {
                return "-1";
            }
        } catch (Exception e) {
            Log.d(TAG, "catch:"+e);
            return "-1";
        }
    }
    /**获取用户code*/
    /**
     *
     * @param gaid          googleADid        87f20949-4880-44ca-a902-ee3d3b1e7d72
     * @param imei          手机imei          358015970828188
     * @param mac           手机mac           02:00:00:00:00:00
     * @param countryCode   国家码             CH
     */
    public void getUserCode(String gaid,String imei,String mac,String countryCode)  {
        String data = "";
        data += ("&gaid="+gaid);
        data += ("&imei="+imei);
        data += ("&mac="+mac);
        data += ("&countryCode="+countryCode);
        Log.d(TAG, "getUserCode gaid:"+gaid);
        Log.d(TAG, "getUserCode imei:"+imei);
        Log.d(TAG, "getUserCode mac:"+mac);
        Log.d(TAG, "getUserCode countryCode:"+countryCode);
//        Log.d(TAG, "getUserCode:"+data);
        String res = this.get("api/user/getUserCode?"+data,"getUserCode");
//        Log.d(TAG, "getUserCode"+res);
        //响应结果:  {"status":"SUCCESS","errorCode":null,"message":null,"content":"a866a46a7ea24bc989b27d73092fc698"}  content 就是 userCode
        if(res.indexOf("-1") != -1)
        {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(res);
            String status = jsonObject.getString("status");
            String content = jsonObject.getString("content");
            if(status.indexOf("SUCCESS") ==  -1)
            {
                Log.d(TAG, "getUserCode error:"+status);
            }
            else
            {
                userCode = content;
                Log.d(TAG, "userCode:"+userCode);
                getUserPaymentList(countryCode);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    /**获取支付策略*/
    /**
     *
     * @param countryCode   国家码         CH
     */
    public void getUserPaymentList(String countryCode) {
        String data = "";
        data += ("countryCode="+countryCode);
        String res = this.get("api/payment/getUserPaymentList?"+data,"getUserPaymentList");
        Log.d(TAG, "getUserPaymentList"+res);
        if(res.indexOf("-1") != -1)
        {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(res);
            String status = jsonObject.getString("status");
            String content = jsonObject.getString("content");
            if(status.indexOf("SUCCESS") ==  -1)
            {
                Log.d(TAG, "getUserCode error:"+status);
            }
            else
            {
                Log.d(TAG, "content:"+content);
                JSONObject jsonObject1 = new JSONObject(content);

                String firstChoice = jsonObject1.getString("firstChoice");
                Log.d(TAG, "firstChoice:"+firstChoice);
                JSONObject firstChoiceJson = new JSONObject(firstChoice);
                choiceId = firstChoiceJson.getString("id");
                paymentType = firstChoiceJson.getString("paymentType");
                Log.d(TAG, "choiceId:"+choiceId +";paymentType:"+paymentType);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // 响应结果:
        // {
        // "status":"SUCCESS","errorCode":null,"message":null,
        //      "content":{
        //          "firstChoice":{"id":1,"paymentType":"VAS","paymentName":"短信支付","datetime":null},
        //          "alternateList":[{"id":2,"paymentType":"AD","paymentName":"广告支付","datetime":null}]
        //      }
        //  }
        //  content 中的 firstChoice 是首选，alternateList 是备选，备选可能会有多个

    }

    public String saveSmsPayRecord(String countryCode, String currency, String amount, String orderNumber ) {
        String data = "";
        data += ("countryCode="+countryCode);
        data += ("&currency="+currency);
        data += ("&amount="+amount);
        data += ("&paymentId="+choiceId);
        data += ("&orderNumber="+orderNumber);
        String res = this.get("api/payment/saveSmsPayRecord?"+data,"saveSmsPayRecord");
        Log.d(TAG, "saveSmsPayRecord"+res);
        if(res.indexOf("-1") != -1)
        {
            return "";
        }
        try {
            JSONObject jsonObject = new JSONObject(res);
            String status = jsonObject.getString("status");
            String content = jsonObject.getString("content");
            if(status.indexOf("SUCCESS") ==  -1)
            {
                Log.d(TAG, "getUserCode error:"+status);
                return "";
            }
            else
            {
                Log.d(TAG, "content:"+content);
                JSONObject jsonObject1 = new JSONObject(content);
                String billingNumber = jsonObject1.getString("billingNumber");
                Log.d(TAG, "billingNumber:"+billingNumber);
                return billingNumber;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }
    /**同步支付结果*/
    /**
     *
     * @param billingNumber      saveSmsPayRecord 接口返回去的
     * @param paymentStatus 支付结果        （SUCCESSFUL 或者 FAILED）
     */
    public void smsPaymentNotify(String billingNumber,String  paymentStatus) {
        String data = "";
        data += ("billingNumber="+billingNumber);
        data += ("&paymentStatus="+paymentStatus);
        String res = this.get("paynicornNotify/sms?"+data,"sms");
        Log.d(TAG, "smsPaymentNotify"+res);
        //响应结果: {"status":"SUCCESS","errorCode":null,"message":null,"content":"8852ec247c4a471690c83033d2aa5ca8"}  content 是  billingNumber
    }
}
