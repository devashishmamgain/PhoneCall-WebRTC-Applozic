package com.applozic.audiovideo.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.twilio.conversations.AudioOutput;
import com.twilio.conversations.AudioTrack;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationCallback;
import com.twilio.conversations.IncomingInvite;
import com.twilio.conversations.LocalMedia;
import com.twilio.conversations.LocalVideoTrack;
import com.twilio.conversations.MediaTrack;
import com.twilio.conversations.Participant;
import com.twilio.conversations.TwilioConversationsClient;
import com.twilio.conversations.TwilioConversationsException;
import com.twilio.conversations.VideoRenderer;
import com.twilio.conversations.VideoScaleType;
import com.twilio.conversations.VideoTrack;
import com.twilio.conversations.VideoViewRenderer;

import applozic.com.audiovideo.R;

public class VideoActivity extends AudioCallActivity {
    private static final String TAG = VideoActivity.class.getName();

    private FloatingActionButton switchCameraActionFab;
    private FloatingActionButton localVideoActionFab;

    public VideoActivity() {
        super(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_conversation);
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
        contactName = (TextView) findViewById(R.id.contact_name);
        contactName.setText(contactToCall.getDisplayName());
        conversationStatusTextView = (TextView) findViewById(R.id.conversation_status_textview);
        if (!debugMode) {
            conversationStatusTextView.setVisibility(View.GONE);
        }
        callActionFab = (FloatingActionButton) findViewById(R.id.call_action_fab);
        switchCameraActionFab = (FloatingActionButton) findViewById(R.id.switch_camera_action_fab);
        localVideoActionFab = (FloatingActionButton) findViewById(R.id.local_video_action_fab);
        muteActionFab = (FloatingActionButton) findViewById(R.id.mute_action_fab);
        speakerActionFab = (FloatingActionButton) findViewById(R.id.speaker_action_fab);

        initialize();
    }

    @Override
    public void initiateCall() {
        setHangupAction();
        callId = videoCallNotificationHelper.sendVideoCallRequest(contactToCall);
        scheduleStopRinging(callId);
        inviteSent = true;
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

    /*
     * The initial state when there is no active conversation.
     */
    @Override
    protected void setCallAction() {
        super.setCallAction();
        switchCameraActionFab.show();
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.show();
        localVideoActionFab.setOnClickListener(localVideoClickListener());
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
                ContextCompat.getDrawable(VideoActivity.this,
                        R.drawable.ic_mic_green_24px));

        pauseVideo = false;
        localVideoActionFab.setImageDrawable(
                ContextCompat.getDrawable(VideoActivity.this,
                        R.drawable.ic_videocam_green_24px));
        speakerActionFab.setImageDrawable(
                ContextCompat.getDrawable(VideoActivity.this,
                        R.drawable.ic_volume_down_green_24px));
        setSpeakerphoneOn(true);

        setCallAction();
        if (!wasPreviewing) {
            startPreview();
        }
    }

    private View.OnClickListener switchCameraClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraCapturer != null) {
                    cameraCapturer.switchCamera();
                }
            }
        };
    }

    private View.OnClickListener localVideoClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update pause video if it succeeds
                pauseVideo = pauseVideo(!pauseVideo) ? !pauseVideo : pauseVideo;

                if (pauseVideo) {
                    switchCameraActionFab.hide();
                    localVideoActionFab.setImageDrawable(
                            ContextCompat.getDrawable(VideoActivity.this,
                                    R.drawable.ic_videocam_off_red_24px));
                } else {
                    switchCameraActionFab.show();
                    localVideoActionFab.setImageDrawable(
                            ContextCompat.getDrawable(VideoActivity.this,
                                    R.drawable.ic_videocam_green_24px));
                }
            }
        };
    }

//    @NonNull
//    private DialogInterface.OnClickListener getStopCallingListener() {
//        return new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//                hangup();
//                reset();
//            }
//        };
//    }

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
                }
                if (!loggingOut) {
                    reset();
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
                localVideoRenderer = new VideoViewRenderer(VideoActivity.this,
                        localContainer);
                localVideoRenderer.applyZOrder(true);
                localVideoTrack.addRenderer(localVideoRenderer);
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
                participantVideoRenderer = new VideoViewRenderer(VideoActivity.this,
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
                    Toast.makeText(VideoActivity.this, "answering....", Toast.LENGTH_SHORT).show();
                    progress.setMessage("Answering...");
                    sendInvite();
                    answered = true;
                } else {
                    if (autoCall) {
                        initiateCall();
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
                                VideoActivity.this.conversation = conversation;
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

    @Override
    protected void initialiseClient() {
        super.initialiseClient();
        // Specify the audio output to use for this conversation client
        conversationsClient.setAudioOutput(AudioOutput.SPEAKERPHONE);
    }

    public LocalMedia setupLocalMedia() {
        LocalMedia localMedia = new LocalMedia(localMediaListener());
        LocalVideoTrack localVideoTrack = new LocalVideoTrack(getCameraCapturer());
        if ( pauseVideo) {
            localVideoTrack.enable(false);
        }
        localMedia.addLocalVideoTrack(localVideoTrack);
        if (muteMicrophone) {
            localMedia.mute(true);
        }
        return localMedia;
    }

}
