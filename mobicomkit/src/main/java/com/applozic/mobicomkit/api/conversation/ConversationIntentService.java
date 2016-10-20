package com.applozic.mobicomkit.api.conversation;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.UserService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by devashish on 15/12/13.
 */
public class ConversationIntentService extends IntentService {

    private static final String TAG = "ConversationIntent";
    public static final String SYNC = "AL_SYNC";
    private static final int PRE_FETCH_MESSAGES_FOR = 6;
    private MobiComMessageService mobiComMessageService;

    public ConversationIntentService() {
        super("ConversationIntent");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mobiComMessageService = new MobiComMessageService(this, MessageIntentService.class);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean sync = intent.getBooleanExtra(SYNC, false);
        Log.i(TAG, "Syncing messages service started: " + sync);
        if (sync) {
            mobiComMessageService.syncMessages();
        } else {
            Thread thread = new Thread(new ConversationSync());
            thread.start();
        }
    }

    private class ConversationSync implements Runnable {

        public ConversationSync() {
        }

        @Override
        public void run() {
            try {
                UserService.getInstance(ConversationIntentService.this).processSyncUserBlock();
                MobiComConversationService mobiComConversationService = new MobiComConversationService(ConversationIntentService.this);
                List<Message> messages = mobiComConversationService.getLatestMessagesGroupByPeople();
                for (Message message: messages.subList(0, Math.min(PRE_FETCH_MESSAGES_FOR, messages.size()))) {
                    Contact contact = null;
                    Channel channel = null;

                    if (message.getGroupId() != null) {
                        channel = new Channel(message.getGroupId());
                    }else {
                        contact = new Contact(message.getContactIds());
                    }

                    mobiComConversationService.getMessages(1L, null, contact, channel, null);
                }

                Set<String> contactNoSet = new HashSet<String>();

                List<Contact> contacts = new AppContactService(ConversationIntentService.this).getContacts(Contact.ContactType.DEVICE);
                for (Contact contact: contacts) {
                    if (!TextUtils.isEmpty(contact.getFormattedContactNumber())) {
                        contactNoSet.add(contact.getFormattedContactNumber());
                    }
                }

                if(!contactNoSet.isEmpty()){
                    UserService userService = UserService.getInstance(getApplicationContext());
                    userService.processUserDetailsByContactNos(contactNoSet);
                }
                MobiComUserPreference.getInstance(ConversationIntentService.this).setDeviceContactSyncCompleted(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
