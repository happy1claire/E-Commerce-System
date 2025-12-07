package com.cs6650.leaderless.model;

public class NodeAddress {
    private String url;

    public NodeAddress() {
    }

    public NodeAddress(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
