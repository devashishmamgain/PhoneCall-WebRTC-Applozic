package com.applozic.mobicomkit.broadcast;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.conversation.Message;

import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;

/**
 * Created by devashish on 24/1/15.
 */
public class BroadcastService {

    private static final String TAG = "BroadcastService";
    private static final String PACKAGE_NAME = "com.package.name";
    private static final String MOBICOMKIT_ALL = "MOBICOMKIT_ALL";

    public static String currentUserId = null;
    public static Integer currentConversationId = null;
    public static boolean mobiTexterBroadcastReceiverActivated;
    private static boolean contextBasedChatEnabled = false;
    public static boolean videoCallAcitivityOpend = false;

    public static String currentInfoId = null;

    public static void selectMobiComKitAll() {
        currentUserId = MOBICOMKIT_ALL;
    }

    public static boolean isQuick() {
        return currentUserId != null && currentUserId.equals(MOBICOMKIT_ALL);
    }
    public static boolean isChannelInfo() {
        return currentInfoId != null;
    }

    public static boolean isIndividual() {
        return currentUserId != null && !isQuick();
    }

    public static synchronized boolean isContextBasedChatEnabled() {
        return contextBasedChatEnabled;
    }

    public static synchronized boolean setContextBasedChat(boolean contextBasedChat) {
        return contextBasedChatEnabled = contextBasedChat;
    }

