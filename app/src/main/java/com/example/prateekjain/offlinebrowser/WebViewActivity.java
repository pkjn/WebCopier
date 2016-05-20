package com.example.prateekjain.offlinebrowser;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.github.silvestrpredko.dotprogressbar.DotProgressBar;

public class WebViewActivity extends AppCompatActivity implements SearchView.OnQueryTextListener{
    //EditText link;
    WebView wv1;
    DotProgressBar progressBar;
    String textUrl="";
    SearchView searchView;
    boolean flag=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                //intent.putExtra("urlToSave",link.getText().toString());
                intent.putExtra("urlToSave",textUrl);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });


        //link=(EditText)findViewById(R.id.webview_url);
        //link.setSelectAllOnFocus(true);
        //link.clearFocus();
        wv1=(WebView)findViewById(R.id.webView);
        progressBar = (DotProgressBar) findViewById(R.id.dot_progress_bar);
        progressBar.setVisibility(View.GONE);
        String url = getIntent().getDataString();

        if(url==null){
            startWebView("http://www.google.com");
        }
        else{
            //link.setText(url);
            textUrl=url;
            startWebView(url);
        }
        /*link.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ( (actionId == EditorInfo.IME_ACTION_DONE) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    link.clearFocus();
                    progressBar.setVisibility(View.VISIBLE);
                    textUrl=link.getText().toString();
                    String url=textUrl;
                    if(!url.contains("http") && !url.contains("file")){
                        if(Patterns.WEB_URL.matcher(("http://"+url)).matches()){
                            url="http://"+url;
                        }
                        else{
                            url="https://www.google.com/search?q="+url;
                        }
                    }
                    startWebView(url);
                    return true;
                }
                else{
                    return false;
                }
            }
        });*/

    }
    private void startWebView(String url) {
        wv1.setWebViewClient(new WebViewClient() {

            //If you will not use this method url links are opeen in new brower not in webview
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                textUrl=url;
                //if(!flag)
                //searchView.setQuery(textUrl,false);
                view.loadUrl(url);
                progressBar.setVisibility(View.VISIBLE);
                return true;
            }
            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                //method to handle PAGE NOT AVAILABLE
                if(description.equals("net::ERR_NAME_NOT_RESOLVED")){
                    Snackbar.make(findViewById(android.R.id.content), "Check the URL", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    wv1.goBack();
                }

                if(description.equals("net::ERR_INTERNET_DISCONNECTED")){
                    Snackbar.make(findViewById(android.R.id.content), "Check Your Internet Connection", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    wv1.goBack();
                }
            }

            @TargetApi(android.os.Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
            }
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                //link.setText(url);
                textUrl=url;
                //if(!flag)
                searchView.setQuery(textUrl,false);
            }
        });

        wv1.setDrawingCacheBackgroundColor(0);
        wv1.setFocusableInTouchMode(true);
        wv1.setFocusable(true);
        wv1.setDrawingCacheEnabled(true);
        wv1.setWillNotCacheDrawing(false);
        wv1.setScrollbarFadingEnabled(true);
        wv1.setHorizontalScrollBarEnabled(false);
        WebSettings settings = wv1.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        if (Build.VERSION.SDK_INT > 16) {
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowFileAccess(true);
        }
        settings.setAppCacheEnabled(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        if (Build.VERSION.SDK_INT >= 19) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        wv1.loadUrl(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web_view, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setQuery(textUrl,true);
            }
        });
        return true;
    }

    public void onBackPressed() {
        if(wv1.canGoBack()) {
            wv1.goBack();
        } else {
            // Let the system handle the back button
            super.onBackPressed();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        flag=false;

        progressBar.setVisibility(View.VISIBLE);
        textUrl=query;
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        }
        String url=textUrl;
        if(!url.contains("http") && !url.contains("file")){
            if(Patterns.WEB_URL.matcher(("http://"+url)).matches()){
                url="http://"+url;
            }
            else{
                url="https://www.google.com/search?q="+url;
            }
        }
        startWebView(url);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        flag=true;
        return false;
    }

}
