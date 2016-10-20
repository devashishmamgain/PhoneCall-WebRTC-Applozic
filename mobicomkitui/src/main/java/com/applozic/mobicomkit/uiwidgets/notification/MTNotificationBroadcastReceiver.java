package com.applozic.mobicomkit.uiwidgets.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.service.ConversationService;
import com.applozic.mobicomkit.api.notification.NotificationService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;

/**
 * Created by adarsh on 3/5/15.
 */
public class MTNotificationBroadcastReceiver extends BroadcastReceiver {


    private static final String TAG = "MTBroadcastReceiver";

    private static String NOTIFICATION_ICON_METADATA = "com.applozic.mobicomkit.notification.icon";

    @Override
    public void onReceive(final Context context, Intent intent) {

        Integer notificationId = Utils.getMetaDataValueForResources(context, NOTIFICATION_ICON_METADATA);

        String action = intent.getAction();
        String messageJson = intent.getStringExtra(MobiComKitConstants.MESSAGE_JSON_INTENT);
        Log.i(TAG, "Received broadcast, action: " + action + ", message: " + messageJson);
        if (!TextUtils.isEmpty(messageJson)) {
            final Message message = (Message) GsonUtils.getObjectFromJson(messageJson, Message.class);
            final NotificationService notificationService =
                    new NotificationService(notificationId == null ? R.drawable.mobicom_ic_launcher : notificationId, context, R.string.wearable_action_label, R.string.wearable_action_title, R.drawable.mobicom_ic_action_send);


            if (MobiComUserPreference.getInstance(context).isLoggedIn()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Channel channel = ChannelService.getInstance(context).getChannelInfo(message.getGroupId());
                        Contact contact = null;
                        if(message.getConversationId() != null){
                            ConversationService.getInstance(context).getConversation(message.getConversationId());
                        }
                        if (message.getGroupId() == null) {
                            contact = new AppContactService(context).getContactById(message.getContactIds());
                        }
                        notificationService.notifyUser(contact, channel, message);
                    }
                }).start();
            }

        }
    }
}
