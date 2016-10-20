package com.applozic.mobicomkit.uiwidgets.conversation.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;

import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.UserService;
import com.applozic.mobicomkit.api.conversation.ApplozicMqttIntentService;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.MobiComConversationService;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.feed.RegisteredUsersApiResponse;
import com.applozic.mobicomkit.feed.TopicDetail;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicomkit.uiwidgets.async.ApplozicChannelDeleteTask;
import com.applozic.mobicomkit.uiwidgets.async.ApplozicChannelLeaveMember;
import com.applozic.mobicomkit.uiwidgets.conversation.DeleteConversationAsyncTask;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ChannelInfoActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ConversationActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.MobiComKitActivityInterface;

import com.applozic.mobicomkit.uiwidgets.people.activity.MobiComKitPeopleActivity;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.channel.ChannelUtils;
import com.applozic.mobicommons.people.channel.Conversation;
import com.applozic.mobicommons.people.contact.Contact;


public class ConversationUIServiceNew {

    public static final int APPLOZIC_CONTACT_OR_GROUP_SELECTED = 3001;

    public static final String CONVERSATION_FRAGMENT = "ConversationFragment";
    public static final String MESSGAE_INFO_FRAGMENT = "messageInfoFagment";
    public static final String QUICK_CONVERSATION_FRAGMENT = "QuickConversationFragment";
    public static final String DISPLAY_NAME = "displayName";
    public static final String USER_ID = "userId";
    public static final String GROUP_ID = "groupId";
    public static final String GROUP_NAME = "groupName";
    public static final String FIRST_TIME_MTEXTER_FRIEND = "firstTimeMTexterFriend";
    public static final String CONTACT_ID = "contactId";
    public static final String CONTEXT_BASED_CHAT = "contextBasedChat";
    public static final String CONTACT_NUMBER = "contactNumber";
    public static final String APPLICATION_ID = "applicationId";
    public static final String DEFAULT_TEXT = "defaultText";
    public static final String FINAL_PRICE_TEXT = "Final agreed price ";
    public static final String PRODUCT_TOPIC_ID = "topicId";
    public static final String PRODUCT_IMAGE_URL = "productImageUrl";
    private static final String TAG = "ConversationUIService";
    public static final String CONTACT = "CONTACT";
    private static final String APPLICATION_KEY_META_DATA = "com.applozic.application.key";
    public static final String GROUP = "group-";
    public static final String SUCCESS = "success";
    public static String TAKE_ORDER= "takeOrder";
    private Activity activity;
    private BaseContactService baseContactService;
    private MobiComUserPreference userPreference;
    private ApplozicSetting applozicSetting;
    private Conversation conversation;
    private TopicDetail topicDetailsParcelable;
    public static final String CONVERSATION_ID = "CONVERSATION_ID";
    public static final String TOPIC_ID = "TOPIC_ID";
    private Contact contact;
    private MobiComQuickConversationFragmentNew quickConversationFragment;

    public void setQuickConversationFragment(MobiComQuickConversationFragmentNew quickConversationFragment) {
        this.quickConversationFragment = quickConversationFragment;
    }

    public MobiComQuickConversationFragmentNew getQuickConversationFragment() {
        return quickConversationFragment;
    }

    public ConversationUIServiceNew(Activity activity) {
        this.activity = activity;
        this.baseContactService = new AppContactService(this.activity);
        this.userPreference = MobiComUserPreference.getInstance(this.activity);
        this.applozicSetting = ApplozicSetting.getInstance(this.activity);
    }


    public void openConversationFragment(Contact contact , final Channel channel, Integer conversationId) {

        Intent intent = new Intent(activity, ConversationActivity.class);
        intent.putExtra(CONVERSATION_ID, conversationId);
        intent.putExtra(TAKE_ORDER, true);

        if (contact != null) {
            intent.putExtra(USER_ID, contact.getUserId());
            intent.putExtra(DISPLAY_NAME, contact.getDisplayName());
            intent.putExtra(TAKE_ORDER, true);
            activity.startActivity(intent);

        } else if (channel != null) {
            intent.putExtra(GROUP_ID, channel.getKey());
            intent.putExtra(DISPLAY_NAME, channel.getName());
            activity.startActivity(intent);
        }
    }


