package com.applozic.mobicomkit.channel.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.MultipleChannelFeedApiResponse;
import com.applozic.mobicomkit.api.HttpRequestUtils;
import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicomkit.api.people.ChannelInfo;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicomkit.feed.ChannelFeed;
import com.applozic.mobicomkit.feed.ChannelFeedApiResponse;
import com.applozic.mobicomkit.feed.GroupInfoUpdate;
import com.applozic.mobicomkit.sync.SyncChannelFeed;
import com.applozic.mobicommons.json.GsonUtils;
import com.google.gson.reflect.TypeToken;

import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

/**
 * Created by sunil on 29/12/15.
 */
public class ChannelClientService extends MobiComKitClientService {
    private static final String CHANNEL_INFO_URL = "/rest/ws/group/info";
    private static final String CHANNEL_SYNC_URL = "/rest/ws/group/list";
    private static final String CREATE_CHANNEL_URL = "/rest/ws/group/create";
    private static final String CREATE_MULTIPLE_CHANNEL_URL = "/rest/ws/group/create/multiple";
    private static final String ADD_MEMBER_TO_CHANNEL_URL = "/rest/ws/group/add/member";
    private static final String REMOVE_MEMBER_FROM_CHANNEL_URL = "/rest/ws/group/remove/member";
    private static final String CHANNEL_UPDATE_URL = "/rest/ws/group/update";
    private static final String CHANNEL_LEFT_URL = "/rest/ws/group/left";
    private static final String ADD_MEMBER_TO_MULTIPLE_CHANNELS_URL = "/rest/ws/group/add/user";
    private static final String CHANNEL_DELETE_URL = "/rest/ws/group/delete";
    private static final String REMOVE_MEMBERS_FROM_MULTIPE_CHANNELS = "/rest/ws/group/remove/user";

    private static final String UPDATED_AT = "updatedAt";
    private static final String USER_ID = "userId";
    private static final String GROUP_ID = "groupId";
    private static final String CLIENT_GROUPID = "clientGroupId";
    private static final String GROUPIDS = "groupIds";
    private static final String CLIENT_GROUPIDs = "clientGroupIds";
    private static final String TAG = "ChannelClientService";
    private static ChannelClientService channelClientService;
    private HttpRequestUtils httpRequestUtils;


    private ChannelClientService(Context context) {
        super(context);
        this.context = context;
        this.httpRequestUtils = new HttpRequestUtils(context);
    }


    public static ChannelClientService getInstance(Context context) {
        if (channelClientService == null) {
            channelClientService = new ChannelClientService(context);
        }
        return channelClientService;
    }

    public String getChannelInfoUrl() {
        return getBaseUrl() + CHANNEL_INFO_URL;
    }

    public String getChannelSyncUrl() {
        return getBaseUrl() + CHANNEL_SYNC_URL;
    }

    public String getCreateChannelUrl() {
        return getBaseUrl() + CREATE_CHANNEL_URL;
    }

    public String getCreateMultipleChannelUrl() {
        return getBaseUrl() + CREATE_MULTIPLE_CHANNEL_URL;
    }

    public String getAddMemberToGroup() {
        return getBaseUrl() + ADD_MEMBER_TO_CHANNEL_URL;
    }

    public String getRemoveMemberUrl() {
        return getBaseUrl() + REMOVE_MEMBER_FROM_CHANNEL_URL;
    }

    public String getChannelUpdateUrl() {
        return getBaseUrl() + CHANNEL_UPDATE_URL;
    }

    public String getChannelLeftUrl() {
        return getBaseUrl() + CHANNEL_LEFT_URL;
    }

    public String getChannelDeleteUrl() {
        return getBaseUrl() + CHANNEL_DELETE_URL;
    }

    public String getAddMemberToMultipleChannelsUrl() {
        return getBaseUrl() + ADD_MEMBER_TO_MULTIPLE_CHANNELS_URL;
    }

