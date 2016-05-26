package com.example.prateekjain.offlinebrowser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ollie.query.Select;

public class NewPage extends AppCompatActivity {
    Spinner spinner_depth,spinner_links;
    EditText url,title;
    CheckBox image,pdf,script;
    boolean editDetails=false;
    Integer max_links_array[]={5,10,50,100,500,100,5000};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        spinner_depth= (Spinner) findViewById(R.id.depthSpinner);
        spinner_links= (Spinner) findViewById(R.id.linksSpinner);
        url= (EditText) findViewById(R.id.url_webpage);
        title= (EditText) findViewById(R.id.title_webpage);
        image= (CheckBox) findViewById(R.id.downloadImages);
        pdf= (CheckBox) findViewById(R.id.downloadPdf);
        script=(CheckBox)findViewById(R.id.downloadScript);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                validateAndReturn(view);
            }
        });


        if(getIntent().hasExtra("title")){
            editDetails=true;
        }
        if(editDetails){
            Log.d("settings","editDetails");
            String data_title=getIntent().getExtras().getString("title");
            List<WebpageDetails> data=new ArrayList<WebpageDetails>();
            data= Select.from(WebpageDetails.class).orderBy("dateWebpage DESC").fetch();
            Iterator itr=data.iterator();
            WebpageDetails details=null;
            while(itr.hasNext()){
                details=(WebpageDetails)itr.next();
                if(details.title.equals(data_title)){
                    break;
                }
            }
            spinner_links.setSelection(Arrays.asList(max_links_array).indexOf(Integer.parseInt(details.max_links)));
            spinner_depth.setSelection(Integer.parseInt(details.depth) - 1);
            title.setText(details.title);
            title.setFocusable(false);
            title.setEnabled(false);
            url.setText(details.link);
            url.setFocusable(false);
            url.setEnabled(false);
            image.setChecked(Boolean.valueOf(details.imageDownload));
            pdf.setChecked(Boolean.valueOf(details.pdfDownload));
            script.setChecked(Boolean.valueOf(details.scriptDownload));
        }
        else{
            getSettingsData();
        }
    }

    private void validateAndReturn(View view) {
        String urlToSave=url.getText().toString();
        /*if(urlToSave.contains("https"))
            urlToSave=urlToSave.replace("https","http");*/
        if(!urlToSave.contains("http")){
            urlToSave="http://"+urlToSave;
        }
        try {
            URL url1 = new URL(urlToSave);
        } catch (MalformedURLException e) {
            // the URL is not in a valid form
            Snackbar.make(view, "Invalid URL", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        } catch (Exception e) {return;}


        //to check for special characters
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(title.getText().toString());
        if(m.find()){
            Snackbar.make(view, "Title cannot contain Special characters", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }

        //to check if it is not empty
        if(title.getText().toString().trim().equals("")){
            Snackbar.make(view, "Title cannot blank", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }
        if(url.getText().toString().trim().equals("")){
            Snackbar.make(view, "URL cannot blank", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }

        String title_line=title.getText().toString();

        //uppercase first letter of title
        title_line = title_line.substring(0, 1).toUpperCase() + title_line.substring(1);
        if(!editDetails) {
            if (titleCheck(title_line)) {
                Snackbar.make(view, "Title Already Exists", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                return;
            }
        }

        Intent intent=new Intent();
        intent.putExtra("url",urlToSave);
        intent.putExtra("title",title_line);
        intent.putExtra("imageDownload",String.valueOf(image.isChecked()));
        intent.putExtra("pdfDownload", String.valueOf(pdf.isChecked()));
        intent.putExtra("scriptDownload", String.valueOf(script.isChecked()));
        intent.putExtra("maxDepth", spinner_depth.getSelectedItem().toString());
        intent.putExtra("maxLinks", spinner_links.getSelectedItem().toString());
        if(editDetails){
            setResult(3, intent);
            Log.d("settings","setResult");
        }

        else
        setResult(2, intent);
        finish();//finishing activity
    }

    boolean titleCheck(String title_line){
        List<WebpageDetails> data=new ArrayList<WebpageDetails>();
        data= Select.from(WebpageDetails.class).orderBy("dateWebpage DESC").fetch();
        Iterator itr=data.iterator();
        WebpageDetails details=null;
        while(itr.hasNext()){
            details=(WebpageDetails)itr.next();
            if(details.title.equals(title_line)){
                return true;
            }
        }
        return false;
    }


    public void getSettingsData() {
        if(getIntent().hasExtra("urlToSave")){
            url.setText(getIntent().getExtras().getString("urlToSave"));
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NewPage.this);
        boolean imageDownload = prefs.getBoolean("imageDownload", true);
        boolean pdfDownload = prefs.getBoolean("pdfDownload", false);
        boolean scriptDownload = prefs.getBoolean("scriptDownload", true);
        String maxDepth = prefs.getString("maxDepth", "1");
        String maxLinks=prefs.getString("maxLinks", "50");
        spinner_links.setSelection(Arrays.asList(max_links_array).indexOf(Integer.parseInt(maxLinks)));
        spinner_depth.setSelection(Integer.parseInt(maxDepth) - 1);
        image.setChecked(imageDownload);
        pdf.setChecked(pdfDownload);
        script.setChecked(scriptDownload);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}