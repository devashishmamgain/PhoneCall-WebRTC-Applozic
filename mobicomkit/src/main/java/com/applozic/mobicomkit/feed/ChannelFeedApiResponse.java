package com.applozic.mobicomkit.feed;

import com.applozic.mobicommons.json.JsonMarker;

/**
 * Created by sunil on 12/1/16.
 */
public class ChannelFeedApiResponse extends JsonMarker {

    private static final String SUCCESS = "success";
    private String status;
    private String generatedAt;
    private ChannelFeed response;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public ChannelFeed getResponse() {
        return response;
    }

    public void setResponse(ChannelFeed response) {
        this.response = response;
    }

    public boolean isSuccess() {
        return SUCCESS.equals(status);
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "status='" + status + '\'' +
                ", generatedAt='" + generatedAt + '\'' +
                ", response='" + response + '\'' +
                '}';
    }
}
