package com.applozic.mobicomkit.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;

/**
 * Created by sunil on 6/7/16.
 */
public class ApplozicBootCompletedReceiver extends BroadcastReceiver {
    final static String TAG = "BootCompletedReceiver";
    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, action);

        if (!MobiComUserPreference.getInstance(context).isLoggedIn()) {
            return;
        }

        if (action.equalsIgnoreCase(BOOT_COMPLETED)) {
            Intent chatService = new Intent(context, ApplozicForegroundListenerService.class);
            context.startService(chatService);
        }
    }
}
