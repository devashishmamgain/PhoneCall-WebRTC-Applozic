package com.applozic.mobicomkit.api.conversation;

import android.app.IntentService;
import android.content.Intent;

import com.applozic.mobicomkit.api.account.user.UserService;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;

/**
 * Created by sunil on 23/7/16.
 */
public class ConversationReadService extends IntentService {

    private static final String TAG = "ConversationReadService";
    public static final String CONTACT = "contact";
    public static final String CHANNEL = "channel";
    public static final String UNREAD_COUNT = "UNREAD_COUNT";

    public ConversationReadService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Integer unreadCount = intent.getIntExtra(UNREAD_COUNT, 0);

        if (unreadCount != 0) {
            Contact contact = (Contact) intent.getSerializableExtra(CONTACT);
            Channel channel = (Channel) intent.getSerializableExtra(CHANNEL);
            new MessageClientService(getApplicationContext()).updateReadStatus(contact, channel);
        } else {
            UserService.getInstance(getApplicationContext()).processUserReadConversation();
        }
    }

}
