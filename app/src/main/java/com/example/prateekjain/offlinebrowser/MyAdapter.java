package com.example.prateekjain.offlinebrowser;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.TextRoundCornerProgressBar;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Prateek Jain on 28-Aug-15.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private LayoutInflater inflator;
    List<WebpageDetails> data= Collections.emptyList();
    Context context;
    ClickListener clickListener;

    public void setClickListener(ClickListener clickListener)
    {
        this.clickListener=clickListener;
    }
    public MyAdapter(Context context,List<WebpageDetails> data){
        this.context=context;
        inflator=LayoutInflater.from(context);
        this.data=data;
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view=inflator.inflate(R.layout.row_layout, parent, false);
        MyViewHolder holder=new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        WebpageDetails current=data.get(position);
        Log.d("status",current.title);
        holder.title.setText(current.title);
        if(current.status.equals("DOWNLOADED") || current.status.equals("INTERRUPTED")){
            holder.progressBar.setVisibility(View.GONE);
            holder.cardview_buttons.setVisibility(View.VISIBLE);
        }
        else if(current.status.equals("DOWNLOADING") || current.status.equals("DELETING")){
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.cardview_buttons.setVisibility(View.GONE);
        }
        SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy");
        holder.date.setText(df.format(current.dateWebpage));
        holder.progressBar.setProgressText(current.count + "   /   " + current.max_size);
        int count=Integer.parseInt(current.count);
        int max_size=Integer.parseInt(current.max_size);
        int progress=(count*100)/max_size;
        if(progress>100)
            holder.progressBar.setProgress(100);
        else
            holder.progressBar.setProgress(progress);
    }
    public void addItem(WebpageDetails details){
        data.add(0, details);
        notifyDataSetChanged();
    }

    //for addition of items
    public void editItem(String title,int count,int size,String status,int max_size){
        int pos=-1;
        Iterator itr=data.iterator();
        WebpageDetails details=null;
        while(itr.hasNext()){
            pos++;
            details=(WebpageDetails)itr.next();
            if(details.title.equals(title)){
                break;
            }
        }
        details.count=String.valueOf(count);
        details.size=String.valueOf(size);
        details.max_size=String.valueOf(max_size);
        details.status=status;
        data.set(pos, details);
        notifyDataSetChanged();
    }


    //for deletion of items
    public void editItem(int position, int total_files, int deleted_files, String status) {
        if(status.equals("DELETING")) {
            WebpageDetails details = data.get(position);
            details.count = String.valueOf(deleted_files);
            details.max_size = String.valueOf(total_files);
            details.status = status;
            data.set(position, details);
        }
        else if(status.equals("DELETED")){
            data.remove(position);
        }
        notifyDataSetChanged();
    }

    public void updateDataFirstTime() {
                Iterator ir=data.iterator();
                while(ir.hasNext()){
                    WebpageDetails details=(WebpageDetails)ir.next();
                    if(details.status.equals("DOWNLOADING") || details.status.equals("DELETING")){
                        details.status="INTERRUPTED";
                        details.save();
                    }
                }
        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        return data.size();
    }

    public void saveChanges() {
        Iterator ir=data.iterator();
        while(ir.hasNext()){
            WebpageDetails details=(WebpageDetails)ir.next();
            details.save();
        }
        Log.d("savechanges", "details saved");
    }



    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView title,date;
        ImageButton download_button;
        ImageButton delete_button,setting_button;
        TextRoundCornerProgressBar progressBar;
        LinearLayout cardview_buttons;
        public MyViewHolder(View itemView) {
            super(itemView);
            cardview_buttons= (LinearLayout) itemView.findViewById(R.id.cardview_buttons);
            title= (TextView) itemView.findViewById(R.id.title);
            date= (TextView) itemView.findViewById(R.id.date);
            progressBar = (TextRoundCornerProgressBar) itemView.findViewById(R.id.progress_1);
            progressBar.setMax(100);
            download_button= (ImageButton) itemView.findViewById(R.id.download_button);
            delete_button= (ImageButton) itemView.findViewById(R.id.delete_button);
            setting_button=(ImageButton)itemView.findViewById(R.id.settings_button);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(clickListener!=null)
                        clickListener.itemClicked(v, getAdapterPosition(),data.get(getAdapterPosition()));
                }
            });
            download_button.setOnClickListener(this);
            delete_button.setOnClickListener(this);
            setting_button.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.download_button:
                    if(clickListener!=null)
                    clickListener.downloadClicked(v, getAdapterPosition(), data.get(getAdapterPosition()));
                    break;
                case R.id.delete_button:
                    if(clickListener!=null)
                        clickListener.deleteClicked(v, getAdapterPosition(), data.get(getAdapterPosition()));
                    break;
                case R.id.settings_button:
                    if(clickListener!=null)
                        clickListener.settingClicked(v, getAdapterPosition(), data.get(getAdapterPosition()));
                    break;
            }

        }
    }
    public interface ClickListener{
         void itemClicked(View view, int position, WebpageDetails data);
        void downloadClicked(View view, int position, WebpageDetails data);
        void deleteClicked(View view, int position, WebpageDetails data);
        void settingClicked(View view, int position, WebpageDetails data);
    }
}
