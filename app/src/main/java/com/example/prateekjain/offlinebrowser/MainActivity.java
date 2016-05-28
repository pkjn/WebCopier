package com.example.prateekjain.offlinebrowser;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.startapp.android.publish.StartAppSDK;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import ollie.Ollie;
import ollie.query.Select;

public class MainActivity extends AppCompatActivity implements MyAdapter.ClickListener {
    private RecyclerView recyclerView;
    MyAdapter adapter;
    final String db_name="OfflineBrowser";
    static LinkedList<Intent> download_queue=new LinkedList<>();
    LinearLayout viewWhenEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StartAppSDK.init(this, "204037436", true);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final FloatingActionMenu fab_menu= (FloatingActionMenu) findViewById(R.id.fab_menu);
        fab_menu.setClosedOnTouchOutside(false);
        FloatingActionButton type_link = (FloatingActionButton) findViewById(R.id.type_link);
        type_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab_menu.close(true);
                Intent intent = new Intent(getApplicationContext(), NewPage.class);
                startActivityForResult(intent, 2);
            }
        });
        FloatingActionButton find_link = (FloatingActionButton) findViewById(R.id.find_link);
        find_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab_menu.close(true);

                Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                startActivityForResult(intent,3);
            }
        });
        Ollie.with(getApplicationContext())
                .setName(db_name)
                .setVersion(2)
                .init();
        viewWhenEmpty=(LinearLayout)findViewById(R.id.viewWhenEmpty);
        recyclerView= (RecyclerView) findViewById(R.id.recyclerView);
        adapter=new MyAdapter(getApplicationContext(),getData());
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        //recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));

        if(!isMyServiceRunning(DownloadService.class))
            adapter.updateDataFirstTime();
        if(getIntent().hasExtra("urlToSave")){
            String url =" ";
            if(getIntent().getStringExtra("urlToSave")!=null){
                url=getIntent().getStringExtra("urlToSave");
            }
            Intent intent = new Intent(getApplicationContext(), NewPage.class);
            intent.putExtra("urlToSave",url);
            startActivityForResult(intent,2);
        }


        //execute when app runs for first time
        SharedPreferences pref = getApplicationContext().getSharedPreferences("OfflineBrowser", MODE_PRIVATE);
        boolean firstTime=pref.getBoolean("first_time", true);
        if(firstTime){
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("first_time", false);
            editor.commit();

            //create .nomeadia file in order to hide images from gallery
            try {
                File dir = getExternalFilesDir(null);
                File output = new File(dir, ".nomedia");
                boolean fileCreated = output.createNewFile();
            }catch (Exception e){}
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if(resultCode != RESULT_CANCELED) {

            if (requestCode == 2) {
                String url = data.getStringExtra("url");
                String title = data.getStringExtra("title");
                String imageDownload = data.getStringExtra("imageDownload");
                String pdfDownload = data.getStringExtra("pdfDownload");
                String scriptDownload = data.getStringExtra("scriptDownload");
                String maxDepth = data.getStringExtra("maxDepth");
                String maxLinks = data.getStringExtra("maxLinks");
                WebpageDetails webpage = new WebpageDetails();
                webpage.link=url;
                webpage.title=title;
                webpage.max_links=maxLinks;
                webpage.depth=maxDepth;
                webpage.pdfDownload=pdfDownload;
                webpage.imageDownload=imageDownload;
                webpage.scriptDownload=scriptDownload;
                webpage.dateWebpage=Calendar.getInstance().getTime();
                webpage.size=String.valueOf(1);
                webpage.count=String.valueOf(0);
                webpage.max_size=String.valueOf(1);
                webpage.status="DOWNLOADING";
                if(resultCode==2){
                    webpage.save();
                    adapter.addItem(webpage);
                }
                Intent intent = new Intent(this, DownloadService.class);
                intent.putExtra("urlpath",url);
                intent.putExtra("title", title.trim());
                intent.putExtra("imageDownload",imageDownload);
                intent.putExtra("pdfDownload",pdfDownload);
                intent.putExtra("scriptDownload",scriptDownload);
                intent.putExtra("maxDepth",maxDepth);
                intent.putExtra("maxLinks", maxLinks);
                viewWhenEmpty.setVisibility(View.GONE);
                if(resultCode==2)
                    intent.putExtra("download","false");
                else
                    intent.putExtra("download","true");
                if(new ConnectionDetector(getApplicationContext()).isConnectingToInternet()){
                    if(!isMyServiceRunning(DownloadService.class)){
                        startService(intent);

                    }
                    else{
                        download_queue.add(intent);
                    }

                }else{
                    adapter.editItem(title, 0,1,"INTERRUPTED",1);
                    Snackbar.make(findViewById(android.R.id.content), "No Network Detected.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(!isMyServiceRunning(DownloadService.class))
            adapter.updateDataFirstTime();
        registerReceiver(receiver, new IntentFilter(
                "service receiver"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public List<WebpageDetails> getData(){
        List<WebpageDetails> data=new ArrayList<WebpageDetails>();
        data= Select.from(WebpageDetails.class).orderBy("dateWebpage DESC").fetch();
        if(data.size()==0){
            viewWhenEmpty.setVisibility(View.VISIBLE);
        }

        return data;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent=new Intent(getApplicationContext(),SettingActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void itemClicked(View view, int position,WebpageDetails detail) {
        if(detail.status.equals("DELETED") || detail.status.equals("DELETING")    /* || detail.status.equals("INTERRUPTED")*/  ){

            Snackbar.make(findViewById(android.R.id.content), "Website is Interrupted, Download Again!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }

        String title=detail.title.replaceAll(" ","");
        String filePath=getExternalFilesDir(null) + "/OfflineBrowser/" + title + "/index.html";
        if(!new File(filePath).exists()){
            Snackbar.make(findViewById(android.R.id.content), "Website is Interrupted, Download Again!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }
        String urlVisit="file://"+filePath;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String defaultBrowser = prefs.getString("defaultBrowser", "choose");

        if(defaultBrowser.equals(getString(R.string.app_name))){

            Intent i = new Intent(getApplicationContext(),WebViewActivity.class);
            i.setData(Uri.parse(urlVisit));
            startActivity(i);
        }
        else{
            try
            {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse(urlVisit), "text/html");
                Intent chooser=Intent.createChooser(i,"Open With");
                startActivity(chooser);
            }
            catch(Exception e){}
        }
    }

    @Override
    public void downloadClicked(View view, int position, final WebpageDetails detail) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Sure To Download Again!");
        alertDialogBuilder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                downloadAgain(detail);
            }
        });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    void downloadAgain(WebpageDetails detail){
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra("urlpath",detail.link);
        intent.putExtra("title", detail.title.trim());
        intent.putExtra("imageDownload",detail.imageDownload);
        intent.putExtra("pdfDownload", detail.pdfDownload);
        intent.putExtra("scriptDownload", detail.scriptDownload);
        intent.putExtra("maxDepth", detail.depth);
        intent.putExtra("maxLinks", detail.max_links);
        intent.putExtra("download", "true");


        if(new ConnectionDetector(getApplicationContext()).isConnectingToInternet()){
            if(!isMyServiceRunning(DownloadService.class)){
                startService(intent);
            }
            else{
                download_queue.add(intent);
            }
        }
        else{
            adapter.editItem(detail.title, Integer.parseInt(detail.count),Integer.parseInt(detail.size),"DOWNLOADED",Integer.parseInt(detail.max_size));
            Snackbar.make(findViewById(android.R.id.content), "No Network Detected.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    @Override
    public void deleteClicked(View view,final int position,final WebpageDetails detail) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Sure To Delete");

        alertDialogBuilder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                delete_details = detail;
                delete_details.status="DELETING";
                delete_details.save();
                new DeleteFile().execute(new String[]{detail.title, String.valueOf(position)});
            }
        });

        alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }

        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    WebpageDetails delete_details=null;
    private class DeleteFile extends AsyncTask<String,String, Void> {
        int position=0;
        int total_files=0;
        int deleted_files=0;

        @Override
        protected Void doInBackground(String... params) {
            String folder_name=getExternalFilesDir(null) + "/OfflineBrowser/" + params[0].replaceAll(" ","");
            position=Integer.parseInt(params[1]);
            countRecursive(new File(folder_name));
            deleteRecursive(new File(folder_name));
            publishProgress("DELETED");
            delete_details.delete();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if(values[0].equals("DELETED")){
             getData();
            }
            adapter.editItem(position,total_files,deleted_files,values[0]);
        }

        //android code to delete folder and its contents
        void countRecursive(File fileOrDirectory) {
            if (fileOrDirectory.isDirectory()){
                if(fileOrDirectory.listFiles().length!=0)
                    total_files+=fileOrDirectory.listFiles().length;
                for (File child : fileOrDirectory.listFiles()) {
                    countRecursive(child);
                }
            }
        }
        void deleteRecursive(File fileOrDirectory) {
            if (fileOrDirectory.isDirectory()){
                for (File child : fileOrDirectory.listFiles()) {
                    deleteRecursive(child);
                    deleted_files++;
                    publishProgress("DELETING");
                }}
            fileOrDirectory.delete();
        }
    }


    @Override
    public void settingClicked(View view, int position, WebpageDetails detail) {
        Intent intent=new Intent(getApplicationContext(),NewPage.class);
        intent.putExtra("title",detail.title);
        startActivityForResult(intent,2);

    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String title = bundle.getString("TITLE");
                String status = bundle.getString("STATUS");
                int count=bundle.getInt("COUNT");
                int queue_size=bundle.getInt("QUEUE_SIZE");
                int max_size=bundle.getInt("MAX_SIZE");
                adapter.editItem(title, count,queue_size,status,max_size);
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        saveChanges();
    }

    //Check if a service is running or not
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private void saveChanges() {
        adapter.saveChanges();
    }
}
