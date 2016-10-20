package com.applozic.mobicomkit.api.people;

import android.app.IntentService;
import android.content.Intent;

import com.applozic.mobicomkit.api.conversation.SyncCallService;

/**
 * Created by devashish on 15/12/13.
 */
public class UserIntentService extends IntentService {

    private static final String TAG = "UserIntentService";
    public static final String USER_ID = "userId";

    public UserIntentService() {
        super("UserIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String userId = intent.getStringExtra(USER_ID);
        SyncCallService.getInstance(UserIntentService.this).processUserStatus(userId);
    }

}
