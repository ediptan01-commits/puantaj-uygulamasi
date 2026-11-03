package com.ediptan.kurumsalpuantaj;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.android.billingclient.api.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Entitlements come from the verification service, never WebView storage. */
public final class PlayBillingBridge {
    private final Activity activity;
    private final WebView view;
    private final BillingClient client;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final JSONObject config;
    private final java.util.function.Consumer<Boolean> entitlementListener;
    private boolean active, closed, querying, verifiedState, purchasing;
    private int generation;
    private final List<Purchase> owned = new ArrayList<>();

    public PlayBillingBridge(Activity activity, WebView view, java.util.function.Consumer<Boolean> listener) {
        entitlementListener=listener;
        this.activity=activity; this.view=view;
        JSONObject settings;
        try (InputStream in=activity.getAssets().open("billing-config.json")) {
            settings=new JSONObject(new java.util.Scanner(in,"UTF-8").useDelimiter("\\A").next());
        } catch(Exception e) { settings=new JSONObject(); }
        config=settings;
        client=BillingClient.newBuilder(activity)
            .setListener((result,purchases)->activity.runOnUiThread(()->{
                if(closed)return;
                purchasing=false;
                if(result.getResponseCode()==BillingClient.BillingResponseCode.OK) refresh();
                else if(result.getResponseCode()==BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) refresh();
                else message(result.getResponseCode()==BillingClient.BillingResponseCode.USER_CANCELED
                    ? "Satın alma iptal edildi." : "Satın alma tamamlanamadı. Lütfen yeniden deneyin.");
            }))
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection().build();
        connect();
    }
    private void connect() {
        client.startConnection(new BillingClientStateListener(){
            public void onBillingSetupFinished(BillingResult r){activity.runOnUiThread(()->{
                if(r.getResponseCode()==0){refresh();getPlans();} else message("Google Play bağlantısı kurulamadı.");
            });}
            public void onBillingServiceDisconnected(){activity.runOnUiThread(()->message("Google Play bağlantısı kesildi. Yeniden deneyin."));}
        });
    }
    private boolean configured(){
        try { URI u=new URI(config.optString("verificationUrl"));
            return "https".equals(u.getScheme()) && u.getHost()!=null && u.getUserInfo()==null;
        }catch(Exception e){return false;}
    }
    private void js(String code){ if(!closed)view.evaluateJavascript(code,null); }
    private void message(String text){js("window.onProBillingMessage && window.onProBillingMessage("+JSONObject.quote(text)+")");}
    private void entitlement(boolean value){active=value; verifiedState=true; entitlementListener.accept(value); js("window.onProEntitlementChanged && window.onProEntitlementChanged("+value+")");}
    private void unknown(){active=false;verifiedState=false;entitlementListener.accept(null);js("window.onProEntitlementChanged && window.onProEntitlementChanged(false)");}
    @JavascriptInterface public void isPro(){activity.runOnUiThread(()->{js("window.onProEntitlementChanged && window.onProEntitlementChanged("+active+")");refresh();});}
    @JavascriptInterface public void restorePurchases(){activity.runOnUiThread(this::refresh);}
    public void refresh(){
        if(closed||querying)return;
        if(!client.isReady()){message("Google Play hazırlanıyor. Lütfen yeniden deneyin.");return;}
        if(!configured()){unknown();message("PRO hazırlanıyor. Uygulamanın tüm özellikleri ücretsiz ve sınırsızdır.");return;}
        querying=true; owned.clear(); int current=++generation;
        query(BillingClient.ProductType.INAPP,current,()->query(BillingClient.ProductType.SUBS,current,()->verifyAll(current)));
    }
    private void query(String type,int current,Runnable next){
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build(),(r,list)->activity.runOnUiThread(()->{
            if(closed||current!=generation)return;
            if(r.getResponseCode()!=0){querying=false;unknown();message("Satın almalar kontrol edilemedi. Yeniden deneyin.");return;}
            owned.addAll(list); next.run();
        }));
    }
    private boolean known(Purchase p){
        for(String id:p.getProducts()) if(id.equals("kurumsal_puantaj_pro")||id.equals(config.optString("subscriptionProductId")))return true;
        return false;
    }
    private void verifyAll(int current){
        List<Purchase> snapshot=new ArrayList<>(owned);
        worker.execute(()->{
            boolean any=false,failed=false,pending=false;
            List<Purchase> verified=new ArrayList<>();
            for(Purchase p:snapshot){
                if(!known(p))continue;
                if(p.getPurchaseState()==Purchase.PurchaseState.PENDING){pending=true;continue;}
                if(p.getPurchaseState()!=Purchase.PurchaseState.PURCHASED)continue;
                try{if(verify(p)){any=true;verified.add(p);}}catch(Exception e){failed=true;}
            }
            final boolean granted=any,error=failed,waiting=pending;
            activity.runOnUiThread(()->{
                if(closed||current!=generation)return;
                querying=false; if(error&&!granted)unknown();else entitlement(granted);
                message(error?"Doğrulama tamamlanamadı. Yeniden deneyin.":waiting?"Ödeme bekleniyor. Onaylanınca yeniden kontrol edilecek.":granted?"PRO satın almanız doğrulandı; reklamsız kullanım hakkınız korunur.":"Etkin PRO satın alması bulunamadı. Ücretsiz kullanım sınırsızdır.");
                for(Purchase p:verified)if(!p.isAcknowledged())client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(),
                    r->activity.runOnUiThread(()->{if(r.getResponseCode()!=0)message("Satın alma onayı tamamlanamadı. Uygulamayı yeniden açın.");}));
            });
        });
    }
    private boolean verify(Purchase p)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(config.getString("verificationUrl")).openConnection();
        try{
            c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(10000);
            c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
            JSONObject request=new JSONObject().put("packageName",activity.getPackageName())
                .put("purchaseToken",p.getPurchaseToken()).put("products",new JSONArray(p.getProducts()));
            try(OutputStream out=c.getOutputStream()){out.write(request.toString().getBytes(StandardCharsets.UTF_8));}
            if(c.getResponseCode()!=200)throw new IOException("verification failed");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] buffer=new byte[1024];int count;
                while((count=in.read(buffer))!=-1){if(bytes.size()+count>16384)throw new IOException("response too large");bytes.write(buffer,0,count);}
            }
            JSONObject result=new JSONObject(bytes.toString("UTF-8"));
            return result.getBoolean("active") && p.getProducts().contains(result.getString("productId"))
                && activity.getPackageName().equals(result.getString("packageName"))
                && p.getPurchaseToken().equals(result.getString("purchaseToken"));
        }finally{c.disconnect();}
    }
    @JavascriptInterface public void getPlans(){activity.runOnUiThread(()->{
        if(closed)return;
        if(!client.isReady()){message("Google Play hazırlanıyor. Lütfen yeniden deneyin.");return;}
        String product=config.optString("subscriptionProductId");
        if(product.isEmpty()){message("Abonelik planları henüz yapılandırılmadı.");return;}
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(
            QueryProductDetailsParams.Product.newBuilder().setProductId(product).setProductType(BillingClient.ProductType.SUBS).build())).build(),
            (r,result)->activity.runOnUiThread(()->{
                if(closed)return;
                JSONArray plans=new JSONArray();
                try{
                    if(r.getResponseCode()==0)for(ProductDetails details:result.getProductDetailsList()){
                        if(!product.equals(details.getProductId())||details.getSubscriptionOfferDetails()==null)continue;
                        for(ProductDetails.SubscriptionOfferDetails offer:details.getSubscriptionOfferDetails()){
                            if(offer.getOfferId()!=null)continue;
                            for(String plan:Arrays.asList("aylik","yillik")){
                                JSONObject map=config.optJSONObject("basePlans");
                                if(map==null||!offer.getBasePlanId().equals(map.optString(plan)))continue;
                                List<ProductDetails.PricingPhase> phases=offer.getPricingPhases().getPricingPhaseList();
                                if(phases.size()!=1)continue;
                                ProductDetails.PricingPhase phase=phases.get(0);
                                String period="aylik".equals(plan)?"P1M":"P1Y";
                                if(!period.equals(phase.getBillingPeriod())||phase.getRecurrenceMode()!=ProductDetails.RecurrenceMode.INFINITE_RECURRING)continue;
                                plans.put(new JSONObject().put("plan",plan).put("price",phase.getFormattedPrice())
                                    .put("purchasable",config.optBoolean("salesEnabled")&&configured()));
                            }
                        }
                    }
                    js("window.onProCatalog && window.onProCatalog("+plans.toString()+")");
                }catch(JSONException e){message("Plan bilgileri okunamadı.");}
            }));
    });}
    @JavascriptInterface public void manageSubscription(){activity.runOnUiThread(()->{
        if(closed)return;
        android.net.Uri uri=android.net.Uri.parse("https://play.google.com/store/account/subscriptions").buildUpon()
            .appendQueryParameter("package",activity.getPackageName()).appendQueryParameter("sku",config.optString("subscriptionProductId")).build();
        try{activity.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,uri));}
        catch(Exception e){message("Google Play abonelikler ekranı açılamadı.");}
    });}
    @JavascriptInterface public void buyPro(String plan){activity.runOnUiThread(()->{
        if(closed)return;
        if(!config.optBoolean("salesEnabled")||!configured()){message("PRO satışları henüz açılmadı.");return;}
        if(active){message("PRO zaten etkin.");return;}
        if(querying||!verifiedState){message("Önce mevcut satın almalarınızı kontrol edin.");refresh();return;}
        if(purchasing){message("Satın alma işlemi sürüyor.");return;}
        String base=config.optJSONObject("basePlans")==null?"":config.optJSONObject("basePlans").optString(plan);
        String product=config.optString("subscriptionProductId");
        if(base.isEmpty()||product.isEmpty()||!client.isReady()){message("Bu plan şu anda kullanılamıyor.");return;}
        purchasing=true;
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(
            QueryProductDetailsParams.Product.newBuilder().setProductId(product).setProductType(BillingClient.ProductType.SUBS).build())).build(),
            (r,result)->activity.runOnUiThread(()->{
                if(closed)return;
                if(r.getResponseCode()!=0||result.getProductDetailsList().isEmpty()){purchasing=false;message("Plan Google Play'den alınamadı.");return;}
                ProductDetails details=result.getProductDetailsList().get(0);
                if(details.getSubscriptionOfferDetails()==null){purchasing=false;message("Plan bulunamadı.");return;}
                for(ProductDetails.SubscriptionOfferDetails offer:details.getSubscriptionOfferDetails()){
                    if(!base.equals(offer.getBasePlanId())||offer.getOfferId()!=null)continue;
                    List<ProductDetails.PricingPhase> phases=offer.getPricingPhases().getPricingPhaseList();
                    if(phases.size()!=1||!("aylik".equals(plan)?"P1M":"P1Y").equals(phases.get(0).getBillingPeriod())
                        ||phases.get(0).getRecurrenceMode()!=ProductDetails.RecurrenceMode.INFINITE_RECURRING)continue;
                    BillingResult launch=client.launchBillingFlow(activity,BillingFlowParams.newBuilder().setProductDetailsParamsList(
                        Collections.singletonList(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.getOfferToken()).build())).build());
                    if(launch.getResponseCode()!=0){purchasing=false;message("Ödeme ekranı açılamadı. Yeniden deneyin.");}
                    return;
                }
                purchasing=false;message("Seçilen temel plan kullanılamıyor.");
            }));
    });}
    public void close(){closed=true;++generation;client.endConnection();worker.shutdownNow();}
}
