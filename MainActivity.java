package com.ediptan.kurumsalpuantaj;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.print.PrintManager;
import android.webkit.*;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.view.View;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.*;
import com.google.android.ump.*;

import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import com.android.billingclient.api.*;

/** Offline WebView host with Google Play subscription billing. */
public class MainActivity extends Activity {
    private static final String PRODUCT_ID = "puantaj_pro";
    private static final String MONTHLY_PLAN = "aylik";
    private static final String YEARLY_PLAN = "yillik-v2";
    // Google-provided TEST ad units for the first internal-test release.
    // Live banner: ca-app-pub-5866785474078079/3864793547
    // Live interstitial: ca-app-pub-5866785474078079/4543450391
    private static final String BANNER_AD = "ca-app-pub-5866785474078079/3864793547";
    private static final String FULL_AD = "ca-app-pub-5866785474078079/4543450391";
    private BillingClient billingClient;
    private ConsentInformation consentInformation;
    private boolean adsInitialized = false;
    private boolean bannerLoaded = false;
    private AdView bannerAd;
    private FrameLayout bannerSlot;
    private InterstitialAd fullAd;
    private volatile boolean entitlementKnown = false;
    private volatile boolean pro = true;
    private int actionCount = 0;
    private long lastFullAd = 0L;
    private WebView webView;
    private WebView printView;
    private FrameLayout root;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingText;
    private static final int OPEN_FILE = 21, SAVE_FILE = 22;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new FrameLayout(this);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                    | android.view.WindowInsets.Type.displayCutout() | android.view.WindowInsets.Type.ime());
                root.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else root.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setupWebView();
        setupBilling();
        setupAds();
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
    }

    @Override protected void onResume() {
        super.onResume(); queryEntitlement();
        if (bannerAd != null) bannerAd.resume();
    }
    @Override protected void onPause() {
        if (bannerAd != null) bannerAd.pause();
        super.onPause();
    }

    private void setupWebView() {
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.addJavascriptInterface(new NativeFiles(), "PuantajNative");
        webView.addJavascriptInterface(new PlayBillingBridge(), "PlayBilling");
        webView.addJavascriptInterface(new AdBridge(), "PuantajAds");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                // Some document providers label JSON as text/plain or octet-stream.
                intent.setType("*/*");
                try { startActivityForResult(intent, OPEN_FILE); }
                catch (Exception error) { fileCallback.onReceiveValue(null); fileCallback=null; notice("Dosya seçici açılamadı."); }
                return true;
            }
            @Override public boolean onJsAlert(WebView view,String url,String message,JsResult result) {
                new AlertDialog.Builder(MainActivity.this).setMessage(message).setPositiveButton("Tamam",(d,w)->result.confirm()).setOnCancelListener(d->result.cancel()).show();
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view,String url) { queryEntitlement(); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url=request.getUrl().toString();
                if (url.startsWith("file:///android_asset/") && !url.contains("..")) return false;
                notice("Bu bağlantı uygulama içinde açılamıyor."); return true;
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                root.removeView(view); view.destroy(); setupWebView();
                notice("Ekran yeniden açıldı; kaydedilmiş verileriniz korunuyor."); return true;
            }
        });
        root.addView(webView, new FrameLayout.LayoutParams(-1,-1));
        webView.loadUrl("file:///android_asset/index.html");
    }

