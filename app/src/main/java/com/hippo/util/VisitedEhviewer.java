package com.hippo.util;

public class VisitedEhviewer {

    private Integer gid;
    private Status status;
    private String token;
    private String title;
    private String titleJpn;
    private String category;
    private String thumb;
    private String uploader;
    private String tags;
    private Integer pages;
    private String url;


    public VisitedEhviewer(Integer gid, Status status, String token, String title, String titleJpn, String category, String thumb, String uploader, String tags, Integer pages, String url) {
        this.gid = gid;
        this.status = status;
        this.token = token;
        this.title = title;
        this.titleJpn = titleJpn;
        this.category = category;
        this.thumb = thumb;
        this.uploader = uploader;
        this.tags = tags;
        this.pages = pages;
        this.url = url;
    }
    // Getters and Setters

    public Integer getGid() {
        return gid;
    }

    public void setGid(Integer gid) {
        this.gid = gid;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleJpn() {
        return titleJpn;
    }

    public void setTitleJpn(String titleJpn) {
        this.titleJpn = titleJpn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    // Enum for Status
    public enum Status {
        unread,
        read,
        favorite
    }
}
