package com.lifescc.cc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView web;
    private SwipeRefreshLayout refresh;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_CODE = 1001;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String startUrl = getString(R.string.start_url);

        refresh = findViewById(R.id.refresh);
        web = findViewById(R.id.web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        // Native bridge the web app calls: window.LifeApp.dial(), openExternal()
        web.addJavascriptInterface(new Bridge(), "LifeApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                if (refresh != null) refresh.setRefreshing(false);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> cb, FileChooserParams params) {
                filePathCallback = cb;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER_CODE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        refresh.setOnRefreshListener(() -> web.reload());

        // Hardware back button → navigate the web app / webview history
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Let the web app handle back first (closes sheets / tabs)
                web.evaluateJavascript(
                    "(function(){try{if(window.LIFE_APP_BACK){var r=window.LIFE_APP_BACK();return r==='handled'?'1':(r==='exit'?'exit':'0');}}catch(e){}return '0';})()",
                    value -> {
                        String v = value == null ? "" : value.replace("\"", "");
                        if ("1".equals(v)) return;                 // web app handled it
                        if (web.canGoBack() && !"exit".equals(v)) { web.goBack(); return; }
                        finish();
                    });
            }
        });

        if (savedInstanceState == null) {
            web.loadUrl(startUrl);
        } else {
            web.restoreState(savedInstanceState);
        }
    }

    private boolean handleUrl(String url) {
        if (url == null) return false;
        String low = url.toLowerCase();
        // Keep the portal inside the app; send everything else to the right app.
        if (low.startsWith("http://") || low.startsWith("https://")) {
            boolean isPortal = low.contains("portal.lifescc.com");
            boolean isWhatsApp = low.contains("wa.me") || low.contains("api.whatsapp.com") || low.contains("web.whatsapp.com");
            if (isPortal && !isWhatsApp) return false; // load in WebView
            openExternal(url);
            return true;
        }
        // tel:, sms:, mailto:, whatsapp:, intent:, etc.
        openExternal(url);
        return true;
    }

    private void openExternal(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No app to open this link", Toast.LENGTH_SHORT).show();
        }
    }

    public class Bridge {
        @JavascriptInterface
        public void dial(final String tel) {
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(tel.startsWith("tel:") ? tel : "tel:" + tel));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) { /* ignore */ }
            });
        }
        @JavascriptInterface
        public void openExternal(final String url) {
            runOnUiThread(() -> MainActivity.this.openExternal(url));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_CODE) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.getDataString() != null) {
                    results = new Uri[]{ Uri.parse(data.getDataString()) };
                } else if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    results = new Uri[n];
                    for (int i = 0; i < n; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }
}
