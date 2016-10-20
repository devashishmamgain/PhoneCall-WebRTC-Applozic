package com.applozic.mobicomkit.api;

import android.content.Context;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.SyncCallService;
import com.applozic.mobicomkit.api.notification.MobiComPushReceiver;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.feed.GcmMessageResponse;
import com.applozic.mobicomkit.feed.MqttMessageResponse;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Date;

import static com.applozic.mobicomkit.api.MobiComKitConstants.APPLICATION_KEY_META_DATA;

/**
 * Created by sunil on 26/11/15.
 */
public class ApplozicMqttService extends MobiComKitClientService implements MqttCallback {

    private static final String STATUS = "status";
    private static final String TAG = "ApplozicMqttService";
    private static final String TYPINGTOPIC = "typing-";
    private static ApplozicMqttService applozicMqttService;
    private MqttClient client;
    private MemoryPersistence memoryPersistence;
    private Context context;

    public static enum NOTIFICATION_TYPE {
        MESSAGE_RECEIVED("APPLOZIC_01"), MESSAGE_SENT("APPLOZIC_02"),
        MESSAGE_SENT_UPDATE("APPLOZIC_03"), MESSAGE_DELIVERED("APPLOZIC_04"),
        MESSAGE_DELETED("APPLOZIC_05"), CONVERSATION_DELETED("APPLOZIC_06"),
        MESSAGE_READ("APPLOZIC_07"), MESSAGE_DELIVERED_AND_READ("APPLOZIC_08"),
        CONVERSATION_READ("APPLOZIC_09"), CONVERSATION_DELIVERED_AND_READ("APPLOZIC_10"),
        USER_CONNECTED("APPLOZIC_11"), USER_DISCONNECTED("APPLOZIC_12"),
        GROUP_DELETED("APPLOZIC_13"), GROUP_LEFT("APPLOZIC_14"), GROUP_SYNC("APPLOZIC_15"),
        USER_BLOCKED("APPLOZIC_16"), USER_UN_BLOCKED("APPLOZIC_17"),
        ACTIVATED("APPLOZIC_18"),
        DEACTIVATED("APPLOZIC_19"),
        REGISTRATION("APPLOZIC_20"),
        GROUP_CONVERSATION_READ("APPLOZIC_21"),
        GROUP_MESSAGE_DELETED("APPLOZIC_22"),
        GROUP_CONVERSATION_DELETED("APPLOZIC_23"),
        APPLOZIC_TEST("APPLOZIC_24"),
        USER_ONLINE_STATUS("APPLOZIC_25"),
        CONTACT_SYNC("APPLOZIC_26");
        private String value;

        private NOTIFICATION_TYPE(String c) {
            value = c;
        }

        public String getValue() {
            return String.valueOf(value);
        }

    }


    private ApplozicMqttService(Context context) {
        super(context);
        this.context = context;
        memoryPersistence = new MemoryPersistence();
    }


    public static ApplozicMqttService getInstance(Context context) {

        if (applozicMqttService == null) {
            applozicMqttService = new ApplozicMqttService(context);
        }
        return applozicMqttService;
    }

