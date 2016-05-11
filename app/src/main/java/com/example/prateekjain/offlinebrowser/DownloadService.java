package com.example.prateekjain.offlinebrowser;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ollie.query.Select;

public class DownloadService extends IntentService {
    public final String URL = "urlpath";
    public String TITLE="title";
    public String IMAGEDOWNLOAD="imageDownload";
    public String PDFDOWNLOAD="pdfDownload";
    public String SCRIPTDOWNLOAD="scriptDownload";
    public String MAXDEPTH="maxDepth";
    public String MAXLINKS="maxLinks";
    public final String NOTIFICATION = "service receiver";
    PowerManager.WakeLock wakeLock;
    static RequestQueue mRequestQueue;

    public DownloadService() {
        super("DownloadService");
    }



    //variable used in downloading
    LinkedList<String> queue;
    String host;
    int max_links_per_page,depth,level,count,max_size;
    String title=new String();
    String title_show=new String();
    boolean imageDownload=false;
    boolean scriptDownload=false;
    boolean pdfDownload=false;
    boolean download_again=false;
    ArrayList<String> all_urls;
    int flag=0;

    //function to download images and Scripts
    public Document downloadImageScript(Document doc) throws IOException{
        Elements scripts = doc.select("script");
        if(imageDownload){
            Elements images = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
            scripts.addAll(images);
        }
        for (Element image : scripts) {
            try{
                if(image.attr("src")==""){
                    //image.remove();
                    continue;
                }
                String src=image.attr("abs:src");
                src=src.replace("../", "");
                URL url = new URL(src);
                src=url.getPath();
                src=getExternalFilesDir(null)+"/"+title+src;
                final File f=new File(src);
                f.getParentFile().mkdirs();
                if(f.exists() && !download_again){
                    image.attr("src",src);
                    continue;
                }
                InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, url.toString(),
                        new Response.Listener<byte[]>() {
                            @Override
                            public void onResponse(byte[] response) {
                                // TODO handle the response
                                try {
                                    if (response!=null) {

                                        FileOutputStream outputStream;
                                        outputStream = new FileOutputStream(f);
                                        outputStream.write(response);
                                        outputStream.close();
                                    }
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    Log.d("KEY_ERROR", "UNABLE TO DOWNLOAD FILE");
                                    e.printStackTrace();
                                }
                            }
                        } ,new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO handle the error
                        error.printStackTrace();
                    }
                }, null);
                mRequestQueue.add(request);
                image.attr("src",src);
            }catch(Exception e){}
        }
        return doc;
    }


    //function to download pdf and styles
    public Document downloadStylePdf(Document doc) throws IOException{
        int counter=0;
        Elements styles = doc.select("link[rel=stylesheet]");
        if(pdfDownload==true)
        {
            Elements pdf = doc.select("a[href~=(?i)\\.(pdf)]");
            styles.addAll(pdf);
        }
        for (Element image : styles) {
            try{
                if(counter==max_links_per_page)
                    break;
                counter++;
                String src=image.attr("abs:href");
                Log.d("styles",src);
                src=src.replace("../", "");
                URL url = new URL(src);
                src=url.getPath();
                src=getExternalFilesDir(null)+"/"+title+src;
                final File f=new File(src);
                f.getParentFile().mkdirs();
                if(f.exists() && !download_again){
                    image.attr("href",src);
                    continue;
                }
                Log.d("styles not exist",src);
                InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, url.toString(),
                        new Response.Listener<byte[]>() {
                            @Override
                            public void onResponse(byte[] response) {
                                // TODO handle the response
                                try {
                                    if (response!=null) {

                                        FileOutputStream outputStream;
                                        outputStream = new FileOutputStream(f);
                                        outputStream.write(response);
                                        outputStream.close();
                                    }
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    Log.d("KEY_ERROR", "UNABLE TO DOWNLOAD FILE");
                                    e.printStackTrace();
                                }
                            }
                        } ,new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO handle the error
                        error.printStackTrace();
                    }
                }, null);
                mRequestQueue.add(request);
                image.attr("href",src);
            }catch(Exception e){}
        }
        return doc;
    }


    //function to traverse links and add to queue
    public Document traverseLinks(Document doc){
        int counter=0;
        Elements links=(Elements) doc.select("a[href]");
        for(Element link: links){
            try{
                if(link.attr("href").trim().equals("") || link.attr("href").trim().equals("#"))
                    continue;
                String src=link.attr("abs:href");
                if(src.contains(".pdf"))
                    continue;
                src=src.replace("../", "");
                URL url=new URL(src);
                Log.d("links",src);
                if(!url.getHost().equals(host)){
                    String host_name=url.getHost();
                    int counterr = 0;
                    for( int i=0; i<host_name.length(); i++ ) {
                        if( host_name.charAt(i) == '.' ) {
                            counterr++;
                        }
                    }
                    if(counterr<2){
                        host_name="www."+host_name;
                    }
                    String url_host=host_name.split("\\.")[1];
                    if(!url_host.equals(host)){
                        continue;
                    }
                }
                Log.d("links downloading",src);
                if(!queue.contains(src) && level!=(depth+2) && counter<max_links_per_page){
                    queue.add(src);
                    all_urls.add(src);
                    counter++;
                }
                String directory_path=url.getPath();
                directory_path=getExternalFilesDir(null)+"/"+title+directory_path;
                Log.d("links downloading path",directory_path);

                if(directory_path.endsWith("/"))
                    directory_path+="index.html";
                else if(directory_path.contains(".")){
                    int pos = directory_path.lastIndexOf(".");
                    if(directory_path.substring(pos,directory_path.length()).contains("/")){
                        directory_path+=".html";
                    }
                    else{
                        directory_path=directory_path.substring(0, pos);
                        directory_path+=".html";
                    }
                }

                link.attr("href",directory_path);
                if(!all_urls.contains(src))
                    link.attr("href",url.toString());

            }catch(Exception e){Log.d("Link Error:",e.toString());}
        }
        return doc;
    }

    //other things to refine
    public Document refinePage(Document doc,String name){
        Elements links=(Elements) doc.select("base[href]");
        for(Element link: links){
            link.attr("href",name);
        }
        return doc;
    }

    public void downloadLink(String s) {
        try {
            URL url1 = new URL(s);
            String baseUri = url1.getProtocol() + "://" + url1.getHost() + "/";
            BufferedReader reader = new BufferedReader(new InputStreamReader(url1.openStream()));
            BufferedWriter writer;
            File file;
            if (count == 1) {
                String host_name=url1.getHost();
                int counterr = 0;
                for( int i=0; i<host_name.length(); i++ ) {
                    if( host_name.charAt(i) == '.' ) {
                        counterr++;
                    }
                }
                if(counterr<2){
                    host_name="www."+host_name;
                }
                host=host_name.split("\\.")[1];
                file = new File(getExternalFilesDir(null), "/" + title + "/index.html");
            } else {
                String directory_path = url1.getPath();
                directory_path = getExternalFilesDir(null) + "/" + title + directory_path;
                if(directory_path.endsWith("/"))
                    directory_path+="index.html";
                else if(directory_path.contains(".")){
                    int pos = directory_path.lastIndexOf(".");
                    if(directory_path.substring(pos,directory_path.length()).contains("/")){
                        Log.d("inside",directory_path);
                        directory_path+=".html";
                    }
                    else{
                        Log.d("outside data",directory_path);
                        directory_path=directory_path.substring(0, pos);
                        directory_path+=".html";
                    }
                }


                file = new File(directory_path);

            }
            file.getParentFile().mkdirs();
            if (file.exists() && !download_again) {
                return;
            }
            if(count>1 && file.getPath().equals(getExternalFilesDir(null)+ "/" + title + "/index.html")){
                return;
            }
            file.createNewFile();
            writer = new BufferedWriter(new FileWriter(file));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            reader.close();
            writer.close();
            Document doc = Jsoup.parse(file, "utf-8", baseUri);
            if(!scriptDownload){
                doc.select("script").remove();
            }
            if(!imageDownload){
                doc.select("img").remove();
            }
            doc = downloadImageScript(doc);
            doc = downloadStylePdf(doc);
            doc = traverseLinks(doc);
            doc = refinePage(doc, "");
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(doc.html());
            writer.close();
        } catch (ConnectException e) {
            Log.d("Service error", e.toString());
            MainActivity.download_queue.clear();
            flag=1;
            publishResults(title_show,"INTERRUPTED",count,queue.size(),max_size);
            stopSelf();
        }
        catch(Exception e){
            Log.d("exception in links",e.toString());
        }
    }

    public void downloadWebsite(String url) {
        String s;
        queue.add("1");
        queue.add(url);
        all_urls.add(url);
        while(queue.size()!=0){
            count++;
            if(max_size<queue.size())
                max_size=queue.size();
            Log.d("titles",title_show);
            publishResults(title_show,"DOWNLOADING",count,queue.size(),max_size);
            s=queue.poll();
            if(s.equals(Integer.toString(level))){
                level++;
                if(queue.size()==0)
                    break;
                s=queue.poll();
                queue.add(Integer.toString(level));
            }
            downloadLink(s);
        }
        if(flag==0){
            publishResults(title_show,"DOWNLOADED",count,queue.size(),max_size);
            List<WebpageDetails> list= Select.from(WebpageDetails.class).fetch();
            Iterator ir=list.iterator();
            while(ir.hasNext()) {
                WebpageDetails wd = (WebpageDetails) ir.next();
                if (wd.title.equals(title_show)) {
                    wd.count = String.valueOf(count);
                    wd.max_size = String.valueOf(max_size);
                    wd.size = String.valueOf(queue.size());
                    wd.status = "DOWNLOADED";
                    wd.save();
                    break;
                }
            }
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        PowerManager mgr = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();
        flag=0;
        host="";
        level=1;
        count=0;
        max_size=1;
        all_urls=new ArrayList<>();
        mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new HurlStack());
        queue=new LinkedList<String>();
        String url= intent.getStringExtra(URL);
        title_show=intent.getStringExtra(TITLE);
        title="OfflineBrowser/"+title_show;
        title=title.replaceAll(" ", "");
        max_links_per_page=Integer.parseInt(intent.getStringExtra(MAXLINKS));
        depth=Integer.parseInt(intent.getStringExtra(MAXDEPTH));
        imageDownload=Boolean.valueOf(intent.getStringExtra(IMAGEDOWNLOAD));
        scriptDownload=Boolean.valueOf(intent.getStringExtra(SCRIPTDOWNLOAD));
        pdfDownload=Boolean.valueOf(intent.getStringExtra(PDFDOWNLOAD));
        download_again=Boolean.valueOf(intent.getStringExtra("download"));
        downloadWebsite(url);
        if(wakeLock.isHeld()){
            wakeLock.release();
        }
        if(MainActivity.download_queue.size()!=0){
            Intent new_download=MainActivity.download_queue.poll();
            startService(new_download);
        }
    }

    private void publishResults(String title1, String status,int count,int queue_size,int max_size) {

        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("TITLE", title1);
        intent.putExtra("STATUS",status);
        intent.putExtra("COUNT",count);
        intent.putExtra("QUEUE_SIZE",queue_size);
        intent.putExtra("MAX_SIZE",max_size);
        sendBroadcast(intent);
    }
}