private void setupBilling() {
          billingClient = BillingClient.newBuilder(this)
              .setListener((result, purchases) -> {
                  if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null)
                      for (Purchase purchase : purchases) handlePurchase(purchase);
                  else if (result.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED)
                      runJs("window.onProBillingError && window.onProBillingError(" + result.getResponseCode() + ")");
              })
              .enableAutoServiceReconnection()
              .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
              .build();
          billingClient.startConnection(new BillingClientStateListener() {
              public void onBillingSetupFinished(BillingResult result) { if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) queryEntitlement(); }
              public void onBillingServiceDisconnected() { }
          });
      }
      private void runJs(String script) { runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(script, null); }); }
      private void queryEntitlement() {
          if (billingClient == null || !billingClient.isReady()) return;
          billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(), (result, purchases) -> {
              if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
              boolean active = false;
              for (Purchase purchase : purchases) {
                  if (purchase.getProducts().contains(PRODUCT_ID) && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) { active = true; handlePurchase(purchase); }
              }
              pro = active;
              entitlementKnown = true;
              runOnUiThread(this::updateAdState);
              runJs("window.onProEntitlementChanged && window.onProEntitlementChanged(" + active + ")");
          });
      }
      private void handlePurchase(Purchase purchase) {
          if (!purchase.getProducts().contains(PRODUCT_ID) || purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
          pro = true;
          entitlementKnown = true;
          runOnUiThread(this::updateAdState);
          if (purchase.isAcknowledged()) { runJs("window.onProEntitlementChanged && window.onProEntitlementChanged(true)"); return; }
          billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(), result -> {
              if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) runJs("window.onProEntitlementChanged && window.onProEntitlementChanged(true)");
          });
      }
      private final class PlayBillingBridge {
          @JavascriptInterface public void showPrivacyOptions() {
              runOnUiThread(() -> {
                  if (consentInformation != null
                      && consentInformation.getPrivacyOptionsRequirementStatus()
                        == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                      UserMessagingPlatform.showPrivacyOptionsForm(MainActivity.this,
                          error -> { if (consentInformation.canRequestAds()) initializeAdsIfAllowed(); });
                  } else notice("Bu hesap için ek reklam gizlilik seçeneği gerekmiyor.");
              });
          }
          @JavascriptInterface public void isPro() { runOnUiThread(() -> queryEntitlement()); }
          @JavascriptInterface public void restorePurchases() { runOnUiThread(() -> queryEntitlement()); }
          // A subscription query must not run before the Play Billing connection is ready.
          private void withBillingReady(Runnable ready, Runnable failed, int retries) {
              if (billingClient == null) { failed.run(); return; }
              if (billingClient.isReady()) { ready.run(); return; }
              if (retries <= 0) { failed.run(); return; }
              new android.os.Handler(android.os.Looper.getMainLooper())
                  .postDelayed(() -> withBillingReady(ready, failed, retries - 1), 500);
          }

          @JavascriptInterface public void loadProPrices(int request) {
              runOnUiThread(() -> withBillingReady(() -> {
                  QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                      .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build();
                  billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder()
                      .setProductList(Collections.singletonList(product)).build(), (result, details) -> {
                          String monthly = null;
                          String yearly = null;
                          if (result.getResponseCode() == BillingClient.BillingResponseCode.OK
                                  && details.getProductDetailsList() != null) {
                              for (ProductDetails detail : details.getProductDetailsList()) {
                                  if (PRODUCT_ID.equals(detail.getProductId())) {
                                      monthly = recurringPrice(detail, MONTHLY_PLAN, "P1M");
                                      yearly = recurringPrice(detail, YEARLY_PLAN, "P1Y");
                                  }
                              }
                          }
                          sendProPrices(request, monthly, yearly);
                      });
              }, () -> sendProPrices(request, null, null), 20));
          }

          private ProductDetails.SubscriptionOfferDetails basePlanOffer(ProductDetails detail, String plan) {
              if (detail.getSubscriptionOfferDetails() == null) return null;
              for (ProductDetails.SubscriptionOfferDetails offer : detail.getSubscriptionOfferDetails()) {
                  if (plan.equals(offer.getBasePlanId()) && offer.getOfferId() == null) return offer;
              }
              return null;
          }

          private String recurringPrice(ProductDetails detail, String plan, String period) {
              ProductDetails.SubscriptionOfferDetails offer = basePlanOffer(detail, plan);
              if (offer == null) return null;
              for (ProductDetails.PricingPhase phase : offer.getPricingPhases().getPricingPhaseList()) {
                  if (period.equals(phase.getBillingPeriod())
                          && phase.getRecurrenceMode() == ProductDetails.RecurrenceMode.INFINITE_RECURRING) {
                      return phase.getFormattedPrice();
                  }
              }
              return null;
          }

          private void sendProPrices(int request, String monthly, String yearly) {
              runJs("window.onProPrices && window.onProPrices(" + request + ","
                  + (monthly == null ? "null" : org.json.JSONObject.quote(monthly)) + ","
                  + (yearly == null ? "null" : org.json.JSONObject.quote(yearly)) + ")");
          }

          @JavascriptInterface public void buyPro(String plan) {
              if (!MONTHLY_PLAN.equals(plan) && !"yillik".equals(plan)) return;
              String basePlanId = MONTHLY_PLAN.equals(plan) ? MONTHLY_PLAN : YEARLY_PLAN;
              String billingPeriod = MONTHLY_PLAN.equals(plan) ? "P1M" : "P1Y";
              runOnUiThread(() -> withBillingReady(() -> {
                  QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                      .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build();
                  billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder()
                      .setProductList(Collections.singletonList(product)).build(), (result, details) -> {
                          if (result.getResponseCode() != BillingClient.BillingResponseCode.OK
                                  || details.getProductDetailsList() == null || details.getProductDetailsList().isEmpty()) {
                              runJs("window.onProBillingError && window.onProBillingError(" + result.getResponseCode() + ")");
                              return;
                          }
                          ProductDetails detail = details.getProductDetailsList().get(0);
                          ProductDetails.SubscriptionOfferDetails selected = basePlanOffer(detail, basePlanId);
                          if (selected == null || recurringPrice(detail, basePlanId, billingPeriod) == null) {
                              runJs("window.onProBillingError && window.onProBillingError('PLAN_UNAVAILABLE')");
                              return;
                          }
                          runOnUiThread(() -> {
                              BillingFlowParams flow = BillingFlowParams.newBuilder()
                                  .setProductDetailsParamsList(Collections.singletonList(
                                      BillingFlowParams.ProductDetailsParams.newBuilder()
                                          .setProductDetails(detail).setOfferToken(selected.getOfferToken()).build()))
                                  .build();
                              BillingResult launched = billingClient.launchBillingFlow(MainActivity.this, flow);
                              if (launched.getResponseCode() != BillingClient.BillingResponseCode.OK
                                      && launched.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED)
                                  runJs("window.onProBillingError && window.onProBillingError("
                                      + launched.getResponseCode() + ")");
                          });
                      });
              }, () -> runJs("window.onProBillingError && window.onProBillingError('BILLING_NOT_READY')"), 20));
          }
      }
    private final class AdBridge {
        @JavascriptInterface public void completedPuantajAction() {
            runOnUiThread(() -> {
                if (!entitlementKnown || pro) return;
                actionCount++;
                if (actionCount < 5 || fullAd == null ||
                    android.os.SystemClock.elapsedRealtime() - lastFullAd < 180000L) return;
                actionCount = 0;
                lastFullAd = android.os.SystemClock.elapsedRealtime();
                InterstitialAd current = fullAd;
                fullAd = null;
                current.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() { requestFullAd(); }
                    @Override public void onAdFailedToShowFullScreenContent(AdError error) { requestFullAd(); }
                });
                current.show(MainActivity.this);
            });
        }
    }
    private void setupAds() {
        bannerSlot = new FrameLayout(this);
        bannerSlot.setVisibility(View.GONE);
        root.addView(bannerSlot, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(this, params,
            () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(this,
                error -> initializeAdsIfAllowed()),
            error -> initializeAdsIfAllowed());
        // Prior-session consent can be used once the update request has started.
        initializeAdsIfAllowed();
    }
    private void initializeAdsIfAllowed() {
        if (adsInitialized || consentInformation == null || !consentInformation.canRequestAds()) return;
        adsInitialized = true;
        MobileAds.initialize(this, status -> runOnUiThread(() -> {
            if (bannerAd != null) return;
            bannerAd = new AdView(this);
            bannerAd.setAdUnitId(BANNER_AD);
            bannerAd.setAdSize(AdSize.BANNER);
            bannerAd.setAdListener(new AdListener() {
                @Override public void onAdLoaded() {
                    bannerLoaded = true;
                    updateAdState();
                }
                @Override public void onAdFailedToLoad(LoadAdError error) {
                    bannerLoaded = false;
                    bannerSlot.setVisibility(View.GONE);
                    setBottomSpace(0);
                }
            });
            bannerSlot.addView(bannerAd,
                new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL));
            // Do not load any ad until Play Billing has confirmed this account is free.
            updateAdState();
        }));
    }
    private void requestFullAd() {
        if (!entitlementKnown || pro || consentInformation == null ||
                !consentInformation.canRequestAds() || fullAd != null) return;
        InterstitialAd.load(this, FULL_AD, new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override public void onAdLoaded(InterstitialAd ad) {
                    if (!entitlementKnown || pro) return;
                    fullAd = ad;
                }
                @Override public void onAdFailedToLoad(LoadAdError error) { fullAd = null; }
            });
    }
    private void setBottomSpace(int pixels) {
        if (webView == null) return;
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) webView.getLayoutParams();
        p.bottomMargin = pixels;
        webView.setLayoutParams(p);
    }
    private void updateAdState() {
        boolean allow = entitlementKnown && !pro && consentInformation != null
            && consentInformation.canRequestAds();
        if (bannerSlot == null) return;
        if (!allow) {
            bannerSlot.setVisibility(View.GONE);
            setBottomSpace(0);
            fullAd = null;
            actionCount = 0;
            return;
        }
        if (!adsInitialized) { initializeAdsIfAllowed(); return; }
        if (bannerAd == null) return;
        if (!bannerLoaded) {
            bannerAd.loadAd(new AdRequest.Builder().build());
            return;
        }
        bannerSlot.setVisibility(View.VISIBLE);
        setBottomSpace((int)(50 * getResources().getDisplayMetrics().density + 0.5f));
        if (fullAd == null) requestFullAd();
    }
    private final class NativeFiles {
        @JavascriptInterface public void saveText(String name,String mime,String text) {
            runOnUiThread(() -> {
                if(pendingText != null){ notice("Lütfen açık kaydetme işlemini tamamlayın."); return; }
                pendingText=text;
                Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(mime).putExtra(Intent.EXTRA_TITLE,name.replaceAll("[/\\\\]","_"));
                try { startActivityForResult(intent,SAVE_FILE); }
                catch(Exception error){ pendingText=null; notice("Kaydetme ekranı açılamadı."); }
            });
        }
        @JavascriptInterface public void printHtml(String html) {
            runOnUiThread(() -> {
                if(printView != null){ notice("Yazdırma hazırlanıyor."); return; }
                printView=new WebView(MainActivity.this);
                printView.setWebViewClient(new WebViewClient(){
                    @Override public void onPageFinished(WebView view,String url){
                        try {
                            PrintManager manager=(PrintManager)getSystemService(PRINT_SERVICE);
                            manager.print("Puantaj Raporu",view.createPrintDocumentAdapter("Puantaj Raporu"),null);
                        } catch(Exception error){ notice("Yazdırma ekranı açılamadı."); }
                        printView=null;
                    }
                });
                printView.loadDataWithBaseURL(null,html,"text/html","UTF-8",null);
            });
        }
    }

    @Override protected void onActivityResult(int request,int result,Intent intent) {
        super.onActivityResult(request,result,intent);
        if(request==OPEN_FILE && fileCallback!=null){
            fileCallback.onReceiveValue(result==RESULT_OK && intent!=null && intent.getData()!=null
                ? new Uri[]{intent.getData()} : null);
            fileCallback=null;
        }
        if(request==SAVE_FILE){
            String text=pendingText; pendingText=null;
            if(result!=RESULT_OK || intent==null || intent.getData()==null){ notice("Kaydetme iptal edildi."); return; }
            if(text==null){ notice("Kaydetme kesildi. Lütfen yeniden yedek alın."); return; }
            try(OutputStream stream=getContentResolver().openOutputStream(intent.getData())){
                if(stream==null) throw new java.io.IOException();
                stream.write(text.getBytes(StandardCharsets.UTF_8));
                notice("Dosya kaydedildi.");
            } catch(Exception error){ notice("Dosya kaydedilemedi. Lütfen tekrar deneyin."); }
        }
    }

    private void notice(String message){ Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private void handleBack(){
        webView.evaluateJavascript("(function(){var ms=Array.from(document.querySelectorAll('.modal.show'));if(ms.length){ms[ms.length-1].classList.remove('show');return true;}var t=document.getElementById('teamDetail');if(t&&t.style.display==='block'){closeTeam();return true;}var r=document.getElementById('reportsScreen');if(r&&r.style.display==='block'){closeReports();return true;}if(!location.pathname.endsWith('/index.html')){location.href='index.html';return true;}return false;})()", value -> {if("false".equals(value))finish();});
    }
    @Override public void onBackPressed(){handleBack();}
    @Override protected void onDestroy(){
        if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;}
        if(billingClient!=null) billingClient.endConnection();
        if(bannerAd!=null) { bannerAd.destroy(); bannerAd=null; }
        if(webView!=null){root.removeView(webView);webView.destroy();}
        super.onDestroy();
    }
}