    public static void sendFirstTimeSyncCompletedBroadcast(Context context) {
        Log.i(TAG, "Sending " + INTENT_ACTIONS.FIRST_TIME_SYNC_COMPLETE.toString() + " broadcast");
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTIONS.FIRST_TIME_SYNC_COMPLETE.toString());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intent);
    }

    public static void sendLoadMoreBroadcast(Context context, boolean loadMore) {
        Log.i(TAG, "Sending " + INTENT_ACTIONS.LOAD_MORE.toString() + " broadcast");
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTIONS.LOAD_MORE.toString());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("loadMore", loadMore);
        sendBroadcast(context, intent);
    }

    public static void sendDeliveryReportForContactBroadcast(Context context, String action, String contactId) {
        Log.i(TAG, "Sending message delivery report of contact broadcast for " + action + ", " + contactId);
        Intent intentUpdate = new Intent();
        intentUpdate.setAction(action);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(MobiComKitConstants.CONTACT_ID, contactId);
        sendBroadcast(context, intentUpdate);
    }

    public static void sendMessageUpdateBroadcast(Context context, String action, Message message) {
        Log.i(TAG, "Sending message update broadcast for " + action + ", " + message.getKeyString());
        Intent intentUpdate = new Intent();
        intentUpdate.setAction(action);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(message, Message.class));
        sendBroadcast(context, intentUpdate);
    }

    public static void sendMessageDeleteBroadcast(Context context, String action, String keyString, String contactNumbers) {
        Log.i(TAG, "Sending message delete broadcast for " + action);
        Intent intentDelete = new Intent();
        intentDelete.setAction(action);
        intentDelete.putExtra("keyString", keyString);
        intentDelete.putExtra("contactNumbers", contactNumbers);
        intentDelete.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intentDelete);
    }

    public static void sendConversationDeleteBroadcast(Context context, String action, String contactNumber, Integer channelKey,String response) {
        Log.i(TAG, "Sending conversation delete broadcast for " + action);
        Intent intentDelete = new Intent();
        intentDelete.setAction(action);
        intentDelete.putExtra("channelKey", channelKey);
        intentDelete.putExtra("contactNumber", contactNumber);
        intentDelete.putExtra("response", response);
        intentDelete.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intentDelete);
    }

    public static void sendNumberVerifiedBroadcast(Context context, String action) {
        Log.i(TAG, "Sending number verified broadcast");
        Intent intentUpdate = new Intent();
        intentUpdate.setAction(action);
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intentUpdate);
    }

    public static void sendNotificationBroadcast(Context context, Message message) {
        Log.i(TAG, "Sending notification broadcast....");
        Intent notificationIntent = new Intent();
        notificationIntent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(message, Message.class));
        notificationIntent.setAction(Utils.getMetaDataValue(context, PACKAGE_NAME) + ".send.notification");
        context.sendBroadcast(notificationIntent);
    }

    public static void sendUpdateLastSeenAtTimeBroadcast(Context context, String action, String contactId){
        Log.i(TAG, "Sending lastSeenAt broadcast....");
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra("contactId", contactId);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intent);
    }

    public static void sendUpdateTypingBroadcast(Context context, String action, String applicationId, String userId, String isTyping){
        Log.i(TAG, "Sending typing Broadcast.......");
        Intent intentTyping = new Intent();
        intentTyping.setAction(action);
        intentTyping.putExtra("applicationId", applicationId);
        intentTyping.putExtra("userId", userId);
        intentTyping.putExtra("isTyping",isTyping);
        intentTyping.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intentTyping);
    }

    public static void sendMQTTDisconnected(Context context, String action){
        Log.i(TAG, "Sending typing Broadcast.......");
        Intent intentTyping = new Intent();
        intentTyping.setAction(action);
        intentTyping.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intentTyping);
    }

    public static void sendUpdateForName(Context context,Integer channelKey,String action){
        Log.i(TAG, "Sending  Broadcast for dataChange.......");
        Intent intent = new Intent();
        intent.putExtra("channelKey", channelKey);
        intent.setAction(action);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intent);
    }

    public static void sendUpdateForChannelSync(Context context,String action){
        Log.i(TAG, "Sending  Broadcast for channelSync ......");
        Intent intent = new Intent();
        intent.setAction(action);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(context, intent);
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_ACTIONS.FIRST_TIME_SYNC_COMPLETE.toString());
        intentFilter.addAction(INTENT_ACTIONS.LOAD_MORE.toString());
        intentFilter.addAction(INTENT_ACTIONS.MESSAGE_SYNC_ACK_FROM_SERVER.toString());
        intentFilter.addAction(INTENT_ACTIONS.SYNC_MESSAGE.toString());
        intentFilter.addAction(INTENT_ACTIONS.DELETE_MESSAGE.toString());
        intentFilter.addAction(INTENT_ACTIONS.DELETE_CONVERSATION.toString());
        intentFilter.addAction(INTENT_ACTIONS.MESSAGE_DELIVERY.toString());
        intentFilter.addAction(INTENT_ACTIONS.MESSAGE_DELIVERY_FOR_CONTACT.toString());
        intentFilter.addAction(INTENT_ACTIONS.UPLOAD_ATTACHMENT_FAILED.toString());
        intentFilter.addAction(INTENT_ACTIONS.MESSAGE_ATTACHMENT_DOWNLOAD_DONE.toString());
        intentFilter.addAction(INTENT_ACTIONS.INSTRUCTION.toString());
        intentFilter.addAction(INTENT_ACTIONS.MESSAGE_ATTACHMENT_DOWNLOAD_FAILD.toString());
        intentFilter.addAction(INTENT_ACTIONS.UPDATE_LAST_SEEN_AT_TIME.toString());
        intentFilter.addAction(INTENT_ACTIONS.UPDATE_TYPING_STATUS.toString());
        intentFilter.addAction(INTENT_ACTIONS.MQTT_DISCONNECTED.toString());
        intentFilter.addAction(INTENT_ACTIONS.UPDATE_NAME.toString());
        intentFilter.addAction(INTENT_ACTIONS.MESSAGE_READ_AND_DELIVERED.toString());
        intentFilter.addAction(INTENT_ACTIONS.MESSAGE_READ_AND_DELIVERED_FOR_CONTECT.toString());
        intentFilter.addAction(INTENT_ACTIONS.CHANNEL_SYNC.toString());
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        return intentFilter;
    }

    public enum INTENT_ACTIONS {
        LOAD_MORE, FIRST_TIME_SYNC_COMPLETE, MESSAGE_SYNC_ACK_FROM_SERVER,
        SYNC_MESSAGE, DELETE_MESSAGE, DELETE_CONVERSATION, MESSAGE_DELIVERY, MESSAGE_DELIVERY_FOR_CONTACT, INSTRUCTION,
        UPLOAD_ATTACHMENT_FAILED, MESSAGE_ATTACHMENT_DOWNLOAD_DONE, MESSAGE_ATTACHMENT_DOWNLOAD_FAILD,
        UPDATE_LAST_SEEN_AT_TIME,UPDATE_TYPING_STATUS, MESSAGE_READ_AND_DELIVERED, MESSAGE_READ_AND_DELIVERED_FOR_CONTECT,CHANNEL_SYNC,
        CONTACT_VERIFIED, NOTIFY_USER, MQTT_DISCONNECTED,UPDATE_NAME
    }

    public static void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