    public void deleteConversationThread(final Contact contact, final Channel channel) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity).
                setPositiveButton(R.string.delete_conversation, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new DeleteConversationAsyncTask(new MobiComConversationService(activity), contact, channel, null, activity).execute();

                    }
                });
        alertDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        String name = "";
        if (channel != null) {
            name = ChannelUtils.getChannelTitleName(channel, MobiComUserPreference.getInstance(activity).getUserId());
        } else {
            name = contact.getDisplayName();
        }
        alertDialog.setTitle(activity.getString(R.string.dialog_delete_conversation_title).replace("[name]", name));
        alertDialog.setMessage(activity.getString(R.string.dialog_delete_conversation_confir).replace("[name]", name));
        alertDialog.setCancelable(true);
        alertDialog.create().show();
    }


    public void deleteGroupConversation(final Channel channel) {

        if (!Utils.isInternetAvailable(activity)) {
            showToastMessage(activity.getString(R.string.you_dont_have_any_network_access_info));
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity).
                setPositiveButton(R.string.channel_deleting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        final ProgressDialog progressDialog = ProgressDialog.show(activity, "",
                                activity.getString(R.string.deleting_channel_user), true);
                        ApplozicChannelDeleteTask.TaskListener channelDeleteTask = new ApplozicChannelDeleteTask.TaskListener() {
                            @Override
                            public void onSuccess(String response) {
                                Log.i(TAG, "Channel deleted response:" + response);

                            }

                            @Override
                            public void onFailure(String response, Exception exception) {
                                showToastMessage(activity.getString(Utils.isInternetAvailable(activity) ? R.string.applozic_server_error : R.string.you_dont_have_any_network_access_info));
                            }

                            @Override
                            public void onCompletion() {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                            }
                        };
                        ApplozicChannelDeleteTask applozicChannelDeleteTask = new ApplozicChannelDeleteTask(activity, channelDeleteTask, channel);
                        applozicChannelDeleteTask.execute((Void) null);
                    }
                });
        alertDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alertDialog.setMessage(activity.getString(R.string.delete_channel_messages_and_channel_info).replace(activity.getString(R.string.group_name_info), channel.getName()).replace(activity.getString(R.string.groupType_info),Channel.GroupType.BROADCAST.getValue().equals(channel.getType())?"broadcast":"group"));
        alertDialog.setCancelable(true);
        alertDialog.create().show();
    }

    public void channelLeaveProcess(final Channel channel) {
        if (!Utils.isInternetAvailable(activity)) {
            showToastMessage(activity.getString(R.string.you_dont_have_any_network_access_info));
            return;
        }
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity).
                setPositiveButton(R.string.channel_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ApplozicChannelLeaveMember.ChannelLeaveMemberListener applozicLeaveMemberListener = new ApplozicChannelLeaveMember.ChannelLeaveMemberListener() {
                            @Override
                            public void onSuccess(String response, Context context) {
                            }

                            @Override
                            public void onFailure(String response, Exception e, Context context) {
                                showToastMessage(activity.getString(Utils.isInternetAvailable(activity) ? R.string.applozic_server_error : R.string.you_dont_have_any_network_access_info));
                            }
                        };
                        ApplozicChannelLeaveMember applozicChannelLeaveMember = new ApplozicChannelLeaveMember(activity, channel.getKey(), MobiComUserPreference.getInstance(activity).getUserId(), applozicLeaveMemberListener);
                        applozicChannelLeaveMember.setEnableProgressDialog(true);
                        applozicChannelLeaveMember.execute((Void) null);

                    }
                });
        alertDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alertDialog.setMessage(activity.getString(R.string.exit_channel_message_info).replace(activity.getString(R.string.group_name_info), channel.getName()).replace(activity.getString(R.string.groupType_info),Channel.GroupType.BROADCAST.getValue().equals(channel.getType())?"broadcast":"group"));
        alertDialog.setCancelable(true);
        alertDialog.create().show();
    }


    void showToastMessage(final String messageToShow) {
        Toast toast = Toast.makeText(activity, messageToShow, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


    public void updateLatestMessage(Message message, String formattedContactNumber) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().updateLatestMessage(message, formattedContactNumber);
    }

    public void removeConversation(Message message, String formattedContactNumber) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().removeConversation(message, formattedContactNumber);
    }

    public void addMessage(Message message) {
        if (!Message.ContentType.HIDDEN.getValue().equals(message.getContentType())) {
            if (!BroadcastService.isQuick()) {
                return;
            }
            getQuickConversationFragment().addMessage(message);
        }
    }

    public void updateLastMessage(String keyString, String userId) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().updateLastMessage(keyString, userId);
    }



    public void downloadConversations(boolean showInstruction) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().downloadConversations(showInstruction);
    }

    public void setLoadMore(boolean loadMore) {
        if (!BroadcastService.isQuick()) {
            return;
        }
        getQuickConversationFragment().setLoadMore(loadMore);
    }



    public void deleteMessage(String keyString, String userId) {
        updateLastMessage(keyString, userId);

    }

    public void updateLastSeenStatus(String contactId) {
        if (BroadcastService.isQuick()) {
            getQuickConversationFragment().updateLastSeenStatus(contactId);
            return;
        }
    }



    public void deleteConversation(Contact contact, Integer channelKey, String response) {
        if (BroadcastService.isQuick()) {
            getQuickConversationFragment().removeConversation(contact, channelKey, response);
        }
    }


    public void updateChannelSync() {
        if (BroadcastService.isChannelInfo()) {
            ((ChannelInfoActivity) activity).updateChannelList();
        }
    }

    public void startContactActivityForResult(Intent intent, Message message, String messageContent, String[] userIdArray) {
        if (message != null) {
            intent.putExtra(MobiComKitPeopleActivity.FORWARD_MESSAGE, GsonUtils.getJsonFromObject(message, message.getClass()));
        }
        if (messageContent != null) {
            intent.putExtra(MobiComKitPeopleActivity.SHARED_TEXT, messageContent);
        }
        if (userIdArray != null) {
            intent.putExtra(MobiComKitPeopleActivity.USER_ID_ARRAY, userIdArray);
        }

        activity.startActivityForResult(intent, APPLOZIC_CONTACT_OR_GROUP_SELECTED);
    }

    public void startContactActivityForResult() {
        startContactActivityForResult(null, null);
    }

    public void startContactActivityForResult(final Message message, final String messageContent) {
        if (applozicSetting.getTotalOnlineUser() > 0 && Utils.isInternetAvailable(activity)) {
            new DownloadNNumberOfUserAsync(applozicSetting.getTotalOnlineUser(), message, messageContent).execute((Void[]) null);
        } else if (applozicSetting.getTotalRegisteredUsers() > 0 && applozicSetting.isRegisteredUsersContactCall() && !userPreference.getWasContactListServerCallAlreadyDone()) {
            if (Utils.isInternetAvailable(activity)) {
                new DownloadNNumberOfUserAsync(applozicSetting.getTotalRegisteredUsers(), message, messageContent, true).execute((Void[]) null);
            }
        } else {
            Intent intent = new Intent(activity, MobiComKitPeopleActivity.class);
            startContactActivityForResult(intent, message, messageContent, null);
        }
    }

    public void reconnectMQTT() {
        try {
            if (((MobiComKitActivityInterface) activity).getRetryCount() <= 3) {
                if (Utils.isInternetAvailable(activity)) {
                    Log.i(TAG, "Reconnecting to mqtt.");
                    ((MobiComKitActivityInterface) activity).retry();
                    Intent intent = new Intent(activity, ApplozicMqttIntentService.class);
                    intent.putExtra(ApplozicMqttIntentService.SUBSCRIBE, true);
                    activity.startService(intent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class DownloadNNumberOfUserAsync extends AsyncTask<Void, Integer, Long> {

        private Message message;
        private UserService userService;
        private ProgressDialog progressDialog;
        private String messageContent;
        private int nNumberOfUsers;
        private String[] userIdArray;
        boolean callForRegistered;
        private RegisteredUsersApiResponse registeredUsersApiResponse;

        public DownloadNNumberOfUserAsync(int nNumberOfUsers, Message message, String messageContent) {
            this.message = message;
            this.messageContent = messageContent;
            this.nNumberOfUsers = nNumberOfUsers;
            this.userService = UserService.getInstance(activity);
        }

        public DownloadNNumberOfUserAsync(int numberOfUsersToFetch, Message message, String messageContent, boolean callForRegistered) {
            this.callForRegistered = callForRegistered;
            this.message = message;
            this.messageContent = messageContent;
            this.nNumberOfUsers = numberOfUsersToFetch;
            this.userService = UserService.getInstance(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(activity, "",
                    activity.getString(R.string.applozic_contacts_loading_info), true);
        }

        @Override
        protected Long doInBackground(Void... params) {
            if (callForRegistered) {
                registeredUsersApiResponse = userService.getRegisteredUsersList(0l, nNumberOfUsers);
            } else {
                userIdArray = userService.getOnlineUsers(nNumberOfUsers);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (!Utils.isInternetAvailable(activity)) {
                Toast toast = Toast.makeText(activity, activity.getString(R.string.applozic_contacts_loading_error), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
            if (userIdArray != null && userIdArray.length > 0) {
                Intent intent = new Intent(activity, MobiComKitPeopleActivity.class);
                startContactActivityForResult(intent, message, messageContent, userIdArray);
            }

            if (registeredUsersApiResponse != null) {
                userPreference.setWasContactListServerCallAlreadyDone(true);
                Intent intent = new Intent(activity, MobiComKitPeopleActivity.class);
                startContactActivityForResult(intent, message, messageContent, null);
            }

        }
    }

}