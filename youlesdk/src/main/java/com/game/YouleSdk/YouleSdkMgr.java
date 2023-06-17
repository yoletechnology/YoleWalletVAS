package com.game.YouleSdk;

import static java.lang.Thread.sleep;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;

//import com.applovin.mediation.MaxAdFormat;
//import com.applovin.mediation.ads.MaxAdView;
//import com.applovin.mediation.ads.MaxInterstitialAd;
//import com.applovin.mediation.ads.MaxRewardedAd;
//import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
//import com.applovin.sdk.AppLovinPrivacySettings;
//import com.applovin.sdk.AppLovinSdk;
//import com.applovin.sdk.AppLovinSdkConfiguration;
//import com.game.AppLovinSdk.AppLovinMgr;
//import com.game.MobileAdsSDK.MobileAdsMgr;
import com.game.PaySDKManager.PaySdkMgr;
import com.transsion.pay.paysdk.manager.PaySDKManager;
import com.transsion.pay.paysdk.manager.entity.ConvertPriceInfo;
import com.transsion.pay.paysdk.manager.entity.CountryCurrencyData;
import com.transsion.pay.paysdk.manager.entity.OrderEntity;
import com.transsion.pay.paysdk.manager.entity.StartPayEntity;
import com.transsion.pay.paysdk.manager.entity.SupportPayInfoEntity;
import com.transsion.pay.paysdk.manager.inter.CurrencyConvertCallBack;
import com.transsion.pay.paysdk.manager.inter.InitResultCallBack;
import com.transsion.pay.paysdk.manager.inter.StartPayCallBack;
import com.transsion.pay.paysdk.manager.testmode.SMSTestUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class YouleSdkMgr {

    private String TAG = "YouleSdkMgr";
    private static  YouleSdkMgr _instance = null;
    private NetUtil request = null;
    private CountryCurrencyData tempData = null;
    private Context var =  null;
    private PhoneInfo info =  null;
    private String payOrderNum = "";//支付的订单号
    private HashMap<String,String> list;
    private boolean isPlayerIng = false;

    private String appkey = "";
    private String model = "";
    private String AP_ID = "";
    private String CP_ID = "";
    private String API_KEY = "";
    private String rewardedAdId = "";
    public static YouleSdkMgr getsInstance() {
        if(YouleSdkMgr._instance == null)
        {
            YouleSdkMgr._instance = new YouleSdkMgr();
        }
        return YouleSdkMgr._instance;
    }
    private YouleSdkMgr() {
        Log.e(TAG,"YouleSdkMgr");
    }
    public void initAd(Context var1, HashMap<String,String> var2,boolean isDebugger)
    {

        list = var2;
        appkey = list.get("appkey");
        model = list.get("model");
        AP_ID = list.get("AP_ID");
        CP_ID = list.get("CP_ID");
        API_KEY = list.get("API_KEY");

        request = new NetUtil(appkey,model);
        var = var1;
        info = new PhoneInfo(var1);

        if(isDebugger == true)
        {
            PaySdkMgr.getsInstance().setTestMode();
        }

        PaySdkMgr.getsInstance().setStrict(true);
        PaySdkMgr.getsInstance().initAriesPay(var1,AP_ID,CP_ID,API_KEY,StartPayEntity.PAY_MODE_ALL, new InitResultCallBack() {

            @Override
            public void onSuccess(List<SupportPayInfoEntity> list, boolean b, CountryCurrencyData countryCurrencyData) {
                Log.i(TAG,"onSuccess:"+
                        countryCurrencyData.countryCode + "	"+
                        countryCurrencyData.currency + " "+
                        countryCurrencyData.mcc +" "+
                        countryCurrencyData.smsOptimalAmount);
                        tempData = countryCurrencyData;

                        YouleSdkMgr.getsInstance().getUserCode();
            }

            @Override
            public void onFail(int i) {
                Log.i(TAG,"onFail:"+i);
            }
        });


    }
    public void preloadAd(Activity var1)
    {

    }

    public void getUserCode()
    {
        new Thread(new Runnable(){
            @Override
            public void run() {
                request.getUserCode(
                        info.gaid,
                        info.imei,
                        info.mac,
                        tempData.countryCode//"CH"
                         );
            }
        }).start();
    }
    public CountryCurrencyData getCountryCurrencyData()
    {
        return tempData;
    }

    public void  startPay(Activity var1,int payMode,CallBackFunction callBack) throws Exception {
        if(isPlayerIng == true)
        {
            Log.i(TAG,"YouleSdkMgr.startPay 正在支付中");
            callBack.onCallBack(false);
            return;
        }

        isPlayerIng = true;
        LoadingDialog.getInstance(var1).show();//显示


        boolean isAd = false;
        if(isAd == false && (request.userCode.length() <= 0 || request.choiceId.length() <= 0 || request.paymentType.length() <= 0))
        {
            Log.i(TAG,"YouleSdkMgr.startPay sdk初始化参数错误；userCode:"+request.userCode+";choiceId:"+request.choiceId+";paymentId"+request.paymentType);
            isAd = true;
        }
        if(isAd == false && (tempData == null || tempData.smsOptimalAmount <= 0))
        {
            Log.i(TAG,"YouleSdkMgr.startPay 没有合适的价格");
            isAd = true;
        }

        if(isAd == false && (tempData == null || tempData.smsOptimalAmount <= 0))
        {
            Log.i(TAG,"YouleSdkMgr.startPay 没有合适的价格");
            isAd = true;
        }

        if(isAd == false && (request.paymentType.indexOf("AD") != -1))
        {
            Log.i(TAG,"YouleSdkMgr.startPay 支付方式为广告");
            isAd = true;
        }

        if( isAd == true)
        {
            isPlayerIng = false;
            callBack.onCallBack(false);
            LoadingDialog.getInstance(var1).hide();//显示
            return;
        }




        paySdkStartPay(var1,payMode, new CallBackFunction() {
            @Override
            public void onCallBack(boolean data) {

                Log.i(TAG,"paySdkStartPay"+data);
                isPlayerIng = false;
                callBack.onCallBack(data);
                LoadingDialog.getInstance(var1).hide();//显示
            }
        });
    }
    public void paySdkStartPay(Activity var1,int payMode,CallBackFunction callBack)
    {


        new Thread(new Runnable(){
            @Override
            public void run() {
                YouleSdkMgr.getsInstance().payOrderNum = request.saveSmsPayRecord( tempData.countryCode,tempData.currency,""+tempData.smsOptimalAmount,"123");
            }
        }).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        StartPayEntity startPayEntity = new StartPayEntity();
        startPayEntity.amount =   tempData.smsOptimalAmount;
        startPayEntity.countryCode = tempData.countryCode;
        startPayEntity.currency = tempData.currency;
        startPayEntity.orderNum =this.payOrderNum;//order number
        startPayEntity.payMode = payMode;//order number

         PaySdkMgr.getsInstance().startPay(var1,startPayEntity, new StartPayCallBack() {

            @Override
            public void onOrderCreated(OrderEntity orderEntity) {
                Log.i(TAG,"PaySdkMgr.startPay.onOrderCreated:"+orderEntity);
            }

            @Override
            public void onPaySuccess(OrderEntity orderEntity) {
                Log.i(TAG,"PaySdkMgr.startPay.onPaySuccess:"+orderEntity);
                callBack.onCallBack(true);
                YouleSdkMgr.getsInstance().smsPaymentNotify(true);
            }

            @Override
            public void onPaying(OrderEntity orderEntity) {
                Log.i(TAG,"PaySdkMgr.startPay.onPaying:"+orderEntity);
            }

            @Override
            public void onPayFail(int i, OrderEntity orderEntity) {
                Log.i(TAG,"PaySdkMgr.startPay.onPayFail:"+orderEntity);
                callBack.onCallBack(false);
                YouleSdkMgr.getsInstance().smsPaymentNotify(false);
            }
        });
//        Log.i(TAG,"PaySdkMgr.startPay.payOrderNum:"+payOrderNum);
    }

    public void smsPaymentNotify(boolean  paymentStatus)
    {
        new Thread(new Runnable(){
            @Override
            public void run() {
                request.smsPaymentNotify(
                        YouleSdkMgr.getsInstance().payOrderNum,
                        paymentStatus == true ? "SUCCESSFUL" : "FAILED");
            }
        }).start();
    }

}