    public String getRemoveMembersFromMultipChannels() {
        return getBaseUrl() + REMOVE_MEMBERS_FROM_MULTIPE_CHANNELS;
    }

    public ChannelFeed getChannelInfoByParameters(String parameters) {
        String response = "";
        try {
            response = httpRequestUtils.getResponse(getCredentials(), getChannelInfoUrl() + "?" + parameters, "application/json", "application/json");
            ChannelFeedApiResponse channelFeedApiResponse = (ChannelFeedApiResponse) GsonUtils.getObjectFromJson(response, ChannelFeedApiResponse.class);
            Log.i(TAG, "Channel info response  is :" + response);

            if (channelFeedApiResponse != null && channelFeedApiResponse.isSuccess()) {
                ChannelFeed channelFeed = channelFeedApiResponse.getResponse();
                return channelFeed;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public ChannelFeed getChannelInfo(String clientGroupId) {
        return getChannelInfoByParameters(CLIENT_GROUPID + "=" + clientGroupId);
    }

    public ChannelFeed getChannelInfo(Integer channelKey) {
        return getChannelInfoByParameters(GROUP_ID + "=" + channelKey);
    }

    public SyncChannelFeed getChannelFeed(String lastChannelSyncTime) {
        String url = getChannelSyncUrl() + "?" +
                UPDATED_AT
                + "=" + lastChannelSyncTime;
        try {
            String response = httpRequestUtils.getResponse(getCredentials(), url, "application/json", "application/json");
            Log.i(TAG, "Channel sync call response: " + response);
            return (SyncChannelFeed) GsonUtils.getObjectFromJson(response, SyncChannelFeed.class);
        } catch (Exception e) {
            return null;
        }
    }

    public ChannelFeed createChannel(ChannelInfo channelInfo) {
        ChannelFeed channelFeed = null;
        try {
            String jsonFromObject = GsonUtils.getJsonFromObject(channelInfo, channelInfo.getClass());
            String createChannelResponse = httpRequestUtils.postData(getCredentials(), getCreateChannelUrl(), "application/json", "application/json", jsonFromObject);
            Log.i(TAG, "Create channel Response :" + createChannelResponse);
            ChannelFeedApiResponse channelFeedApiResponse = (ChannelFeedApiResponse) GsonUtils.getObjectFromJson(createChannelResponse, ChannelFeedApiResponse.class);

            if (channelFeedApiResponse != null && channelFeedApiResponse.isSuccess()) {
                channelFeed = channelFeedApiResponse.getResponse();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return channelFeed;
    }

    public List<ChannelFeed> createMultipleChannels(List<ChannelInfo> channels) {
        List<ChannelFeed> channelFeeds = null;
        try {
            String jsonFromObject = GsonUtils.getJsonFromObject(channels, new TypeToken<List<ChannelInfo>>() {}.getType());
            String createChannelResponse = httpRequestUtils.postData(getCredentials(), getCreateMultipleChannelUrl(), "application/json", "application/json", jsonFromObject);
            Log.i(TAG, "Create Multiple channel Response :" + createChannelResponse);
            MultipleChannelFeedApiResponse channelFeedApiResponse = (MultipleChannelFeedApiResponse) GsonUtils.getObjectFromJson(createChannelResponse, MultipleChannelFeedApiResponse.class);

            if (channelFeedApiResponse != null && channelFeedApiResponse.isSuccess()) {
                channelFeeds = channelFeedApiResponse.getResponse();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return channelFeeds;
    }

    public ApiResponse removeMembersFromMultipleChannelsByChannelKeys(Set<Integer> channelKeys, Set<String> userIds){
        return removeMembersFromMultipleChannels(null,channelKeys, userIds);
    }

    public ApiResponse removeMembersFromMultipleChannelsByClientGroupIds(Set<String> clientGroupIds, Set<String> userIds){
        return removeMembersFromMultipleChannels(clientGroupIds,null, userIds);
    }

    private  ApiResponse removeMembersFromMultipleChannels(Set<String> clientGroupIds, Set<Integer> channelKeys, Set<String> userIds) {
        ApiResponse apiResponse = null;
        try {
            if (userIds != null && userIds.size()>0) {
                String parameters = "";
                if(clientGroupIds != null && clientGroupIds.size()>0){
                    for (String clientGroupId : clientGroupIds) {
                        parameters += CLIENT_GROUPIDs + "=" + URLEncoder.encode(clientGroupId, "UTF-8") + "&";
                    }
                } else if(channelKeys != null && channelKeys.size()>0){
                    for (Integer channelKey : channelKeys) {
                        parameters += GROUPIDS + "=" + channelKey + "&";
                    }
                }
                for(String userId:userIds){
                    parameters += USER_ID + "=" + URLEncoder.encode(userId, "UTF-8") + "&";
                }
                String url = getRemoveMembersFromMultipChannels() + "?" + parameters;
                String response = httpRequestUtils.getResponse(getCredentials(), url, "application/json", "application/json");
                apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                if (apiResponse != null) {
                    Log.i(TAG, "Channel remove members from channels response: " + apiResponse.getStatus());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apiResponse;
    }


    public synchronized ApiResponse addMemberToMultipleChannels(Set<String> clientGroupIds, Set<Integer> channelKeys, String userId) {
        ApiResponse apiResponse = null;
        try {
            if (!TextUtils.isEmpty(userId)) {
                String parameters = "";
                if(clientGroupIds != null && clientGroupIds.size()>0){
                    for (String clientGroupId : clientGroupIds) {
                        parameters += CLIENT_GROUPIDs + "=" + URLEncoder.encode(clientGroupId, "UTF-8") + "&";
                    }
                }else {
                    for (Integer channelKey : channelKeys) {
                        parameters += GROUPIDS + "=" + channelKey + "&";
                    }
                }
                String url = getAddMemberToMultipleChannelsUrl() + "?" + parameters + USER_ID + "=" + URLEncoder.encode(userId, "UTF-8");
                String response = httpRequestUtils.getResponse(getCredentials(), url, "application/json", "application/json");
                apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                if (apiResponse != null) {
                    Log.i(TAG, "Channel add member call response: " + apiResponse.getStatus());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    public ApiResponse addMemberToMultipleChannelsByChannelKey(Set<Integer> channelKeys, String userId) {
        return addMemberToMultipleChannels(null, channelKeys, userId);
    }

    public ApiResponse addMemberToMultipleChannelsByClientGroupIds(Set<String> clientGroupIds, String userId) {
        return addMemberToMultipleChannels(clientGroupIds, null, userId);
    }

    public synchronized ApiResponse addMemberToChannel(String clientGroupId, Integer channelKey, String userId) {
        try {
            String parameters = "";
            if (!TextUtils.isEmpty(clientGroupId)) {
                parameters = CLIENT_GROUPID + "=" + URLEncoder.encode(clientGroupId, "UTF-8");
            } else {
                parameters = GROUP_ID + "=" + channelKey;
            }
            if (!TextUtils.isEmpty(parameters) && !TextUtils.isEmpty(userId) ) {
                String url = getAddMemberToGroup() + "?" +
                        parameters + "&" + USER_ID + "=" + URLEncoder.encode(userId, "UTF-8");
                String response = httpRequestUtils.getResponse(getCredentials(), url, "application/json", "application/json");
                ApiResponse apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                if(apiResponse != null){
                    Log.i(TAG, "Channel add member call response: " + apiResponse.getStatus());
                }
                return apiResponse;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized ApiResponse addMemberToChannel(Integer channelKey, String userId) {
        return addMemberToChannel(null, channelKey, userId);
    }

    public synchronized ApiResponse addMemberToChannel(String clientGroupId, String userId) {
        return addMemberToChannel(clientGroupId, null, userId);
    }

    public synchronized ApiResponse removeMemberFromChannel(String clientGroupId, Integer channelKey, String userId) {
        ApiResponse apiResponse = null;
        try {
            String parameters = "";
            if (!TextUtils.isEmpty(clientGroupId)) {
                parameters = CLIENT_GROUPID + "=" + URLEncoder.encode(clientGroupId, "UTF-8");
            } else {
                parameters = GROUP_ID + "=" + channelKey;
            }
            if (!TextUtils.isEmpty(parameters) &&  !TextUtils.isEmpty(userId) ) {
                String url = getRemoveMemberUrl() + "?" +
                        parameters + "&" + USER_ID + "=" + URLEncoder.encode(userId, "UTF-8");
                String response = httpRequestUtils.getResponse(getCredentials(), url, "application/json", "application/json");
                apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                if(apiResponse != null){
                    Log.i(TAG, "Channel remove member response: " + apiResponse.getStatus());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    public synchronized ApiResponse removeMemberFromChannel(Integer channelKey, String userId) {
        return removeMemberFromChannel(null, channelKey, userId);
    }

    public synchronized ApiResponse removeMemberFromChannel(String clientGroupId, String userId) {
        return removeMemberFromChannel(clientGroupId, null, userId);
    }

    public synchronized ApiResponse updateChannel(GroupInfoUpdate groupInfoUpdate) {
        ApiResponse apiResponse = null;
        try {
            if (groupInfoUpdate != null && (!TextUtils.isEmpty(groupInfoUpdate.getClientGroupId()) || groupInfoUpdate.getGroupId() != null) && (!TextUtils.isEmpty(groupInfoUpdate.getNewName()) || !TextUtils.isEmpty(groupInfoUpdate.getImageUrl()))) {
                String channelNameUpdateJson = GsonUtils.getJsonFromObject(groupInfoUpdate, GroupInfoUpdate.class);
                String response = httpRequestUtils.postData(getCredentials(), getChannelUpdateUrl() , "application/json", "application/json", channelNameUpdateJson);
                apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                if(apiResponse != null){
                    Log.i(TAG, "Update Channel response: " + apiResponse.getStatus());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    public ApiResponse leaveMemberFromChannel(Integer channelKey) {
        return leaveMemberFromChannel(null, channelKey);
    }

    public ApiResponse leaveMemberFromChannel(String clientGroupId) {
        return leaveMemberFromChannel(clientGroupId, null);
    }

    public synchronized ApiResponse leaveMemberFromChannel(String clientGroupId, Integer channelKey) {
        ApiResponse apiResponse = null;
        try {
            String parameters = "";
            if (!TextUtils.isEmpty(clientGroupId)) {
                parameters = CLIENT_GROUPID + "=" + URLEncoder.encode(clientGroupId, "UTF-8");
            } else {
                parameters = GROUP_ID + "=" + channelKey;
            }

            if (!TextUtils.isEmpty(clientGroupId) || (channelKey != null && channelKey != 0)) {
                String url = getChannelLeftUrl() + "?" + parameters;
                String response = httpRequestUtils.getResponse(getCredentials(), url, "application/json", "application/json");
                apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                if(apiResponse != null){
                    Log.i(TAG, "Channel leave member call response: " + apiResponse.getStatus());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    public synchronized ApiResponse deleteChannel(Integer channelKey) {
        try {
            if (channelKey != null) {
                String url = getChannelDeleteUrl() + "?" +
                        GROUP_ID
                        + "=" + URLEncoder.encode(String.valueOf(channelKey), "UTF-8");
                String response = httpRequestUtils.getResponse(getCredentials(), url, "application/json", "application/json");
                ApiResponse apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                if(apiResponse != null){
                    Log.i(TAG, "Channel delete call response: " + apiResponse.getStatus());
                }
                return apiResponse;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
