package com.applozic.audiovideo.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.audiovideo.authentication.Dialog;
import com.applozic.audiovideo.authentication.MakeAsyncRequest;
import com.applozic.audiovideo.authentication.Token;
import com.applozic.audiovideo.authentication.TokenGeneratorCallback;
import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.attachment.VideoCallNotificationHelper;
import com.applozic.mobicomkit.api.conversation.MessageIntentService;
import com.applozic.mobicomkit.api.conversation.MobiComMessageService;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.commons.image.ImageLoader;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.contact.Contact;
import com.twilio.common.AccessManager;
import com.twilio.conversations.AudioOutput;
import com.twilio.conversations.AudioTrack;
import com.twilio.conversations.CameraCapturer;
import com.twilio.conversations.CapturerErrorListener;
import com.twilio.conversations.CapturerException;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationCallback;
import com.twilio.conversations.IncomingInvite;
import com.twilio.conversations.InviteStatus;
import com.twilio.conversations.LocalMedia;
import com.twilio.conversations.LocalVideoTrack;
import com.twilio.conversations.LogLevel;
import com.twilio.conversations.MediaTrack;
import com.twilio.conversations.OutgoingInvite;
import com.twilio.conversations.Participant;
import com.twilio.conversations.TwilioConversationsClient;
import com.twilio.conversations.TwilioConversationsException;
import com.twilio.conversations.VideoRenderer;
import com.twilio.conversations.VideoScaleType;
import com.twilio.conversations.VideoTrack;
import com.twilio.conversations.VideoViewRenderer;

import java.util.HashSet;
import java.util.Set;

import applozic.com.audiovideo.R;

public class AudioCallActivity extends AppCompatActivity implements TokenGeneratorCallback {

    private static final String TAG = AudioCallActivity.class.getName();
    protected static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    protected BroadcastReceiver applozicBroadCastReceiver;
    protected boolean incomingCall;
    protected boolean inviteSent;
    protected String callId;
    protected long callStartTime;
    protected boolean autoCall = false;
    protected MediaPlayer mediaPlayer;
    protected static boolean isInOpenStatus = false;

    protected boolean videoCall = false;

    /* Twilio Conversations Client allows a client to create or participate in a conversation.
     */

    protected TwilioConversationsClient conversationsClient;

    /*
     * A Conversation represents communication between the client and one or more participants.
     */
    public Conversation conversation;

    /*
     * An OutgoingInvite represents an invitation to start or join a conversation with one or
     * more participants
     */
    protected OutgoingInvite outgoingInvite;

    /*
     * A VideoViewRenderer receives frames from a local or remote video track and renders
     * the frames to a provided view
     */
    protected VideoViewRenderer participantVideoRenderer;
    protected VideoViewRenderer localVideoRenderer;

    /*
     * Android application UI elements
     */
    protected FrameLayout previewFrameLayout;
    protected ViewGroup localContainer;
    protected ViewGroup participantContainer;
    protected TextView conversationStatusTextView;
    protected AccessManager accessManager;
    protected CameraCapturer cameraCapturer;
    protected FloatingActionButton callActionFab;
    protected FloatingActionButton muteActionFab;
    protected FloatingActionButton speakerActionFab;
    protected android.support.v7.app.AlertDialog alertDialog;
    protected AudioManager audioManager;
    protected AppContactService contactService;
    protected TextView contactName;
    protected ImageView profileImage;

    protected VideoCallNotificationHelper videoCallNotificationHelper;
    protected Contact contactToCall;

    protected boolean muteMicrophone;
    protected boolean pauseVideo;
    protected int savedAudioMode = AudioManager.MODE_INVALID;

    protected boolean wasPreviewing;
    protected boolean wasLive;

    protected boolean loggingOut;
    protected Token token;
    protected boolean answered;
    protected ProgressDialog progress;

    protected boolean debugMode = false;

    ImageLoader mImageLoader;
    CountDownTimer timer;
    TextView txtCount;
    private int cnt;

    protected MobiComMessageService messageService;

    public AudioCallActivity() {
        this.videoCall = false;
    }

    public AudioCallActivity(boolean videoCall) {
        this.videoCall = videoCall;
    }

    public static boolean isInOpenStatus() {
        return isInOpenStatus;
    }

