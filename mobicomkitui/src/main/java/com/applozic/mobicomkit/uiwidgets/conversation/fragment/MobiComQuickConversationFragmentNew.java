package com.applozic.mobicomkit.uiwidgets.conversation.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.SyncCallService;
import com.applozic.mobicomkit.api.conversation.database.MessageDatabaseService;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.channel.database.ChannelDatabaseService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationListView;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.MobiComKitActivityInterface;
import com.applozic.mobicomkit.uiwidgets.conversation.adapter.QuickConversationAdapter;
import com.applozic.mobicomkit.InstructionUtil;
import com.applozic.mobicommons.commons.core.utils.DateUtils;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

;

/**
 * Created by devashish on 10/2/15.
 */
public class MobiComQuickConversationFragmentNew extends android.app.Fragment {

    protected ConversationListView listView = null;
    protected ImageButton fabButton;
    protected TextView emptyTextView;
    protected SwipeRefreshLayout swipeLayout;
    protected int listIndex;
    protected Map<String, Message> latestMessageForEachContact = new HashMap<String, Message>();
    protected List<Message> messageList = new ArrayList<Message>();
    protected QuickConversationAdapter conversationAdapter = null;
    protected boolean loadMore = false;
    protected SyncCallService syncCallService;
    private ApplozicSetting applozicSetting;
    private Long minCreatedAtTime;
    private DownloadConversation downloadConversation;
    private BaseContactService baseContactService;
    ConversationUIServiceNew conversationUIService;

    public ConversationListView getListView() {
        return listView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applozicSetting = ApplozicSetting.getInstance(getActivity());
        syncCallService = SyncCallService.getInstance(getActivity());
        conversationAdapter = new QuickConversationAdapter(getActivity(),
                messageList, null);
        conversationUIService = new ConversationUIServiceNew(getActivity());
        baseContactService = new AppContactService(getActivity());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MobiComUserPreference.getInstance(getActivity()).setDeviceTimeOffset(DateUtils.getTimeDiffFromUtc());
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View list = inflater.inflate(R.layout.mobicom_message_list_new, container, false);
        listView = (ConversationListView) list.findViewById(R.id.messageList);
        listView.setBackgroundColor(getResources().getColor(R.color.conversation_list_all_background));
        listView.setScrollToBottomOnSizeChange(Boolean.FALSE);
        fabButton = (ImageButton) list.findViewById(R.id.fab_start_new);

        LinearLayout individualMessageSendLayout = (LinearLayout) list.findViewById(R.id.individual_message_send_layout);
        LinearLayout extendedSendingOptionLayout = (LinearLayout) list.findViewById(R.id.extended_sending_option_layout);

        individualMessageSendLayout.setVisibility(View.GONE);
        extendedSendingOptionLayout.setVisibility(View.GONE);

        View spinnerLayout = inflater.inflate(R.layout.mobicom_message_list_header_footer, null);
        listView.addFooterView(spinnerLayout);

        //spinner = (ProgressBar) spinnerLayout.findViewById(R.id.spinner);
        emptyTextView = (TextView) list.findViewById(R.id.noConversations);
        // startNewButton = (Button) spinnerLayout.findViewById(R.id.start_new_conversation);

      //  fabButton.setVisibility(applozicSetting.isStartNewFloatingActionButtonVisible() ? View.VISIBLE : View.GONE);
        fabButton.setVisibility(View.VISIBLE);

        swipeLayout = (SwipeRefreshLayout) list.findViewById(R.id.swipe_container);
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        listView.setLongClickable(true);
        registerForContextMenu(listView);

        return list;
    }

