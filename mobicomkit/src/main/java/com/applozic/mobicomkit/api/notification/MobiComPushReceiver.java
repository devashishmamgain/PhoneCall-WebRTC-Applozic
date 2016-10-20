package com.applozic.mobicomkit.api.notification;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.MobiComConversationService;
import com.applozic.mobicomkit.api.conversation.SyncCallService;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.feed.GcmMessageResponse;
import com.applozic.mobicomkit.feed.MqttMessageResponse;
import com.applozic.mobicommons.json.GsonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MobiComPushReceiver {

    public static final String MTCOM_PREFIX = "APPLOZIC_";
    public static final List<String> notificationKeyList = new ArrayList<String>();
    private static final String TAG = "MobiComPushReceiver";
    private static Queue<String> notificationIdList = new LinkedList<String>();
    public static final String BLOCKED_TO = "BLOCKED_TO";
    public static final String UNBLOCKED_TO ="UNBLOCKED_TO";

    static {

        notificationKeyList.add("APPLOZIC_01"); // 0 for MESSAGE_RECEIVED //done
        notificationKeyList.add("APPLOZIC_02");// 1 for MESSAGE_SENT
        notificationKeyList.add("APPLOZIC_03");// 2 for MESSAGE_SENT_UPDATE
        notificationKeyList.add("APPLOZIC_04"); //3 for MESSAGE_DELIVERED//done
        notificationKeyList.add("APPLOZIC_05"); //4 for MESSAGE_DELETED
        notificationKeyList.add("APPLOZIC_06");// 5 for CONVERSATION_DELETED//done
        notificationKeyList.add("APPLOZIC_07"); // 6 for MESSAGE_READ
        notificationKeyList.add("APPLOZIC_08"); // 7 for MESSAGE_DELIVERED_AND_READ//done
        notificationKeyList.add("APPLOZIC_09"); // 8 for CONVERSATION_READ
        notificationKeyList.add("APPLOZIC_10"); // 9 for CONVERSATION_DELIVERED_AND_READ
        notificationKeyList.add("APPLOZIC_11");// 10 for USER_CONNECTED//done
        notificationKeyList.add("APPLOZIC_12");// 11 for USER_DISCONNECTED//done
        notificationKeyList.add("APPLOZIC_13");// 12 for GROUP_DELETED
        notificationKeyList.add("APPLOZIC_14");// 13 for GROUP_LEFT
        notificationKeyList.add("APPLOZIC_15");// 14 for group_sync
        notificationKeyList.add("APPLOZIC_16");//15 for blocked
        notificationKeyList.add("APPLOZIC_17");//16 for unblocked
        notificationKeyList.add("APPLOZIC_18");//ACTIVATED
        notificationKeyList.add("APPLOZIC_19");//DEACTIVATED
        notificationKeyList.add("APPLOZIC_20");//REGISTRATION
        notificationKeyList.add("APPLOZIC_21");//GROUP_CONVERSATION_READ
        notificationKeyList.add("APPLOZIC_22");//GROUP_MESSAGE_DELETED
        notificationKeyList.add("APPLOZIC_23");//GROUP_CONVERSATION_DELETED
        notificationKeyList.add("APPLOZIC_24");//APPLOZIC_TEST
        notificationKeyList.add("APPLOZIC_25");//USER_ONLINE_STATUS
        notificationKeyList.add("APPLOZIC_26");//CONTACT_SYNC
    }

    public static boolean isMobiComPushNotification(Intent intent) {
        Log.i(TAG, "checking for Applozic notification.");
        return isMobiComPushNotification(intent.getExtras());
    }

    public static boolean isMobiComPushNotification(Bundle bundle) {
        //This is to identify collapse key sent in notification..
        String payLoad = bundle.getString("collapse_key");
        Log.i(TAG, "Received notification: " + payLoad);

        if (payLoad != null && payLoad.contains(MTCOM_PREFIX) || notificationKeyList.contains(payLoad)) {
            return true;
        } else {
            for (String key : notificationKeyList) {
                payLoad = bundle.getString(key);
                if (payLoad != null) {
                    return true;
                }
            }
            return false;
        }
    }

    public synchronized static boolean processPushNotificationId(String id) {
        if (id != null && notificationIdList != null && notificationIdList.contains(id)) {
            if(notificationIdList.size()>0){
                notificationIdList.remove(id);
            }
            return true;
        }
        return false;
    }

    public synchronized static void addPushNotificationId(String notificationId) {
        try {
            if (notificationIdList != null && notificationIdList.size() < 20) {
                notificationIdList.add(notificationId);
            }
            if (notificationIdList != null && notificationIdList.size() == 20) {
                for (int i = 1; i <= 14; i++) {
                    if(notificationIdList.size()>0){
                        notificationIdList.remove();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void processMessage(Context context, Bundle bundle) {
        // Bundle extras = intent.getExtras();
        if (bundle != null) {
            // ToDo: do something for invalidkey ;
            // && extras.get("InvalidKey") != null
            String message = bundle.getString("collapse_key");

            /*
            "key" : "APPLOZIC_01",
            "value" : "{sadjflkjalsdfj}
            MqttResponse
            * */

            String deleteConversationForContact = bundle.getString(notificationKeyList.get(5));
            String deleteMessage = bundle.getString(notificationKeyList.get(4));
            //  String multipleMessageDelete = bundle.getString(notificationKeyList.get(5));
            // String mtexterUser = bundle.getString(notificationKeyList.get(7));
            String payloadForDelivered = bundle.getString(notificationKeyList.get(3));
            String userConnected = bundle.getString(notificationKeyList.get(10));
            String userDisconnected = bundle.getString(notificationKeyList.get(11));
            processMessage(context, bundle, message, deleteConversationForContact, deleteMessage, payloadForDelivered, userConnected, userDisconnected);
        }
    }

    public static void processMessage(final Context context, Bundle bundle, String message, String deleteConversationForContact, String deleteMessage, String payloadForDelivered, String userConnected, String userDisconnected) {
        SyncCallService syncCallService = SyncCallService.getInstance(context);
        try {

            String playloadDeliveredAndRead =  bundle.getString(notificationKeyList.get(7));
            if (!TextUtils.isEmpty(payloadForDelivered)) {
                MqttMessageResponse messageResponseForDelivered = (MqttMessageResponse) GsonUtils.getObjectFromJson(payloadForDelivered, MqttMessageResponse.class);
                if (processPushNotificationId(messageResponseForDelivered.getId())) {
                    return;
                }
                addPushNotificationId(messageResponseForDelivered.getId());
                String splitKeyString[] = (messageResponseForDelivered.getMessage()).toString().split(",");
                String keyString = splitKeyString[0];
               // String userId = splitKeyString[1];
                syncCallService.updateDeliveryStatus(keyString);
            }

            if (!TextUtils.isEmpty(playloadDeliveredAndRead)) {
                MqttMessageResponse messageResponseForDelivered = (MqttMessageResponse) GsonUtils.getObjectFromJson(playloadDeliveredAndRead, MqttMessageResponse.class);
                if (processPushNotificationId(messageResponseForDelivered.getId())) {
                    return;
                }
                addPushNotificationId(messageResponseForDelivered.getId());
                String splitKeyString[] = (messageResponseForDelivered.getMessage()).toString().split(",");
                String keyString = splitKeyString[0];
                // String userId = splitKeyString[1];
                syncCallService.updateReadStatus(keyString);
            }


            if (!TextUtils.isEmpty(deleteConversationForContact)) {
                MqttMessageResponse deleteConversationResponse = (MqttMessageResponse) GsonUtils.getObjectFromJson(deleteConversationForContact, MqttMessageResponse.class);
                if (processPushNotificationId(deleteConversationResponse.getId())) {
                    return;
                }
                addPushNotificationId(deleteConversationResponse.getId());
                MobiComConversationService conversationService = new MobiComConversationService(context);
                conversationService.deleteConversationFromDevice(deleteConversationResponse.getMessage().toString());
                BroadcastService.sendConversationDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_CONVERSATION.toString(), deleteConversationResponse.getMessage().toString(), 0, "success");
            }

        /*if (!TextUtils.isEmpty(mtexterUser)) {
            Log.i(TAG, "Received GCM message MTEXTER_USER: " + mtexterUser);
            if (mtexterUser.contains("{")) {
                Gson gson = new Gson();
                ContactContent contactContent = gson.fromJson(mtexterUser, ContactContent.class);
                ContactService.addUsersToContact(context, contactContent.getContactNumber(), contactContent.getAppVersion(), true);
            } else {
                String[] details = mtexterUser.split(",");
                ContactService.addUsersToContact(context, details[0], Short.parseShort(details[1]), true);
            }
        }*/

            if (!TextUtils.isEmpty(userConnected)) {
                MqttMessageResponse userConnectedResponse = (MqttMessageResponse) GsonUtils.getObjectFromJson(userConnected, MqttMessageResponse.class);
                if (processPushNotificationId(userConnectedResponse.getId())) {
                    return;
                }
                addPushNotificationId(userConnectedResponse.getId());
                syncCallService.updateConnectedStatus(userConnectedResponse.getMessage().toString(), new Date(), true);
            }

            if (!TextUtils.isEmpty(userDisconnected)) {
                MqttMessageResponse userDisconnectedResponse = (MqttMessageResponse) GsonUtils.getObjectFromJson(userDisconnected, MqttMessageResponse.class);
                if (processPushNotificationId(userDisconnectedResponse.getId())) {
                    return;
                }
                addPushNotificationId(userDisconnectedResponse.getId());
                String[] parts = userDisconnectedResponse.getMessage().toString().split(",");
                String userId = parts[0];
                Date lastSeenAt = new Date();
                if (parts.length >= 2 && !parts[1].equals("null")) {
                    lastSeenAt = new Date(Long.valueOf(parts[1]));
                }
                syncCallService.updateConnectedStatus(userId, lastSeenAt, false);
            }

      /*  if (!TextUtils.isEmpty(multipleMessageDelete)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            MessageDeleteContent messageDeleteContent = gson.fromJson(multipleMessageDelete, MessageDeleteContent.class);

            for (String deletedSmsKeyString : messageDeleteContent.getDeleteKeyStrings()) {
                processDeleteSingleMessageRequest(context, deletedSmsKeyString, messageDeleteContent.getContactNumber());
            }
        }*/

            if (!TextUtils.isEmpty(deleteMessage)) {
                MqttMessageResponse deleteSingleMessageResponse = (MqttMessageResponse) GsonUtils.getObjectFromJson(deleteMessage, MqttMessageResponse.class);
                if (processPushNotificationId(deleteSingleMessageResponse.getId())) {
                    return;
                }
                addPushNotificationId(deleteSingleMessageResponse.getId());
                String deleteMessageKeyAndUserId = deleteSingleMessageResponse.getMessage().toString();
                //String contactNumbers = deleteMessageKeyAndUserId.split(",").length > 1 ? deleteMessageKeyAndUserId.split(",")[1] : null;
                syncCallService.deleteMessage(deleteMessageKeyAndUserId.split(",")[0]);
            }

            String messageSent = bundle.getString(notificationKeyList.get(1));
            if (!TextUtils.isEmpty(messageSent)) {
                GcmMessageResponse syncSentMessageResponse = (GcmMessageResponse) GsonUtils.getObjectFromJson(messageSent, GcmMessageResponse.class);
                if (processPushNotificationId(syncSentMessageResponse.getId())) {
                    return;
                }
                addPushNotificationId(syncSentMessageResponse.getId());
                syncCallService.syncMessages(null);
            }

            String messageKey = bundle.getString(notificationKeyList.get(0));
            GcmMessageResponse syncMessageResponse = null;
            if (!TextUtils.isEmpty(messageKey)) {
                syncMessageResponse = (GcmMessageResponse) GsonUtils.getObjectFromJson(messageKey, GcmMessageResponse.class);
                if (processPushNotificationId(syncMessageResponse.getId())) {
                    return;
                }
                addPushNotificationId(syncMessageResponse.getId());
                Message messageObj = syncMessageResponse.getMessage();
                if (!TextUtils.isEmpty(messageObj.getKeyString())) {
                    syncCallService.syncMessages(messageObj.getKeyString());
                } else {
                    syncCallService.syncMessages(null);
                }
            }

            String conversationReadResponse = bundle.getString(notificationKeyList.get(9));
            if (!TextUtils.isEmpty(conversationReadResponse)) {
                MqttMessageResponse updateDeliveryStatusForContactResponse = (MqttMessageResponse) GsonUtils.getObjectFromJson(conversationReadResponse, MqttMessageResponse.class);
                if (notificationKeyList.get(9).equals(updateDeliveryStatusForContactResponse.getType())) {
                    if (processPushNotificationId(updateDeliveryStatusForContactResponse.getId())) {
                        return;
                    }
                    addPushNotificationId(updateDeliveryStatusForContactResponse.getId());
                    syncCallService.updateDeliveryStatusForContact(updateDeliveryStatusForContactResponse.getMessage().toString(),true);
                }
            }

            String userBlockedResponse = bundle.getString(notificationKeyList.get(15));
            if(!TextUtils.isEmpty(userBlockedResponse)) {
                MqttMessageResponse syncUserBlock = (MqttMessageResponse) GsonUtils.getObjectFromJson(userBlockedResponse, MqttMessageResponse.class);
                if (processPushNotificationId(syncUserBlock.getId())) {
                    return;
                }
                addPushNotificationId(syncUserBlock.getId());
                SyncCallService.getInstance(context).syncBlockUsers();
               /* String[] splitKeyString = syncUserBlock.getMessage().toString().split(":");
                 String type = splitKeyString[0];
                 String userId;
                if (splitKeyString.length >= 2) {
                    userId = splitKeyString[1];
                    if(BLOCKED_TO.equals(type)){
                        syncCallService.updateUserBlocked(userId,true);
                    }else {
                        syncCallService.updateUserBlockedBy(userId, true);
                    }
                } */
            }


            String userUnBlockedResponse = bundle.getString(notificationKeyList.get(16));
            if(!TextUtils.isEmpty(userUnBlockedResponse)) {
                MqttMessageResponse syncUserUnBlock = (MqttMessageResponse) GsonUtils.getObjectFromJson(userUnBlockedResponse, MqttMessageResponse.class);
                if (processPushNotificationId(syncUserUnBlock.getId())) {
                    return;
                }
                addPushNotificationId(syncUserUnBlock.getId());
                SyncCallService.getInstance(context).syncBlockUsers();
                /*String[] splitKeyString = syncUserUnBlock.getMessage().toString().split(":");
                String type = splitKeyString[0];
                String userId;

                if (splitKeyString.length >= 2) {
                    userId = splitKeyString[1];
                    if(UNBLOCKED_TO.equals(type)){
                        syncCallService.updateUserBlocked(userId,false);
                    }else {
                        syncCallService.updateUserBlockedBy(userId,false);
                    }
                } */
            }

            String contactSync = bundle.getString(notificationKeyList.get(25));
            if (!TextUtils.isEmpty(contactSync)) {
                MqttMessageResponse mqttMessageResponse = (MqttMessageResponse) GsonUtils.getObjectFromJson(contactSync, MqttMessageResponse.class);
                if (processPushNotificationId(mqttMessageResponse.getId())) {
                    return;
                }
                addPushNotificationId(mqttMessageResponse.getId());
                syncCallService.processContactSync(mqttMessageResponse.getMessage().toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void processMessageAsync(final Context context, final Bundle bundle) {
        if (MobiComUserPreference.getInstance(context).isLoggedIn()) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    processMessage(context, bundle);
                }
            }).start();
        }
    }

    public static void processMessageAsync(final Context context, final Intent intent) {
        processMessageAsync(context, intent.getExtras());
    }

}
