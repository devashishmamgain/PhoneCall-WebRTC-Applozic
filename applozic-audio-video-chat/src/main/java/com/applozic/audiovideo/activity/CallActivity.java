package com.applozic.audiovideo.activity;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.attachment.VideoCallNotificationHelper;
import com.applozic.mobicomkit.api.conversation.MessageIntentService;
import com.applozic.mobicomkit.api.conversation.MobiComMessageService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicommons.commons.image.ImageLoader;
import com.applozic.mobicommons.people.contact.Contact;

import applozic.com.audiovideo.R;


public class CallActivity extends Activity {

    private static final String TAG = CallActivity.class.getName();

    BaseContactService baseContactService;
    MobiComMessageService messageService;
    ImageLoader mImageLoader;
    boolean responded;
    private BroadcastReceiver applozicBroadCastReceiver;
    Contact contact;
    String inComingCallId;
    boolean isAudioOnly;
    Vibrator vibrator;
    Ringtone r;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_received);
        Log.i(TAG, "Reached CallActivity");

        //Notifications and Vibrations...
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 1000};
        vibrator.vibrate(pattern, 0);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();

        baseContactService = new AppContactService(this);
        messageService = new MobiComMessageService(this, MessageIntentService.class);
        Intent intent = getIntent();

        //// contactId /////////
        final String contactId = intent.getStringExtra("CONTACT_ID");
        inComingCallId = intent.getStringExtra(VideoCallNotificationHelper.CALL_ID);
        isAudioOnly = intent.getBooleanExtra(VideoCallNotificationHelper.CALL_AUDIO_ONLY, false);

        Log.i(TAG, "contactId: " + contactId + ", inComingCallId: " + inComingCallId + ", isAudioOnly: " + isAudioOnly);

        contact = baseContactService.getContactById(contactId);

        Animation shake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shaking_ani);
        shake.setRepeatCount(Animation.INFINITE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        ImageButton accept = (ImageButton) findViewById(R.id.alarmlistitem_acceptButton);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    responded = true;
                    Class activityClass = isAudioOnly ? AudioCallActivity.class : VideoActivity.class;
                    Intent intent = new Intent(getApplicationContext(), activityClass);
                    intent.putExtra("CONTACT_ID", contactId);
                    intent.putExtra("INCOMING_CALL", Boolean.TRUE);
                    intent.putExtra("CALL_ID", inComingCallId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    vibrator.cancel();
                    if (r.isPlaying()) {
                        r.stop();
                    }
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        accept.startAnimation(shake);

        ImageButton reject = (ImageButton) findViewById(R.id.alarmlistitem_rejectButton);
        ImageView profileImage = (ImageView) findViewById(R.id.notification_profile_image);
        TextView textView = (TextView) findViewById(R.id.notification_user_name);

        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rejectCall();
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!responded) {
                        Log.i(TAG, "Rejecting call due to responded being false.");
                        rejectCall();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, VideoCallNotificationHelper.MAX_NOTIFICATION_RING_DURATION);

        mImageLoader = new ImageLoader(this, profileImage.getHeight()) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return baseContactService.downloadContactImage(CallActivity.this, (Contact) data);
            }
        };
        mImageLoader.setLoadingImage(R.drawable.applozic_ic_contact_picture_180_holo_light);
        // Add a cache to the image loader
        mImageLoader.setImageFadeIn(false);
        mImageLoader.loadImage(contact, profileImage);
        textView.setText(contact.getDisplayName());
        applozicBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String callId = intent.getStringExtra(VideoCallNotificationHelper.CALL_ID);
                boolean isNotificationForSameId = (inComingCallId.equals(callId));
                if ((VideoCallNotificationHelper.CALL_CANCELED.equals(intent.getAction()) ||
                        MobiComKitConstants.APPLOZIC_VIDEO_CALL_REJECTED.equals(intent.getAction()))
                        && isNotificationForSameId) {
                    responded = true;
                    vibrator.cancel();
                    if (r.isPlaying()) {
                        r.stop();
                    }
                    finish();
                }
            }
        };
        registerForBroadcast();
    }

    private void rejectCall() {
        try {
            responded = true;

            VideoCallNotificationHelper helper = new VideoCallNotificationHelper(CallActivity.this, isAudioOnly);
            helper.sendVideoCallReject(contact, inComingCallId);
            vibrator.cancel();
            if (r.isPlaying()) {
                r.stop();
            }
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void registerForBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VideoCallNotificationHelper.CALL_CANCELED);
        intentFilter.addAction(MobiComKitConstants.APPLOZIC_VIDEO_CALL_REJECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(applozicBroadCastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(applozicBroadCastReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        rejectCall();
    }
}