    public static void setIsInOpenStatus(boolean isInOpenStatus) {
        AudioCallActivity.isInOpenStatus = isInOpenStatus;
        BroadcastService.videoCallAcitivityOpend = isInOpenStatus;
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        autoCall = true;

        if (videoCall) {
            return;
        }
        setContentView(R.layout.applozic_audio_call);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        init();
        /*
         * Load views from resources
         */
        previewFrameLayout = (FrameLayout) findViewById(R.id.previewFrameLayout);
        localContainer = (ViewGroup) findViewById(R.id.localContainer);
        participantContainer = (ViewGroup) findViewById(R.id.participantContainer);
        conversationStatusTextView = (TextView) findViewById(R.id.conversation_status_textview);
        contactName = (TextView) findViewById(R.id.contact_name);
        profileImage = (ImageView) findViewById(R.id.applozic_audio_profile_image);
        txtCount = (TextView) findViewById(R.id.applozic_audio_timer);

        contactName.setText(contactToCall.getDisplayName());

        previewFrameLayout.setVisibility(View.GONE);
        localContainer.setVisibility(View.GONE);
        participantContainer.setVisibility(View.GONE);
        pauseVideo = true;

        mImageLoader = new ImageLoader(this, profileImage.getHeight()) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return contactService.downloadContactImage(AudioCallActivity.this, (Contact) data);
            }
        };
        mImageLoader.setLoadingImage(R.drawable.applozic_ic_contact_picture_holo_light);
        // Add a cache to the image loader
        mImageLoader.setImageFadeIn(false);
        mImageLoader.loadImage(contactToCall, profileImage);

        if (!debugMode) {
            conversationStatusTextView.setVisibility(View.GONE);
        }
        callActionFab = (FloatingActionButton) findViewById(R.id.call_action_fab);
        muteActionFab = (FloatingActionButton) findViewById(R.id.mute_action_fab);
        speakerActionFab = (FloatingActionButton) findViewById(R.id.speaker_action_fab);

        initialize();
    }

    public void initialize() {
         /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        /*
         * Needed for setting/abandoning audio focus during call
         */
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = MediaPlayer.create(this, R.raw.hangouts_video_call);
        mediaPlayer.setLooping(true);

        setCallAction();
        checkForInternet();

         /*
         * Check camera and microphone permissions. Needed in Android M.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            initializeTwilioSdk();

            if (incomingCall) {
                progress = new ProgressDialog(this);
                progress.setMessage("Connecting...");
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setIndeterminate(true);
                progress.setCancelable(false);
                progress.show();
            }

            LocalBroadcastManager.getInstance(this).registerReceiver(applozicBroadCastReceiver,
                    BrodCastIntentFilters());
        }

        timer = initializeTimer();
    }

    public void checkForInternet() {
        if (!Utils.isInternetAvailable(this)) {
            Toast toast = Toast.makeText(this, getString(R.string.internet_connection_not_available), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
    }

    @NonNull
    public CountDownTimer initializeTimer() {
        return new CountDownTimer(Long.MAX_VALUE, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                cnt++;
                long millis = cnt;
                int seconds = (int) (millis / 60);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                txtCount.setText(String.format("%d:%02d:%02d", minutes, seconds, millis));
            }

            @Override
            public void onFinish() {

            }
        };
    }

    protected void init() {
        Intent intent = getIntent();
        String contactId = intent.getStringExtra("CONTACT_ID");
        Log.i(TAG, "ContactId: " + contactId);
        contactService = new AppContactService(this);
        messageService = new MobiComMessageService(this, MessageIntentService.class);
        videoCallNotificationHelper = new VideoCallNotificationHelper(this, !videoCall);

        contactToCall = contactService.getContactById(contactId);
        incomingCall = intent.getBooleanExtra("INCOMING_CALL", Boolean.FALSE);
        callId = intent.getStringExtra("CALL_ID");
        registerForNotificationBroadcast();
    }

    public void initiateCall() {
        setHangupAction();
        callId = videoCallNotificationHelper.sendAudioCallRequest(contactToCall);
        scheduleStopRinging(callId);
        inviteSent = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE &&
                permissions.length > 0) {
            boolean granted = true;
            /*
             * Check if all permissions are granted
             */
            for (int i = 0; i < permissions.length; i++) {
                granted = granted && (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            if (granted) {
                /*
                 * Initialize the Twilio Conversations SDK
                 */
                initializeTwilioSdk();
            } else {
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                setCallAction();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setIsInOpenStatus(true);
        if (TwilioConversationsClient.isInitialized() &&
                conversationsClient != null &&
                !conversationsClient.isListening()) {
            conversationsClient.listen();
        }
        // Resume preview
        if (cameraCapturer != null && wasPreviewing) {
            cameraCapturer.startPreview(previewFrameLayout);
            wasPreviewing = false;
        }
        // Resume live video
        if (conversation != null && wasLive) {
            pauseVideo(false);
            wasLive = false;
        }

    }


    @Override
    public void onPause() {
        super.onPause();
        autoCall = false;
//        if (TwilioConversationsClient.isInitialized() &&
//                conversationsClient != null  &&
//                conversationsClient.isListening() &&
//                conversation == null) {
//            conversationsClient.unlisten();
//        }
        // Stop preview before going to the background
        if (cameraCapturer != null && cameraCapturer.isPreviewing()) {
            cameraCapturer.stopPreview();
            wasPreviewing = true;
        }
        // Pause live video before going to the background
        if (conversation != null && !pauseVideo) {
            pauseVideo(true);
            wasLive = true;
        }
        setIsInOpenStatus(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        TwilioConversationsClient.destroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(applozicBroadCastReceiver);
    }


    /*
     * The initial state when there is no active conversation.
     */
    protected void setCallAction() {
        callActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_white_24px));
        callActionFab.show();
        callActionFab.setOnClickListener(callActionFabClickListener());
        muteActionFab.show();
        muteActionFab.setOnClickListener(muteClickListener());
        speakerActionFab.hide();
        muteActionFab.hide();
    }

    /*
     * The actions performed during hangup.
     */
    protected void setHangupAction() {
        callActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_end_white_24px));
        callActionFab.show();
        callActionFab.setOnClickListener(hangupClickListener());
        speakerActionFab.show();
        muteActionFab.show();
        speakerActionFab.setOnClickListener(speakerClickListener());
        if (!incomingCall) {
            mediaPlayer.start();
        }
    }

    /*
     * Creates an outgoing conversation UI dialog
     */
    private void showCallDialog() {
        //alertDialog.show();
        mediaPlayer.start();
    }

    /*
     * Initialize the Twilio Conversations SDK
     */
    public void initializeTwilioSdk() {
        TwilioConversationsClient.setLogLevel(LogLevel.ERROR);

        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
            return;
        }
        if (!TwilioConversationsClient.isInitialized()) {

            TwilioConversationsClient.initialize(getApplicationContext());

            if (!Utils.isInternetAvailable(AudioCallActivity.this)) {
                showNetworkError();
                return;
            } else if (MobiComUserPreference.getInstance(this).getVideoCallToken() == null) {
                AsyncTask asyncTask = new MakeAsyncRequest(this, this);
                asyncTask.execute(new String[]{"https://192.168.0.113:/token", "GET"});

            } else {
                retrieveAccessTokenfromServer(new Token(contactToCall.getUserId(), MobiComUserPreference.getInstance(this).getVideoCallToken()));
            }
        }
    }

    public void startPreview() {
        getCameraCapturer().startPreview(previewFrameLayout);
    }

    private void stopPreview() {
        if (cameraCapturer != null && cameraCapturer.isPreviewing()) {
            cameraCapturer.stopPreview();
        }
    }

    public void hangup(boolean islogout) {
        loggingOut = islogout;
        inviteSent = false;
        if (conversation != null) {
            conversation.disconnect();
        } else if (outgoingInvite != null) {
            outgoingInvite.cancel();
        }
        setAudioFocus(false);
        hideProgress();
        if (islogout) {
            loggingOut = true;
            LocalBroadcastManager.getInstance(this).unregisterReceiver(applozicBroadCastReceiver);
            logout("hangup");
        }
    }


    public void logout(String from) {

        Log.i(TAG, "logout is called from ###" + from);

        // Teardown preview
        if (!TwilioConversationsClient.isInitialized()) {
            return;
        }
        if (cameraCapturer != null && cameraCapturer.isPreviewing()) {
            stopPreview();
            cameraCapturer = null;
        }

        conversation = null;

        // Lets unlisten first otherwise complete logout
        if (conversationsClient != null && conversationsClient.isListening()) {
            conversationsClient.unlisten();
        } else {
            completeLogout();
        }
    }

    /*
     * Once all conversations have been ended and invites are no longer being listened for, the
     * Conversations SDK can be torn down
     */
    public void completeLogout() {
        Log.i(TAG, "completeLogout ::: conversationsClient setting as null ");
        conversationsClient = null;
        destroyConversationsSdk();

        // Only required if you are done using the access manager
        disposeAccessManager();

        finish();
        loggingOut = false;
        videoCall = false;
    }

    protected void destroyConversationsSdk() {
        TwilioConversationsClient.destroy();
    }

    protected void disposeAccessManager() {
        if (accessManager != null) {
            accessManager.dispose();
            accessManager = null;
        }
    }

    /*
     * Resets UI elements. Used after conversation has ended.
     */
    private void reset() {

        if (participantVideoRenderer != null) {
            participantVideoRenderer.release();
            participantVideoRenderer = null;
        }
        localContainer.removeAllViews();
        localContainer = (ViewGroup) findViewById(R.id.localContainer);
        participantContainer.removeAllViews();

        conversation = null;
        outgoingInvite = null;
        callId = null;
        inviteSent = false;
        muteMicrophone = false;
        muteActionFab.setImageDrawable(
                ContextCompat.getDrawable(AudioCallActivity.this,
                        R.drawable.ic_mic_green_24px));

        pauseVideo = true;
        speakerActionFab.setImageDrawable(
                ContextCompat.getDrawable(AudioCallActivity.this,
                        R.drawable.ic_volume_down_green_24px));
        setSpeakerphoneOn(false);

        setCallAction();
        if (!wasPreviewing) {
            startPreview();
        }
    }

    protected DialogInterface.OnClickListener acceptCallClickListner(final Contact contact) {
        final Context context = this;
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progress = new ProgressDialog(context);
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setIndeterminate(true);
                progress.setMessage("Answering...");
                progress.setCancelable(false);
                progress.show();
                setHangupAction();
                if (!contactToCall.getContactIds().equals(contact.getContactIds())) {
                    contactToCall = contact;
                }
                sendInvite();
                alertDialog.dismiss();
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }
        };
    }

    private DialogInterface.OnClickListener rejectCallClickListener(final Contact contact) {

        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                videoCallNotificationHelper.sendVideoCallReject(contact, callId);
                setCallAction();
                hideProgress();
            }
        };

    }

    private View.OnClickListener hangupClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conversation == null) {
                    videoCallNotificationHelper.sendVideoCallCanceledNotification(contactToCall, callId);
                    videoCallNotificationHelper.sendVideoCallCanceled(contactToCall, callId);
                }
                hangup(true);
            }
        };
    }


    public boolean pauseVideo(boolean pauseVideo) {
        /*
         * Enable/disable local video track
         */
        if (conversation != null) {
            LocalVideoTrack videoTrack =
                    conversation.getLocalMedia().getLocalVideoTracks().get(0);
            if (videoTrack != null) {
                return videoTrack.enable(!pauseVideo);
            }
        }
        return false;
    }

    private View.OnClickListener muteClickListener() {
        final Context context = this;
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Mute/unmute microphone
                 */
                muteMicrophone = !muteMicrophone;
                if (conversation != null) {
                    conversation.getLocalMedia().mute(muteMicrophone);
                }
                if (muteMicrophone) {
                    muteActionFab.setImageDrawable(
                            ContextCompat.getDrawable(context,
                                    R.drawable.ic_mic_off_red_24px));
                } else {
                    muteActionFab.setImageDrawable(
                            ContextCompat.getDrawable(context,
                                    R.drawable.ic_mic_green_24px));
                }
            }
        };
    }

    protected void setSpeakerphoneOn(boolean on) {
        if (conversationsClient == null) {
            Log.e(TAG, "Unable to set audio output, conversation client is null");
            return;
        }

        try {
            if (conversationsClient.isListening()) {
                conversationsClient.setAudioOutput(on ? AudioOutput.SPEAKERPHONE : AudioOutput.HEADSET);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (on) {
            Drawable drawable = ContextCompat.getDrawable(this,
                    R.drawable.ic_volume_down_green_24px);
            speakerActionFab.setImageDrawable(drawable);
        } else {
            // route back to headset
            Drawable drawable = ContextCompat.getDrawable(this,
                    R.drawable.ic_volume_down_white_24px);
            speakerActionFab.setImageDrawable(drawable);
        }
    }

    private View.OnClickListener speakerClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Audio routing to speakerphone or headset
                 */
                if (conversationsClient == null) {
                    Log.e(TAG, "Unable to set audio output, conversation client is null");
                    return;
                }
                setSpeakerphoneOn(!(conversationsClient.getAudioOutput() == AudioOutput.SPEAKERPHONE));
            }
        };
    }

    protected View.OnClickListener callActionFabClickListener() {
        final Context context = this;
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //If it is in progress..do not call
                if (!Utils.isInternetAvailable(context)) {
                    showNetworkError();
                    return;
                }
                if (!TwilioConversationsClient.isInitialized()) {
                    initializeTwilioSdk();
                    return;
                }
                initiateCall();
            }


        };
    }

    private DialogInterface.OnClickListener closeSessionListener() {

        return new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!incomingCall) {
                    long duration = System.currentTimeMillis() - callStartTime;
                    videoCallNotificationHelper.sendVideoCallEnd(contactToCall, callId, String.valueOf(duration));
                }
                if (conversation != null && conversation.isActive()) {
                    conversation.disconnect();
                }
                loggingOut = true;
                logout("closeSessionListener");
            }
        };
    }

    protected void showNetworkError() {
        Toast toast = Toast.makeText(this, getString(R.string.internet_connection_not_available), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /*
     * Conversation Listener
     */
    private Conversation.Listener conversationListener() {
        return new Conversation.Listener() {
            @Override
            public void onParticipantConnected(Conversation conversation, Participant participant) {
                conversationStatusTextView.setText("onParticipantConnected " + participant.getIdentity());
                participant.setParticipantListener(participantListener());
                //SEND ..CALL STARTED messages
                callStartTime = System.currentTimeMillis();

               if(!videoCall){
                   timer.start();
               }
                if (!incomingCall) {
                    inviteSent = false;
                    videoCallNotificationHelper.sendVideoCallStarted(contactToCall, callId);
                }
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }

            }

            @Override
            public void onFailedToConnectParticipant(Conversation conversation,
                                                     Participant participant,
                                                     TwilioConversationsException e) {
                Log.e(TAG, e.getMessage());
                conversationStatusTextView.setText("onFailedToConnectParticipant " +
                        participant.getIdentity());
                callId = null;

            }

            @Override
            public void onParticipantDisconnected(Conversation conversation,
                                                  Participant participant) {
                conversationStatusTextView.setText("onParticipantDisconnected " +
                        participant.getIdentity());
                callId = null;

            }

            @Override
            public void onConversationEnded(Conversation conversation,
                                            TwilioConversationsException e) {
                conversationStatusTextView.setText("onConversationEnded");
                inviteSent = false;
                callId = null;
                if (!incomingCall) {

                    long duration = System.currentTimeMillis() - callStartTime;
                    videoCallNotificationHelper.sendVideoCallEnd(contactToCall, callId, String.valueOf(duration));
                    timer.cancel();

                }
                if (!loggingOut) {
                    reset();
                    hangup(true);
                }
                finish();
            }
        };
    }

    /*
     * LocalMedia listener
     */
    private LocalMedia.Listener localMediaListener() {

        return new LocalMedia.Listener() {
            @Override
            public void onLocalVideoTrackAdded(LocalMedia localMedia,
                                               LocalVideoTrack localVideoTrack) {
                conversationStatusTextView.setText("onLocalVideoTrackAdded");
                localVideoRenderer = new VideoViewRenderer(AudioCallActivity.this,
                        localContainer);
                localVideoRenderer.applyZOrder(true);
                // localVideoTrack.addRenderer(localVideoRenderer);
            }

            @Override
            public void onLocalVideoTrackRemoved(LocalMedia localMedia,
                                                 LocalVideoTrack localVideoTrack) {
                conversationStatusTextView.setText("onLocalVideoTrackRemoved");
                localContainer.removeAllViews();
                localVideoRenderer.release();
            }

            @Override
            public void onLocalVideoTrackError(LocalMedia localMedia,
                                               LocalVideoTrack localVideoTrack,
                                               TwilioConversationsException e) {
                Log.e(TAG, "LocalVideoTrackError: " + e.getMessage());
            }
        };
    }

    /*
     * Participant listener
     */
    public Participant.Listener participantListener() {
        return new Participant.Listener() {
            @Override
            public void onVideoTrackAdded(Conversation conversation,
                                          Participant participant,
                                          VideoTrack videoTrack) {
                Log.i(TAG, "onVideoTrackAdded " + participant.getIdentity());
                conversationStatusTextView.setText("onVideoTrackAdded " +
                        participant.getIdentity());

                // Remote participant
                participantVideoRenderer = new VideoViewRenderer(AudioCallActivity.this,
                        participantContainer);

                // Scale the remote video to fill the view group
                participantVideoRenderer.setVideoScaleType(VideoScaleType.ASPECT_FILL);

                participantVideoRenderer.setObserver(new VideoRenderer.Observer() {

                    @Override
                    public void onFirstFrame() {
                        Log.i(TAG, "Participant onFirstFrame");
                    }

                    @Override
                    public void onFrameDimensionsChanged(int width, int height, int rotation) {
                        Log.i(TAG, "Participant onFrameDimensionsChanged " + width + " " +
                                height + " " + rotation);
                    }

                });
                videoTrack.addRenderer(participantVideoRenderer);

            }

            @Override
            public void onVideoTrackRemoved(Conversation conversation,
                                            Participant participant,
                                            VideoTrack videoTrack) {
                Log.i(TAG, "onVideoTrackRemoved " + participant.getIdentity());
                conversationStatusTextView.setText("onVideoTrackRemoved " +
                        participant.getIdentity());
                participantContainer.removeAllViews();
                if (participantVideoRenderer != null) {
                    participantVideoRenderer.release();
                }

            }

            @Override
            public void onAudioTrackAdded(Conversation conversation,
                                          Participant participant,
                                          AudioTrack audioTrack) {
                Log.i(TAG, "onAudioTrackAdded " + participant.getIdentity());
            }

            @Override
            public void onAudioTrackRemoved(Conversation conversation,
                                            Participant participant,
                                            AudioTrack audioTrack) {
                Log.i(TAG, "onAudioTrackRemoved " + participant.getIdentity());
            }

            @Override
            public void onTrackEnabled(Conversation conversation,
                                       Participant participant,
                                       MediaTrack mediaTrack) {
                Log.i(TAG, "onTrackEnabled " + participant.getIdentity());
            }

            @Override
            public void onTrackDisabled(Conversation conversation,
                                        Participant participant,
                                        MediaTrack mediaTrack) {
                Log.i(TAG, "onTrackDisabled " + participant.getIdentity());
            }
        };
    }

    /*
     * ConversationsClient listener
     */
    private TwilioConversationsClient.Listener conversationsClientListener() {
        return new TwilioConversationsClient.Listener() {
            @Override
            public void onStartListeningForInvites(TwilioConversationsClient conversationsClient) {
                conversationStatusTextView.setText("onStartListeningForInvites");

                if (incomingCall && !TextUtils.isEmpty(callId)) {
                    Toast.makeText(AudioCallActivity.this, "answering....", Toast.LENGTH_SHORT).show();
                    progress.setMessage("Answering...");
                    sendInvite();
                    answered = true;
                } else {
                    if (autoCall) {
                        initiateCall();
                    }else{
                        Log.i("AudioCallActivity ", "audio call is coming as false ...");
                    }
                }
            }

            @Override
            public void onStopListeningForInvites(TwilioConversationsClient conversationsClient) {
                conversationStatusTextView.setText("onStopListeningForInvites");
                // If we are logging out let us finish the teardown process
                Log.i(TAG, "@@@onStopListeningForInvites");
                if (loggingOut) {
                    completeLogout();
                    return;
                }
                hideProgress();
                if (conversation == null || !conversation.isActive()) {
                    hangup(false);
                    reset();

                }
            }

            @Override
            public void onFailedToStartListening(TwilioConversationsClient conversationsClient,
                                                 TwilioConversationsException e) {

                Log.e(TAG, "::onFailedToStartListening::", e);
                hideProgress();
            }

            @Override
            public void onIncomingInvite(TwilioConversationsClient conversationsClient,
                                         IncomingInvite incomingInvite) {
                Log.i(TAG, "Receiving call invite.");
                conversationStatusTextView.setText("onIncomingInvite");
                if (conversation == null) {
                    LocalMedia localMedia = setupLocalMedia();
                    setAudioFocus(true);

                    incomingInvite.accept(localMedia, new ConversationCallback() {
                        @Override
                        public void onConversation(Conversation conversation, TwilioConversationsException e) {
                            Log.e(TAG, "Accepted conversation invite");
                            if (e == null) {
                                hideProgress();
                                AudioCallActivity.this.conversation = conversation;
                                conversation.setConversationListener(conversationListener());
                            } else {
                                Log.e(TAG, e.getMessage(), e);
                                hangup(false);
                                reset();
                            }
                        }
                    });
                    setHangupAction();
                } else {
                    Log.w(TAG, String.format("Conversation in progress. Invite from %s ignored",
                            incomingInvite.getInviter()));
                }
            }

            @Override
            public void onIncomingInviteCancelled(TwilioConversationsClient conversationsClient,
                                                  IncomingInvite incomingInvite) {
                conversationStatusTextView.setText("onIncomingInviteCancelled");
                hideProgress();
                hangup(false);
                reset();
                Snackbar.make(conversationStatusTextView, "Invite from " +
                        incomingInvite.getInviter() + " terminated", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        };
    }

    protected void hideProgress() {
        try {
            Log.i(TAG, "Hiding progres dialog.");
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * CameraCapture error listener
     */
    public CapturerErrorListener capturerErrorListener() {
        return new CapturerErrorListener() {
            @Override
            public void onError(CapturerException e) {
                Log.e(TAG, "Camera capturer error: " + e.getMessage());
            }
        };
    }

    /*
     * AccessManager listener
     */
    protected AccessManager.Listener accessManagerListener() {
        final Context context = this;
        return new AccessManager.Listener() {
            @Override
            public void onTokenExpired(AccessManager twilioAccessManager) {
                conversationStatusTextView.setText("onAccessManagerTokenExpire");
                Log.e(TAG, "token Expired....");
                AsyncTask asyncTask = new MakeAsyncRequest(AudioCallActivity.this, context);
                asyncTask.execute(new String[]{"https://192.168.0.113:/token", "GET"});
            }

            @Override
            public void onTokenUpdated(AccessManager twilioAccessManager) {
                conversationStatusTextView.setText("onTokenUpdated");
                Log.e(TAG, "token updated....");

            }

            @Override
            public void onError(AccessManager twilioAccessManager, String s) {
                conversationStatusTextView.setText("onError");
                Log.e(TAG, "token error....");

            }
        };
    }

    /*
     * Helper methods
     */
    public LocalMedia setupLocalMedia() {
        LocalMedia localMedia = new LocalMedia(localMediaListener());
        LocalVideoTrack localVideoTrack = new LocalVideoTrack(getCameraCapturer());
        localVideoTrack.enable(false);
        localMedia.addLocalVideoTrack(localVideoTrack);
        if (muteMicrophone) {
            localMedia.mute(true);
        }
        return localMedia;
    }

    protected boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestPermissionForCameraAndMicrophone() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                CAMERA_MIC_PERMISSION_REQUEST_CODE);
    }

    protected void retrieveAccessTokenfromServer(Token token) {
        this.token = token;
        initialiseClient();
        startPreview();
    }

    protected void initialiseClient() {
        if (!TwilioConversationsClient.isInitialized()) {
            TwilioConversationsClient.initialize(getApplicationContext());
        }
        if (token == null) {
            Log.e(TAG, "Token is coming as null, please check");
            return;
        }
        accessManager = new AccessManager(AudioCallActivity.this,
                token.getToken(),
                accessManagerListener());
        conversationsClient =
                TwilioConversationsClient
                        .create(accessManager,
                                conversationsClientListener());
        // Specify the audio output to use for this conversation client
        conversationsClient.setAudioOutput(AudioOutput.HEADSET);
        // Initialize the camera capturer and start the camera preview
        cameraCapturer = CameraCapturer.create(this,
                CameraCapturer.CameraSource.CAMERA_SOURCE_FRONT_CAMERA,
                capturerErrorListener());
        conversationsClient.listen();
    }

    public CameraCapturer getCameraCapturer() {
        if (cameraCapturer == null) {
            cameraCapturer = CameraCapturer.create(this,
                    CameraCapturer.CameraSource.CAMERA_SOURCE_FRONT_CAMERA,
                    capturerErrorListener());
        }
        return cameraCapturer;
    }

    protected void setAudioFocus(boolean setFocus) {
        if (audioManager != null) {
            if (setFocus) {
                savedAudioMode = audioManager.getMode();
                // Request audio focus before making any device switch.
                audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

                // Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
                // required to be in this mode when playout and/or recording starts for
                // best possible VoIP performance.
                // Some devices have difficulties with speaker mode if this is not set.
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            } else {
                audioManager.setMode(savedAudioMode);
                audioManager.abandonAudioFocus(null);
            }
        }
    }

    public void scheduleStopRinging(final String callIdScheduled) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!incomingCall && inviteSent && (callId != null && callId.equals(callIdScheduled))) {
                            inviteSent = false;
                            videoCallNotificationHelper.sendVideoCallCanceledNotification(contactToCall, callId);
                            videoCallNotificationHelper.sendVideoCallCanceled(contactToCall, callId);
                            Toast.makeText(context, "No answer..", Toast.LENGTH_LONG).show();
                            hideProgress();
                            reset();
                            hangup(true);
                        }
                    }
                }, VideoCallNotificationHelper.MAX_NOTIFICATION_RING_DURATION + 10 * 1000);
            }
        });
    }

    @Override
    public void onNetworkComplete(String response) {
        Log.i(TAG, "Token response: " + response);
        if (TextUtils.isEmpty(response)) {
            Log.i(TAG, "Not able to get token");
            return;
        }

        Token token = (Token) GsonUtils.getObjectFromJson(response, Token.class);
        MobiComUserPreference.getInstance(this).setVideoCallToken(token.getToken());
        retrieveAccessTokenfromServer(token);
    }

    public void registerForNotificationBroadcast() {

        applozicBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String incomingCallId = intent.getStringExtra(VideoCallNotificationHelper.CALL_ID);
                boolean isNotificationForSameId = false;

                Log.i(TAG, "incomingCallId: " + incomingCallId + ", intent.getAction(): " + intent.getAction());

                if (!TextUtils.isEmpty(callId)) {
                    isNotificationForSameId = (callId.equals(incomingCallId));
                }

                if (MobiComKitConstants.APPLOZIC_VIDEO_CALL_ANSWER.equals(intent.getAction()) && isNotificationForSameId) {
                    answered = true;
                    sendInvite();
                } else if ((MobiComKitConstants.APPLOZIC_VIDEO_CALL_REJECTED.equals(intent.getAction()) ||
                        VideoCallNotificationHelper.CALL_CANCELED.equals(intent.getAction()) ||
                        VideoCallNotificationHelper.CALL_MISSED.equals(intent.getAction()) || VideoCallNotificationHelper.CALL_END.equals(intent.getAction()))
                        && isNotificationForSameId) {
                    if (outgoingInvite != null &&
                            outgoingInvite.getStatus() == InviteStatus.PENDING) {
                        outgoingInvite.cancel();
                    }

                    Toast.makeText(context, "Participant is busy..", Toast.LENGTH_LONG).show();
                    hideProgress();
                    reset();
                    hangup(true);
                } else if (MobiComKitConstants.APPLOZIC_VIDEO_DIALED.equals(intent.getAction())) {
                    String contactId = intent.getStringExtra("CONTACT_ID");
                    if (conversation != null && conversation.isActive()) {
                        Contact contact = contactService.getContactById(contactId);
                        videoCallNotificationHelper.sendVideoCallReject(contact, incomingCallId);
                        return;
                    }
                    callId = incomingCallId;
                    handleIncomingInvite(contactId);
                }
            }
        };
    }

    protected void handleIncomingInvite(String contactId) {
        Contact contact = contactService.getContactById(contactId);
        alertDialog = Dialog.createInviteDialog(contact.getDisplayName(), acceptCallClickListner(contact), rejectCallClickListener(contact), this);
        alertDialog.show();
    }

    protected void sendInvite() {
        stopPreview();
        // Create participants set (we support only one in this example)
        Set<String> participants = new HashSet<>();

        participants.add(contactToCall.getContactIds());
        // Create local media
        LocalMedia localMedia = setupLocalMedia();
        setAudioFocus(true);
        // Create outgoing invite
        outgoingInvite = conversationsClient.inviteToConversation(participants,
                localMedia, new ConversationCallback() {
                    @Override
                    public void onConversation(Conversation conversation,
                                               TwilioConversationsException e) {
                        if (e == null) {
                            // Participant has accepted invite, we are in active conversation
                            AudioCallActivity.this.conversation = conversation;
                            conversation.setConversationListener(conversationListener());
                            inviteSent = false;
                        } else {
                            Log.e(TAG, "got exception..." + e.getMessage());
                            Toast.makeText(getApplicationContext(), "Not able to connect. Connection Error.." , Toast.LENGTH_LONG).show();
                            if (!loggingOut) {
                                hangup(true);
                            } else {
                                logout("onConversation");
                            }
                        }
                        hideProgress();
                    }
                });
        setHangupAction();
    }

    @Override
    public void onBackPressed() {
        if (conversation != null && conversation.isActive()) {
            alertDialog = Dialog.createCloseSessionDialog(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i(TAG, "onBackPressed cancel do nothing.. ");
                }
            }, closeSessionListener(), this);
            alertDialog.show();
        } else if(conversationsClient!=null && inviteSent ) {
            videoCallNotificationHelper.sendVideoCallCanceledNotification(contactToCall, callId);
            videoCallNotificationHelper.sendVideoCallCanceled(contactToCall, callId);
            hangup(true);
        }
        else {
            super.onBackPressed();

        }

    }

    static IntentFilter BrodCastIntentFilters() {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(MobiComKitConstants.APPLOZIC_VIDEO_CALL_ANSWER);
        intentFilter.addAction(MobiComKitConstants.APPLOZIC_VIDEO_CALL_REJECTED);
        intentFilter.addAction(VideoCallNotificationHelper.CALL_CANCELED);
        intentFilter.addAction(VideoCallNotificationHelper.CALL_END);
        intentFilter.addAction(MobiComKitConstants.APPLOZIC_VIDEO_DIALED);
        intentFilter.addAction(VideoCallNotificationHelper.CALL_MISSED);

        return intentFilter;
    }


}
