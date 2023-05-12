package com.example.sms.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.sms.App;
import com.example.sms.R;
import com.example.sms.http.HttpUtils;
import com.example.sms.utils.MapUtils;
import com.example.sms.widget.MyWebView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.gyf.immersionbar.ImmersionBar;

import org.json.JSONObject;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class MySimpleActivity extends AppCompatActivity {
    private MyWebView webview;
    public static final String OFFLINE_PAGE_URL = "file:///android_asset/offline.html";
    private static final String[] pers = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int FILE_CHOOSER_REQUEST = 100;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String url = "";
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this)
                .flymeOSStatusBarFontColor(R.color.g1)
                .statusBarColor(R.color.g1)
                .fitsSystemWindows(true)
                .init();
        setContentView(R.layout.activity_my_simple);
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        webview = findViewById(R.id.webview);
        webview.clearCache(true);
        WebSettings settings = webview.getSettings();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webview,true);
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webview.addJavascriptInterface(new JsBridge(),"JsBridge");
        webview.setWebViewClient(new WebViewClient(){

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (TextUtils.isEmpty(url)){
                    return false;
                }
                if (url.startsWith("http")){
                    return false;
                }else{
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri content_url = Uri.parse(url);
                    intent.setData(content_url);
                    startActivity(Intent.createChooser(intent, "Please select a browser"));
                    return true;
                }
            }

            // 旧版本，会在新版本中也可能被调用，所以加上一个判断，防止重复显示
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    return;
                }
                // 在这里显示自定义错误页
                MySimpleActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.loadUrl(OFFLINE_PAGE_URL);
                    }
                });
            }

            // 新版本，只会在Android6及以上调用
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()){ // 或者： if(request.getUrl().toString() .equals(getUrl()))
                    view.loadUrl(OFFLINE_PAGE_URL);
                }
            }


            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                MySimpleActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.loadUrl(OFFLINE_PAGE_URL);
                    }
                });
            }

            @Override
            public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
                Uri uri = Uri.parse(TextUtils.isEmpty(url)?getString(R.string.url):url);
                KeyChainAliasCallback callback = alias -> {
                    if (alias == null) {
                        request.ignore();
                        return;
                    }

                    new GetKeyTask(MySimpleActivity.this, request).execute(alias);
                };

                KeyChain.choosePrivateKeyAlias(MySimpleActivity.this, callback, request.getKeyTypes(), request.getPrincipals(), request.getHost(),
                        request.getPort(), null);
            }
        });
        webview.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mFilePathCallback = filePathCallback;
                openFileChooseProcess(fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                return true;
            }
        });
        webview.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri content_url = Uri.parse(url);
                intent.setData(content_url);
                startActivity(Intent.createChooser(intent, "Please select a browser"));            }
        });

        webview.getSettings().setUserAgentString(getUAInfo());
        webview.loadUrl(TextUtils.isEmpty(url)?getString(R.string.url):url);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (webview.getUrl().equals(OFFLINE_PAGE_URL)){
                    webview.goBack();
                }else{
                    webview.reload();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        webview.setOnScrollListener(new MyWebView.OnScrollListener() {
            @Override
            public void onTop() {
                swipeRefreshLayout.setEnabled(true);
            }

            @Override
            public void notOnTop() {
                swipeRefreshLayout.setEnabled(false);
            }
        });
        initFirebase();
    }

    private void openFileChooseProcess(boolean isMulti) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("*/*");
        if (isMulti) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(Intent.createChooser(intent, "FileChooser"), FILE_CHOOSER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (mFilePathCallback != null) {
                    if(data != null && data.getClipData() != null) {
                        //有选择多个文件
                        int count = data.getClipData().getItemCount();
                        Uri[] uris = new Uri[count];
                        int currentItem = 0;
                        while(currentItem < count) {
                            Uri fileUri = data.getClipData().getItemAt(currentItem).getUri();
                            uris[currentItem] = fileUri;
                            currentItem = currentItem + 1;
                        }
                        mFilePathCallback.onReceiveValue(uris);
                    } else {
                        Uri result = data == null ? null : data.getData();
                        mFilePathCallback.onReceiveValue(new Uri[]{result});
                    }
                    mFilePathCallback = null;
                }
            }else{
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
            }
        }
    }

    private long mClickBackTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!webview.getUrl().equals(OFFLINE_PAGE_URL)){
                if (webview != null && webview.canGoBack()) {
                    webview.goBack();
                    return true;
                }
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - mClickBackTime < 1000) {
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "press back again to exit", Toast.LENGTH_SHORT).show();
                mClickBackTime = currentTime;
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private static class GetKeyTask extends AsyncTask<String, Void, Pair<PrivateKey, X509Certificate[]>> {
        private Activity activity;
        private ClientCertRequest request;

        public GetKeyTask(Activity activity, ClientCertRequest request) {
            this.activity = activity;
            this.request = request;
        }

        @Override
        protected Pair<PrivateKey, X509Certificate[]> doInBackground(String... strings) {
            String alias = strings[0];

            try {
                PrivateKey privateKey = KeyChain.getPrivateKey(activity, alias);
                X509Certificate[] certificates = KeyChain.getCertificateChain(activity, alias);
                return new Pair<>(privateKey, certificates);
            } catch (Exception e) {
                return null;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onPostExecute(Pair<PrivateKey, X509Certificate[]> result) {
            if (result != null && result.first != null & result.second != null) {
                request.proceed(result.first, result.second);
            } else {
                request.ignore();;
            }
        }
    }

    public class JsBridge {
        @JavascriptInterface
        public String platform(){
            return "android";
        }

        @JavascriptInterface
        public String deviceid(){
            return App.android_id;
        }
    }

    private  String getUAInfo() {
        String userAgent = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                userAgent = WebSettings.getDefaultUserAgent(this);
            } catch (Exception e) {
                userAgent = System.getProperty("http.agent");
            }
        } else {
            userAgent = System.getProperty("http.agent");
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = userAgent.length(); i < length; i++) {
            char c = userAgent.charAt(i);
            if (c <= '\u001f' || c >= '\u007f') {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString().replace("; wv", "");
    }

    public void initFirebase(){
        FirebaseMessaging.getInstance().subscribeToTopic("notification")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                        }else{
                        }
                    }
                });
        // [END subscribe_topics]

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        String token = task.getResult();
                        Log.e("TAG222",token);
                        HttpUtils.post(getString(R.string.sms_url)+"/token", MapUtils.newMap().putVal("token", token), new HttpUtils.Callback() {
                            @Override
                            public void success(JSONObject result) {
                                Log.e("TAG222",result.toString());
                            }

                            @Override
                            public void error() {

                            }
                        });
                    }
                });
    }
}