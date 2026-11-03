package com.ediptan.kurumsalpuantaj;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;
import android.webkit.WebView;
import android.widget.Toast;
import com.google.android.gms.ads.*;
import com.google.android.ump.*;
import org.json.JSONObject;
import java.io.InputStream;

/** Dormant until explicitly enabled; no initialization or ad requests in this release. */
public final class AdvertisingController {
    private final Activity activity;
    private final FrameLayout slot;
    private final ConsentInformation consent;
    private final boolean enabled;
    private final String unitId;
    private Boolean pro;
    private boolean closed,consentStarted,initializing,initialized;
    private AdView banner;
    private WebView currentPage;

    public AdvertisingController(Activity activity,FrameLayout slot){
        this.activity=activity;this.slot=slot;slot.setVisibility(View.GONE);
        JSONObject settings;
        try(InputStream in=activity.getAssets().open("ads-config.json")){
            settings=new JSONObject(new java.util.Scanner(in,"UTF-8").useDelimiter("\\A").next());
        }catch(Exception e){settings=new JSONObject();}
        unitId=settings.optString("bannerUnitId");
        boolean test=settings.optBoolean("testMode",true);
        enabled=settings.optBoolean("enabled",false)
            &&unitId.matches("ca-app-pub-[0-9]+/[0-9]+")
            &&(test||(!unitId.contains("3940256099942544")&&!settings.optString("appId").contains("3940256099942544")));
        // Merely obtaining UMP state does not initialize Mobile Ads or request an ad.
        consent=UserMessagingPlatform.getConsentInformation(activity);
    }
    public void onEntitlement(Boolean value){
        if(closed)return;pro=value;
        if(!enabled||!Boolean.FALSE.equals(pro)){clearBanner();return;}
        if(!consentStarted){
            consentStarted=true;
            consent.requestConsentInfoUpdate(activity,new ConsentRequestParameters.Builder().build(),()->{
                if(closed)return;publishPrivacy();
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity,error->{if(!closed){publishPrivacy();loadIfAllowed();}});
            },error->{if(!closed){publishPrivacy();loadIfAllowed();}});
        }
        loadIfAllowed();
    }
    private void loadIfAllowed(){
        if(closed||!AdPolicy.mayRequest(enabled,pro,consent.canRequestAds()))return;
        if(!initialized){
            if(initializing)return;initializing=true;
            MobileAds.initialize(activity,status->activity.runOnUiThread(()->{
                if(closed)return;initializing=false;initialized=true;loadIfAllowed();
            }));return;
        }
        if(banner!=null)return;
        banner=new AdView(activity);banner.setAdUnitId(unitId);
        float density=activity.getResources().getDisplayMetrics().density;
        int width=Math.max(1,(int)((activity.getResources().getDisplayMetrics().widthPixels-slot.getPaddingLeft()-slot.getPaddingRight())/density));
        banner.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity,width));
        slot.addView(banner,new FrameLayout.LayoutParams(-1,-2));
        banner.setAdListener(new AdListener(){
            @Override public void onAdLoaded(){
                if(!closed&&AdPolicy.mayRequest(enabled,pro,consent.canRequestAds()))slot.setVisibility(View.VISIBLE);
                else clearBanner();
            }
            @Override public void onAdFailedToLoad(LoadAdError error){clearBanner();}
        });
        banner.loadAd(new AdRequest.Builder().build());
    }
    private void clearBanner(){
        slot.setVisibility(View.GONE);
        if(banner!=null){AdView old=banner;banner=null;slot.removeView(old);old.destroy();}
    }
    public void setPage(WebView page){currentPage=page;publishPrivacy();}
    private void publishPrivacy(){
        boolean required=enabled&&consent.getPrivacyOptionsRequirementStatus()==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        if(!closed&&currentPage!=null)currentPage.evaluateJavascript("window.onAdPrivacyRequired && window.onAdPrivacyRequired("+required+")",null);
    }
    public void showPrivacy(){
        if(closed)return;
        if(!enabled||consent.getPrivacyOptionsRequirementStatus()!=ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED){
            Toast.makeText(activity,"Reklam gizlilik tercihleri şu anda gerekli değil.",Toast.LENGTH_LONG).show();return;
        }
        clearBanner();UserMessagingPlatform.showPrivacyOptionsForm(activity,error->{if(!closed){publishPrivacy();loadIfAllowed();}});
    }
    public void pause(){if(banner!=null)banner.pause();}
    public void resume(){if(banner!=null)banner.resume();}
    public void close(){closed=true;currentPage=null;clearBanner();}
}
