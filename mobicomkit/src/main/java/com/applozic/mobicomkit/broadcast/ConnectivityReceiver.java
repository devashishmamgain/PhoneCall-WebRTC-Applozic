package com.applozic.mobicomkit.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.net.ConnectivityManager;
import android.util.Log;

import com.applozic.mobicomkit.InstructionUtil;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.UserService;
import com.applozic.mobicomkit.api.conversation.MessageClientService;
import com.applozic.mobicomkit.api.conversation.MobiComConversationService;
import com.applozic.mobicomkit.api.conversation.SyncCallService;
import com.applozic.mobicommons.commons.core.utils.Utils;

/**
 * Created by devashish on 29/08/15.
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    static final private String TAG = "ConnectivityReceiver";
    static final private String CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    Context context;
    private static boolean firstConnect = true;
    private MessageClientService messageClientService;
    private MobiComConversationService conversationService;

    @Override
    public void onReceive(final Context context, Intent intent) {
        this.context = context;
        this.messageClientService = new MessageClientService(context);
        this.conversationService = new MobiComConversationService(context);

        String action = intent.getAction();

        Log.i(TAG, action);

        if (action.equalsIgnoreCase(CONNECTIVITY_CHANGE) ||  action.equalsIgnoreCase(BOOT_COMPLETED)) {
            if (!Utils.isInternetAvailable(context)) {
                firstConnect = true;
                return;
            }
            if (!MobiComUserPreference.getInstance(context).isLoggedIn()) {
                return;
            }
            ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
            if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                if (firstConnect) {
                    firstConnect = false;
                    Thread thread =  new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SyncCallService.getInstance(context).syncMessages(null);
                            messageClientService.syncPendingMessages(true);
                            messageClientService.syncDeleteMessages(true);
                            conversationService.processLastSeenAtStatus();
                            UserService.getInstance(context).processSyncUserBlock();
                        }
                    });
                    thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    thread.start();
                }
            }
        }
    }

}

