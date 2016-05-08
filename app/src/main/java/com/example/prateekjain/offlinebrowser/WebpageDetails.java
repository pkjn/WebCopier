package com.example.prateekjain.offlinebrowser;

import java.util.Date;

import ollie.Model;
import ollie.annotation.Column;
import ollie.annotation.Table;

/**
 * Created by Prateek on 17-Mar-16.
 */
@Table("notes")
public class WebpageDetails extends Model {
    @Column("link")
    public String link;
    @Column("title")
    public String title;
    @Column("max_links")
    public String max_links;
    @Column("depth")
    public String depth;
    @Column("pdfDownload")
    public String pdfDownload;
    @Column("imageDownload")
    public String imageDownload;
    @Column("scriptDownload")
    public String scriptDownload;
    @Column("dateWebpage")
    public Date dateWebpage;
    @Column("size")
    public String size;
    @Column("count")
    public String count;
    @Column("max_size")
    public String max_size;
    @Column("status")
    public String status;
}
