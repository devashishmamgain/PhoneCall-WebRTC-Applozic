package com.applozic.mobicomkit.uiwidgets.conversation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.ConversationUIServiceNew;
import com.applozic.mobicomkit.InstructionUtil;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.contact.Contact;

/**
 * Created by devashish on 4/2/15.
 */
public class MobiComKitBroadcastReceiverNew extends BroadcastReceiver {

    private static final String TAG = "MTBroadcastReceiver";

    private ConversationUIServiceNew conversationUIService;
    private BaseContactService baseContactService;

    public MobiComKitBroadcastReceiverNew(Activity activity,ConversationUIServiceNew conversationUIService ) {
        this.conversationUIService =conversationUIService;
        this.baseContactService = new AppContactService(activity);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Message message = null;
        String messageJson = intent.getStringExtra(MobiComKitConstants.MESSAGE_JSON_INTENT);
        if (!TextUtils.isEmpty(messageJson)) {
            message = (Message) GsonUtils.getObjectFromJson(messageJson, Message.class);
        }
        Log.i(TAG, "Received broadcast, action: " + action + ", message: " + message);

        if (message != null && !message.isSentToMany()) {
            conversationUIService.addMessage(message);
        } else if (message != null && message.isSentToMany() && BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString().equals(intent.getAction())) {
            for (String toField : message.getTo().split(",")) {
                Message singleMessage = new Message(message);
                singleMessage.setKeyString(message.getKeyString());
                singleMessage.setTo(toField);
                singleMessage.processContactIds(context);
                conversationUIService.addMessage(message);
            }
        }

        String keyString = intent.getStringExtra("keyString");
        String userId = message != null ? message.getContactIds() : "";

        if (BroadcastService.INTENT_ACTIONS.INSTRUCTION.toString().equals(action)) {
            InstructionUtil.showInstruction(context, intent.getIntExtra("resId", -1), intent.getBooleanExtra("actionable", false), R.color.instruction_color);
            String instruction = intent.getStringExtra("instruction");
            if (!TextUtils.isEmpty(instruction)) {
                InstructionUtil.showToast(context, instruction);
            }
        } else if (BroadcastService.INTENT_ACTIONS.UPDATE_NAME.toString().equals(action)) {
            Integer channelKey = intent.getIntExtra("channelKey",0);
            if(channelKey == 0){
                channelKey = null;
            }
           // conversationUIService.updateName(channelKey);
        }
        else if (BroadcastService.INTENT_ACTIONS.FIRST_TIME_SYNC_COMPLETE.toString().equals(action)) {
            conversationUIService.downloadConversations(true);
        } else if (BroadcastService.INTENT_ACTIONS.LOAD_MORE.toString().equals(action)) {
            conversationUIService.setLoadMore(intent.getBooleanExtra("loadMore", true));
        } else if (BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString().equals(intent.getAction())) {
            //conversationUIService.syncMessages(message, keyString);
        } else if (BroadcastService.INTENT_ACTIONS.DELETE_MESSAGE.toString().equals(intent.getAction())) {
            userId = intent.getStringExtra("contactNumbers");
            conversationUIService.deleteMessage(keyString, userId);
        } else if (BroadcastService.INTENT_ACTIONS.DELETE_CONVERSATION.toString().equals(action)) {
            String contactNumber = intent.getStringExtra("contactNumber");
            Integer channelKey = intent.getIntExtra("channelKey", 0);
            String response = intent.getStringExtra("response");
            Contact contact = null;
            if(contactNumber != null){
                contact = baseContactService.getContactById(contactNumber);
            }
            conversationUIService.deleteConversation(contact, channelKey, response);
        } else if (BroadcastService.INTENT_ACTIONS.UPDATE_LAST_SEEN_AT_TIME.toString().equals(action)) {
            conversationUIService.updateLastSeenStatus(intent.getStringExtra("contactId"));
        } else if (BroadcastService.INTENT_ACTIONS.MQTT_DISCONNECTED.toString().equals(action)) {
            conversationUIService.reconnectMQTT();
        } else if(BroadcastService.INTENT_ACTIONS.CHANNEL_SYNC.toString().equals(action)){
            conversationUIService.updateChannelSync();
        }
    }
}