    protected View.OnClickListener startNewConversation() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MobiComKitActivityInterface) getActivity()).startContactActivityForResult();
            }
        };
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int position = info.position;
        if (messageList.size() <= position) {
            return;
        }
        Message message = messageList.get(position);
        menu.setHeaderTitle(R.string.conversation_options);

        String[] menuItems = getResources().getStringArray(R.array.conversation_options_menu);

        boolean isUserPresentInGroup = false;
        if (message.getGroupId() != null) {
            isUserPresentInGroup =  ChannelService.getInstance(getActivity()).processIsUserPresentInChannel(message.getGroupId());
        }

        for (int i = 0; i < menuItems.length; i++) {

            if (message.getGroupId() == null &&  (menuItems[i].equals("Delete group") ||
                    menuItems[i].equals("Exit group"))) {
                continue;
            }

            if (menuItems[i].equals("Exit group") && !isUserPresentInGroup) {
                continue;
            }

            if (menuItems[i].equals("Delete group") && isUserPresentInGroup) {
                continue;
            }

            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        if (messageList.size() <= position) {
            return true;
        }
        Message message = messageList.get(position);

        Channel channel = null;
        Contact contact = null;

        if (message.getGroupId() != null) {
            channel = ChannelDatabaseService.getInstance(getActivity()).getChannelByChannelKey(message.getGroupId());
        } else {
            contact = baseContactService.getContactById(message.getContactIds());
        }

        switch (item.getItemId()) {
            case 0:
                conversationUIService.deleteConversationThread(contact, channel);
                break;
            case 1:
                conversationUIService.deleteGroupConversation(channel);
                break;
            case 2:
                conversationUIService.channelLeaveProcess(channel);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

//        if (!ApplozicSetting.getInstance(getActivity()).isStartNewButtonVisible()) {
//            menu.removeItem(R.id.start_new);
//        }else {
//            menu.findItem(R.id.start_new).setVisible(true);
//        }
//        if (!ApplozicSetting.getInstance(getActivity()).isStartNewGroupButtonVisible()) {
//            menu.removeItem(R.id.conversations);
//        }else {
//            menu.findItem(R.id.conversations).setVisible(true);
//        }
//        menu.findItem(R.id.refresh).setVisible(true);
    }

    public void addMessage(final Message message) {
        final Context context = getActivity();
        this.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                message.processContactIds(context);
                Message recentMessage;
                if (message.getGroupId() != null) {
                    recentMessage = latestMessageForEachContact.get(ConversationUIService.GROUP + message.getGroupId());
                } else {
                    recentMessage = latestMessageForEachContact.get(message.getContactIds());
                }

                if (recentMessage != null && message.getCreatedAtTime() >= recentMessage.getCreatedAtTime()) {
                    messageList.remove(recentMessage);
                } else if (recentMessage != null) {
                    return;
                }
                if (message.getGroupId() != null) {
                    latestMessageForEachContact.put(ConversationUIService.GROUP + message.getGroupId(), message);
                } else {
                    latestMessageForEachContact.put(message.getContactIds(), message);
                }
                messageList.add(0, message);
                conversationAdapter.notifyDataSetChanged();
                //listView.smoothScrollToPosition(messageList.size());
                listView.setSelection(0);
                emptyTextView.setVisibility(View.GONE);
                // startQNewButton.setVisibility(View.GONE);
            }
        });
    }

    public void updateLastMessage(String keyString, String userId) {
        for (Message message : messageList) {
            if (message.getKeyString() != null && message.getKeyString().equals(keyString)) {
                MessageDatabaseService messageDatabaseService = new MessageDatabaseService(getActivity());
                List<Message> lastMessage = messageDatabaseService.getLatestMessage(userId);
                if (lastMessage.isEmpty()) {
                    removeConversation(message, userId);
                } else {
                    deleteMessage(lastMessage.get(0), userId);
                }
                break;
            }
        }
    }

    public String getLatestContact() {
        if (messageList != null && !messageList.isEmpty()) {
            Message message = messageList.get(0);
            return message.getTo();
        }
        return null;
    }

    public void updateUserName(final Integer channelKey) {
        this.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try{
                    conversationAdapter.notifyDataSetChanged();
                   /* Message message = latestMessageForEachContact.get("group-" + channelKey);
                    if (message != null) {
                        int index = messageList.indexOf(message);
                        if (index != -1) {
                            View view = listView.getChildAt(index -
                                    listView.getFirstVisiblePosition());
                            if (view != null) {
                                TextView receiver = (TextView) view.findViewById(R.id.smReceivers);
                                Channel channel = ChannelService.getInstance(getActivity()).getChannelByChannelKey(channelKey);
                                if(channel != null){
                                    receiver.setText(ChannelUtils.getChannelTitleName(channel, MobiComUserPreference.getInstance(getActivity()).getUserId()));
                                    conversationAdapter.notifyDataSetChanged();
                                }

                            }
                        }
                    }*/
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

    }


    public void deleteMessage(final Message message, final String userId) {
        this.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Message recentMessage;
                if (message.getGroupId() != null) {
                    recentMessage = latestMessageForEachContact.get(ConversationUIService.GROUP + message.getGroupId());
                } else {
                    recentMessage = latestMessageForEachContact.get(userId);
                }
                if (recentMessage != null && message.getCreatedAtTime() <= recentMessage.getCreatedAtTime()) {
                    if (message.getGroupId() != null) {
                        latestMessageForEachContact.put(ConversationUIService.GROUP + message.getGroupId(), message);
                    } else {
                        latestMessageForEachContact.put(userId, message);
                    }

                    messageList.set(messageList.indexOf(recentMessage), message);

                    conversationAdapter.notifyDataSetChanged();
                    if (messageList.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        // startNewButton.setVisibility(applozicSetting.isStartNewButtonVisible() ? View.VISIBLE : View.GONE);
                    }
                }
            }
        });
    }

    public void updateLatestMessage(Message message, String userId) {
        deleteMessage(message, userId);
    }

    public void removeConversation(final Message message, final String userId) {
        this.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                latestMessageForEachContact.remove(userId);
                messageList.remove(message);
                conversationAdapter.notifyDataSetChanged();
                checkForEmptyConversations();
            }
        });
    }

    public void removeConversation(final Contact contact, final Integer channelKey, String response) {

        if ("success".equals(response)) {
            this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Message message = null;
                    if (channelKey != null && channelKey != 0) {
                        message = latestMessageForEachContact.get(ConversationUIService.GROUP + channelKey);
                    } else {
                        message = latestMessageForEachContact.get(contact.getUserId());
                    }
                    messageList.remove(message);
                    if (channelKey != null && channelKey != 0) {
                        latestMessageForEachContact.remove(ConversationUIService.GROUP + channelKey);
                    } else {
                        latestMessageForEachContact.remove(contact.getUserId());
                    }
                    conversationAdapter.notifyDataSetChanged();
                    checkForEmptyConversations();
                }
            });
        } else {
            if (!Utils.isInternetAvailable(getActivity())) {
                Toast.makeText(getActivity(), getString(R.string.you_need_network_access_for_delete), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.delete_conversation_failed), Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void checkForEmptyConversations() {
        boolean isLodingConversation = (downloadConversation != null && downloadConversation.getStatus() == AsyncTask.Status.RUNNING);
        if (latestMessageForEachContact.isEmpty() && !isLodingConversation) {
            emptyTextView.setVisibility(View.VISIBLE);
            //startNewButton.setVisibility(applozicSetting.isStartNewButtonVisible() ? View.VISIBLE : View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            // startNewButton.setVisibility(View.GONE);
        }
    }

    public void setLoadMore(boolean loadMore) {
        this.loadMore = loadMore;
    }

    @Override
    public void onPause() {
        super.onPause();
        listIndex = listView.getFirstVisiblePosition();
        BroadcastService.currentUserId = null;
        if(conversationAdapter != null){
            conversationAdapter.contactImageLoader.setPauseWork(false);
            conversationAdapter.channelImageLoader.setPauseWork(false);
        }
    }

    @Override
    public void onResume() {
        //Assigning to avoid notification in case if quick conversation fragment is opened....
        BroadcastService.selectMobiComKitAll();
        super.onResume();
        latestMessageForEachContact.clear();
        messageList.clear();
        if (listView != null) {
            if (listView.getCount() > listIndex) {
                listView.setSelection(listIndex);
            } else {
                listView.setSelection(0);
            }
        }
        downloadConversations();
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                SyncMessages syncMessages = new SyncMessages();
                syncMessages.execute();
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //FlurryAgent.logEvent(QUICK_CONVERSATION_EVENT);
        listView.setAdapter(conversationAdapter);
        fabButton.setOnClickListener(startNewConversation());
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                if (conversationAdapter != null) {
                    conversationAdapter.contactImageLoader.setPauseWork(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING);
                    conversationAdapter.channelImageLoader.setPauseWork(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING);
                }

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount != 0 && loadMore) {
                    loadMore = false;
                    DownloadConversation downloadConversation = new DownloadConversation(view, false, firstVisibleItem, visibleItemCount, totalItemCount);
                    AsyncTaskCompat.executeParallel(downloadConversation);
                }
            }
        });
    }


    public void downloadConversations() {
        downloadConversations(false);
    }

    public void downloadConversations(boolean showInstruction) {
        minCreatedAtTime = null;
        downloadConversation = new DownloadConversation(listView, true, 1, 0, 0, showInstruction);
        AsyncTaskCompat.executeParallel(downloadConversation);
    }

    public void updateLastSeenStatus(final String userId) {
        if (!ApplozicSetting.getInstance(getActivity()).isOnlineStatusInMasterListVisible()) {
            return;
        }
        this.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Message message = latestMessageForEachContact.get(userId);
                    if (message != null) {
                        int index = messageList.indexOf(message);
                        View view = listView.getChildAt(index - listView.getFirstVisiblePosition());
                        if (view != null) {
                            TextView onlineTextView = (TextView) view.findViewById(R.id.onlineTextView);
                            Contact contact = baseContactService.getContactById(userId);
                            onlineTextView.setVisibility(contact != null && contact.isOnline() ? View.VISIBLE : View.GONE);
                        }
                    }
                } catch (Exception ex) {
                    Log.w("AL", "Exception while updating online status.");
                }
            }
        });

    }

    public class DownloadConversation extends AsyncTask<Void, Integer, Long> {

        private AbsListView view;
        private int firstVisibleItem;
        private int amountVisible;
        private int totalItems;
        private boolean initial;
        private boolean showInstruction;
        private List<Message> nextMessageList = new ArrayList<Message>();
        private Context context;

        public DownloadConversation(AbsListView view, boolean initial, int firstVisibleItem, int amountVisible, int totalItems, boolean showInstruction) {
            this.context = getActivity();
            this.view = view;
            this.initial = initial;
            this.firstVisibleItem = firstVisibleItem;
            this.amountVisible = amountVisible;
            this.totalItems = totalItems;
            this.showInstruction = showInstruction;
        }

        public DownloadConversation(AbsListView view, boolean initial, int firstVisibleItem, int amountVisible, int totalItems) {
            this(view, initial, firstVisibleItem, amountVisible, totalItems, false);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadMore = false;
            swipeLayout.setEnabled(true);
            swipeLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(true);
                }
            });
        }

        protected Long doInBackground(Void... voids) {
            //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);
            if (initial) {
                nextMessageList = syncCallService.getLatestMessagesGroupByPeople();
                if (!nextMessageList.isEmpty()) {
                    minCreatedAtTime = nextMessageList.get(nextMessageList.size() - 1).getCreatedAtTime();
                }
            } else if (!messageList.isEmpty()) {
                listIndex = firstVisibleItem;
                Long createdAt = messageList.isEmpty() ? null : messageList.get(messageList.size() - 1).getCreatedAtTime();
                minCreatedAtTime = (minCreatedAtTime == null ? createdAt : Math.min(minCreatedAtTime, createdAt));
                nextMessageList = syncCallService.getLatestMessagesGroupByPeople(minCreatedAtTime);
            }

            return 0L;
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Long result) {
            swipeLayout.setEnabled(true);
            swipeLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(false);
                }
            });
            for (Message currentMessage : nextMessageList) {
                if (currentMessage.isSentToMany()) {
                    continue;
                }
                Message recentSms;
                if (currentMessage.getGroupId() != null) {
                    recentSms = latestMessageForEachContact.get(ConversationUIService.GROUP + currentMessage.getGroupId());
                } else {
                    recentSms = latestMessageForEachContact.get(currentMessage.getContactIds());
                }

                if (recentSms != null) {
                    if (currentMessage.getCreatedAtTime() >= recentSms.getCreatedAtTime()) {
                        if (currentMessage.getGroupId() != null) {
                            latestMessageForEachContact.put(ConversationUIService.GROUP + currentMessage.getGroupId(), currentMessage);
                        } else {
                            latestMessageForEachContact.put(currentMessage.getContactIds(), currentMessage);
                        }

                        Log.d("Current message", "message" + currentMessage);
                        messageList.remove(recentSms);
                        messageList.add(currentMessage);
                    }
                } else {
                    if (currentMessage.getGroupId() != null) {
                        latestMessageForEachContact.put(ConversationUIService.GROUP + currentMessage.getGroupId(), currentMessage);
                    } else {
                        latestMessageForEachContact.put(currentMessage.getContactIds(), currentMessage);
                    }

                    messageList.add(currentMessage);
                }
            }

            conversationAdapter.notifyDataSetChanged();
            if (initial) {
                emptyTextView.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
                if (applozicSetting.isStartNewButtonVisible()) {
                    // startNewButton.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (!messageList.isEmpty()) {
                    listView.setSelection(0);
                }
            } else {
                listView.setSelection(firstVisibleItem);
            }
            /*if (isAdded()) {
                //Utils.isNetworkAvailable(getActivity(), errorMessage);
                if (!Utils.isInternetAvailable(getActivity())) {
                    String errorMessage = getResources().getString(R.string.internet_connection_not_available);
                    ((MobiComKitActivityInterface) getActivity()).showErrorMessageView(errorMessage);
                }
            }*/

            if (context != null && showInstruction) {
                InstructionUtil.showInstruction(context, R.string.instruction_open_conversation_thread, MobiComKitActivityInterface.INSTRUCTION_DELAY, BroadcastService.INTENT_ACTIONS.INSTRUCTION.toString());
            }
        }
    }
    private class SyncMessages extends AsyncTask<Void, Integer, Long>{
        SyncMessages(){
        }
        @Override
        protected Long doInBackground(Void... params) {
            syncCallService.syncMessages(null);
            return 1l;
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            swipeLayout.setRefreshing(false);
        }
    }
}