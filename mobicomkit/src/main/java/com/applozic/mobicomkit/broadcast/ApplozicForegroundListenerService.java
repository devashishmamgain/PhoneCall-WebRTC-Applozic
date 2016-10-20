package com.applozic.mobicomkit.broadcast;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.Foreground;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.ApplozicMqttIntentService;

/**
 * Created by sunil on 6/7/16.
 */
public class ApplozicForegroundListenerService extends Service implements Foreground.Listener {

    private static final String TAG = "ALForeground";
    MyContentObserver mObserver;

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Started service......");
        mObserver = new MyContentObserver();
        Foreground.get(this).addListener(this);
        getApplicationContext().getContentResolver().registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, mObserver);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags,
                              final int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroy service......");
        Foreground.get(this).removeListener(this);
        getApplicationContext().getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public void onBecameForeground() {
        if (MobiComUserPreference.getInstance(this).isLoggedIn()) {
            Log.i(TAG, "Foreground Subscribing......");
            Intent intent = new Intent(this, ApplozicMqttIntentService.class);
            intent.putExtra(ApplozicMqttIntentService.SUBSCRIBE, true);
            startService(intent);
        }
    }

    @Override
    public void onBecameBackground() {
        Log.i(TAG, "Background UnSubscribing......");
        final String userKeyString = MobiComUserPreference.getInstance(this).getSuUserKeyString();
        if (!TextUtils.isEmpty(userKeyString)) {
            Intent intent = new Intent(this, ApplozicMqttIntentService.class);
            intent.putExtra(ApplozicMqttIntentService.USER_KEY_STRING, userKeyString);
            startService(intent);
        }
    }

    private class MyContentObserver extends ContentObserver {

        public MyContentObserver() {
            super(null);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            MobiComUserPreference.getInstance(getApplicationContext()).setSyncContacts(true);
            Log.i(TAG, "ContentObserver is called for contacts change");
        }

    }

}