    private MqttClient connect() {
        String userId = MobiComUserPreference.getInstance(context).getUserId();
        try {
            if (TextUtils.isEmpty(userId)) {
                return client;
            }
            if (client == null) {
                client = new MqttClient(getMqttBaseUrl(), userId + "-" + new Date().getTime(), memoryPersistence);
            }

            if (!client.isConnected()) {
                Log.i(TAG, "Connecting to mqtt...");
                MqttConnectOptions options = new MqttConnectOptions();
                options.setConnectionTimeout(60);
                options.setWill(STATUS, (MobiComUserPreference.getInstance(context).getSuUserKeyString() + "," + "0").getBytes(), 0, true);
                client.setCallback(ApplozicMqttService.this);

                client.connect(options);
            }
        } catch (MqttException e) {
            Log.d(TAG, "Connecting already in progress.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return client;
    }

    public synchronized void connectPublish(final String userKeyString, final String status) {

        try {
            final MqttClient client = connect();
            if (client == null || !client.isConnected()) {
                return;
            }
            MqttMessage message = new MqttMessage();
            message.setRetained(false);
            message.setPayload((userKeyString + "," + status).getBytes());
            Log.i(TAG, "UserKeyString, status:" + userKeyString + ", " + status);
            message.setQos(0);
            client.publish(STATUS, message);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void subscribe() {
        if (!Utils.isInternetAvailable(context)) {
            return;
        }
        final String userKeyString = MobiComUserPreference.getInstance(context).getSuUserKeyString();
        if (TextUtils.isEmpty(userKeyString)) {
            return;
        }
        try {
            final MqttClient client = connect();
            if (client == null || !client.isConnected()) {
                return;
            }
            MqttMessage message = new MqttMessage();
            message.setRetained(false);
            message.setPayload((userKeyString + "," + "1").getBytes());
            Log.i(TAG, "UserKeyString, status:" + userKeyString + ", " + "1");
            message.setQos(0);
            client.publish(STATUS, message);
            subscribeToConversation();
           // subscribeToTypingTopic();
            if (client != null) {
                client.setCallback(ApplozicMqttService.this);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void unSubscribe() {
        unSubscribeToConversation();
       // unSubscribeToTypingTopic();
    }

    public synchronized void subscribeToConversation() {
        try {
            String userKeyString = MobiComUserPreference.getInstance(context).getSuUserKeyString();
            if (TextUtils.isEmpty(userKeyString)) {
                return;
            }
            if (client != null && client.isConnected()) {
                Log.i(TAG, "Subscribing to conversation topic.");
                client.subscribe(userKeyString, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void unSubscribeToConversation() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client == null || !client.isConnected()) {
                        return;
                    }
                    String userKeyString = MobiComUserPreference.getInstance(context).getSuUserKeyString();
                    client.unsubscribe(userKeyString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    public void disconnectPublish(String userKey, String status) {
        try {
            connectPublish(userKey, status);
            if (!MobiComUserPreference.getInstance(context).isLoggedIn()) {
                disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        BroadcastService.sendMQTTDisconnected(context, BroadcastService.INTENT_ACTIONS.MQTT_DISCONNECTED.toString());
    }

    @Override
    public void messageArrived(String s,final MqttMessage mqttMessage) throws Exception {
        Log.i(TAG, "Received MQTT message: " + new String(mqttMessage.getPayload()));
        try {
            if (!TextUtils.isEmpty(s) && s.startsWith(TYPINGTOPIC)) {
                String typingResponse[] = mqttMessage.toString().split(",");
                String applicationId = typingResponse[0];
                String userId = typingResponse[1];
                String isTypingStatus = typingResponse[2];
                BroadcastService.sendUpdateTypingBroadcast(context, BroadcastService.INTENT_ACTIONS.UPDATE_TYPING_STATUS.toString(), applicationId, userId, isTypingStatus);
            } else {
                final MqttMessageResponse mqttMessageResponse = (MqttMessageResponse) GsonUtils.getObjectFromJson(mqttMessage.toString(), MqttMessageResponse.class);
                if (mqttMessageResponse != null) {
                    if (MobiComPushReceiver.processPushNotificationId(mqttMessageResponse.getId())) {
                        return;
                    }
                    final SyncCallService syncCallService = SyncCallService.getInstance(context);
                    MobiComPushReceiver.addPushNotificationId(mqttMessageResponse.getId());
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "MQTT message type: " + mqttMessageResponse.getType());
                            if (NOTIFICATION_TYPE.MESSAGE_RECEIVED.getValue().equals(mqttMessageResponse.getType()) || "MESSAGE_RECEIVED".equals(mqttMessageResponse.getType())) {
                                syncCallService.syncMessages(null);
                            }

                            if (NOTIFICATION_TYPE.MESSAGE_DELIVERED.getValue().equals(mqttMessageResponse.getType())
                                    || "MT_MESSAGE_DELIVERED".equals(mqttMessageResponse.getType())) {
                                String splitKeyString[] = (mqttMessageResponse.getMessage()).toString().split(",");
                                String keyString = splitKeyString[0];
                                //String userId = splitKeyString[1];
                                syncCallService.updateDeliveryStatus(keyString);
                            }

                            if ( NOTIFICATION_TYPE.MESSAGE_DELIVERED_AND_READ.getValue().equals(mqttMessageResponse.getType())
                                    || "MT_MESSAGE_DELIVERED_READ".equals(mqttMessageResponse.getType())) {
                                String splitKeyString[] = (mqttMessageResponse.getMessage()).toString().split(",");
                                String keyString = splitKeyString[0];
                                syncCallService.updateReadStatus(keyString);
                            }

                            if (NOTIFICATION_TYPE.CONVERSATION_DELIVERED_AND_READ.getValue().equals(mqttMessageResponse.getType())) {
                                String contactId = mqttMessageResponse.getMessage().toString();
                                syncCallService.updateDeliveryStatusForContact(contactId, true);
                            }

                            if (NOTIFICATION_TYPE.USER_CONNECTED.getValue().equals(mqttMessageResponse.getType())) {
                                syncCallService.updateConnectedStatus(mqttMessageResponse.getMessage().toString(), new Date(), true);
                            }

                            if (NOTIFICATION_TYPE.CONTACT_SYNC.getValue().equals(mqttMessageResponse.getType())) {
                                syncCallService.processContactSync(mqttMessageResponse.getMessage().toString());
                            }

                            if (NOTIFICATION_TYPE.USER_DISCONNECTED.getValue().equals(mqttMessageResponse.getType())) {
                                //disconnect comes with timestamp, ranjeet,1449866097000
                                String[] parts = mqttMessageResponse.getMessage().toString().split(",");
                                String userId = parts[0];
                                Date lastSeenAt = new Date();
                                if (parts.length >= 2 && !parts[1].equals("null")) {
                                    lastSeenAt = new Date(Long.valueOf(parts[1]));
                                }
                                syncCallService.updateConnectedStatus(userId, lastSeenAt, false);
                            }

                            if(NOTIFICATION_TYPE.CONVERSATION_DELETED.getValue().equals(mqttMessageResponse.getType())) {
                                syncCallService.deleteConversationThread(mqttMessageResponse.getMessage().toString());
                                BroadcastService.sendConversationDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_CONVERSATION.toString(), mqttMessageResponse.getMessage().toString(), 0, "success");
                            }

                            if (NOTIFICATION_TYPE.MESSAGE_DELETED.getValue().equals(mqttMessageResponse.getType())) {
                                String messageKey = mqttMessageResponse.getMessage().toString().split(",")[0];
                                syncCallService.deleteMessage(messageKey);
                                BroadcastService.sendMessageDeleteBroadcast(context, BroadcastService.INTENT_ACTIONS.DELETE_MESSAGE.toString(), messageKey, null);
                            }

                            if (NOTIFICATION_TYPE.MESSAGE_SENT.getValue().equals(mqttMessageResponse.getType())) {
                                GcmMessageResponse messageResponse = (GcmMessageResponse) GsonUtils.getObjectFromJson(mqttMessage.toString(), GcmMessageResponse.class);
                                Message sentMessageSync = messageResponse.getMessage();
                                syncCallService.syncMessages(sentMessageSync.getKeyString());
                            }

                            if (NOTIFICATION_TYPE.USER_BLOCKED.getValue().equals(mqttMessageResponse.getType()) ||
                                    NOTIFICATION_TYPE.USER_UN_BLOCKED.getValue().equals(mqttMessageResponse.getType())) {
                                syncCallService.syncBlockUsers();
                            }

                            /*if(NOTIFICATION_TYPE.USER_BLOCKED.getValue().equals(mqttMessageResponse.getType())){
                                String[] splitKeyString = mqttMessageResponse.getMessage().toString().split(":");
                                String type = splitKeyString[0];
                                String userId;
                                if (splitKeyString.length >= 2) {
                                    userId = splitKeyString[1];
                                    if(MobiComPushReceiver.BLOCKED_TO.equals(type)){
                                        syncCallService.updateUserBlocked(userId,true);
                                    }else {
                                        syncCallService.updateUserBlockedBy(userId, true);
                                    }
                                }
                            }

                            if(NOTIFICATION_TYPE.USER_UN_BLOCKED.getValue().equals(mqttMessageResponse.getType())){
                                String[] splitKeyString = mqttMessageResponse.getMessage().toString().split(":");
                                String type = splitKeyString[0];
                                String userId;
                                if (splitKeyString.length >= 2) {
                                    userId = splitKeyString[1];
                                    if(MobiComPushReceiver.UNBLOCKED_TO.equals(type)){
                                        syncCallService.updateUserBlocked(userId,false);
                                    }else {
                                        syncCallService.updateUserBlockedBy(userId,false);
                                    }
                                }
                            } */

                        }
                    });
                    thread.start();

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void publishTopic(final String applicationId, final String status, final String loggedInUserId, final String userId) {
        try {
            final MqttClient client = connect();
            if (client == null || !client.isConnected()) {
                return;
            }
            MqttMessage message = new MqttMessage();
            message.setRetained(false);
            message.setPayload((applicationId + "," + loggedInUserId + "," + status).getBytes());
            message.setQos(0);
            client.publish("typing" + "-" + applicationId + "-" + userId, message);
            Log.i(TAG, "Published " + new String(message.getPayload()) + " to topic: " + "typing" + "-" + applicationId + "-" + userId);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public synchronized void subscribeToTypingTopic(Channel channel) {
        try {
            String currentId = null;
            if(channel != null) {
                currentId = String.valueOf(channel.getKey());
            }else {
                MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(context);
                currentId = mobiComUserPreference.getUserId();
            }

            final MqttClient client = connect();
            if (client == null || !client.isConnected()) {
                return;
            }

            client.subscribe("typing-" + Utils.getMetaDataValue(context, APPLICATION_KEY_META_DATA) + "-" + currentId, 0);
            Log.i(TAG, "Subscribed to topic: " + "typing-" + Utils.getMetaDataValue(context, APPLICATION_KEY_META_DATA) + "-" + currentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public synchronized void unSubscribeToTypingTopic(Channel channel) {
        try {
            String currentId = null;
            if(channel != null) {
                currentId = String.valueOf(channel.getKey());
            }else {
                MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(context);
                currentId = mobiComUserPreference.getUserId();
            }

            final MqttClient client = connect();
            if (client == null || !client.isConnected()) {
                return;
            }

            client.unsubscribe("typing-" + Utils.getMetaDataValue(context, APPLICATION_KEY_META_DATA) + "-" + currentId);
            Log.i(TAG, "UnSubscribed to topic: " + "typing-" + Utils.getMetaDataValue(context, APPLICATION_KEY_META_DATA) + "-" + currentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public void typingStarted(Contact contact, Channel channel) {
        String currentId;
        if(channel != null){
            currentId = String.valueOf(channel.getKey());
        }else {
            currentId = contact.getUserId();
        }
        MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(context);
        publishTopic(getApplicationId(contact), "1", mobiComUserPreference.getUserId(), currentId);
    }

    public void typingStopped(Contact contact,Channel channel) {
        String currentId;
        if(channel != null){
            currentId = String.valueOf(channel.getKey());
        }else {
            currentId = contact.getUserId();
        }
        MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(context);
        publishTopic(getApplicationId(contact), "0", mobiComUserPreference.getUserId(), currentId);
    }

    public String getApplicationId(Contact contact) {
        String applicationId = contact != null ? contact.getApplicationId() : null;
        if (TextUtils.isEmpty(applicationId)) {
            applicationId = Utils.getMetaDataValue(context, APPLICATION_KEY_META_DATA);
        }
        return applicationId;
    }

}


