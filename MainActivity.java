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
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Offline WebView host. No billing gate: all features are free. */
public class MainActivity extends Activity {
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
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
    }

    private void setupWebView() {
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.addJavascriptInterface(new NativeFiles(), "PuantajNative");
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
        if(webView!=null){root.removeView(webView);webView.destroy();}
        super.onDestroy();
    }
}
