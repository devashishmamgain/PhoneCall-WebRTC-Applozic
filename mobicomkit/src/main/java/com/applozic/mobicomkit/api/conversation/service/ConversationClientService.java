package com.applozic.mobicomkit.api.conversation.service;

import android.content.Context;
import android.util.Log;

import com.applozic.mobicomkit.api.HttpRequestUtils;
import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicomkit.api.conversation.database.ConversationDatabaseService;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicomkit.feed.ChannelFeed;
import com.applozic.mobicomkit.feed.ChannelFeedApiResponse;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Conversation;

/**
 * Created by sunil on 18/2/16.
 */
public class ConversationClientService extends MobiComKitClientService {

    private static final String CREATE_CONVERSATION_URL = "/rest/ws/conversation/id";
    private static final String CONVERSATION_URL = "/rest/ws/conversation/topicId";
    private static final String TAG = "ConversationClient";
    private static ConversationClientService conversationClientService;
    private Context context;
    private ConversationDatabaseService conversationDatabaseService;
    private HttpRequestUtils httpRequestUtils;


    private ConversationClientService(Context context) {
        super(context);
        this.context = context;
        this.httpRequestUtils = new HttpRequestUtils(context);

    }

    public synchronized static ConversationClientService getInstance(Context context) {
        if (conversationClientService == null) {
            conversationClientService = new ConversationClientService(context);
        }
        return conversationClientService;
    }

    public String getCreateConversationUrl() {
        return getBaseUrl() + CREATE_CONVERSATION_URL;
    }

    public String getConversationUrl() {
        return getBaseUrl() + CONVERSATION_URL;
    }


    public ChannelFeed createConversation(Conversation conversation) {
        ChannelFeed channelFeed = null;
        try {
            String jsonFromObject = GsonUtils.getJsonFromObject(conversation, conversation.getClass());
            String createChannelResponse = httpRequestUtils.postData(getCredentials(), getCreateConversationUrl(), "application/json", "application/json", jsonFromObject);
            Log.i(TAG, "Create Conversation reponse:" + createChannelResponse);
            ChannelFeedApiResponse channelFeedApiResponse = (ChannelFeedApiResponse) GsonUtils.getObjectFromJson(createChannelResponse, ChannelFeedApiResponse.class);

            if (channelFeedApiResponse != null && channelFeedApiResponse.isSuccess()) {
                channelFeed = channelFeedApiResponse.getResponse();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return channelFeed;
    }

    public Conversation getConversation(Integer conversationId) {
        String response = "";
        try {
            if (conversationId != null) {
                response = httpRequestUtils.getResponse(getCredentials(), getConversationUrl() + "?id=" + String.valueOf(conversationId), "application/json", "application/json");
                ApiResponse apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                Log.i(TAG, "Conversation response  is :" + response);
                if (apiResponse != null && apiResponse.isSuccess()) {
                    return (Conversation) apiResponse.getResponse();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
