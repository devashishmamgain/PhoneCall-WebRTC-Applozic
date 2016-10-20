package com.applozic.mobicomkit.uiwidgets.conversation.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.attachment.FileClientService;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.MessageIntentService;
import com.applozic.mobicomkit.api.conversation.MobiComConversationService;
import com.applozic.mobicomkit.api.conversation.MobiComMessageService;
import com.applozic.mobicomkit.api.conversation.SyncCallService;
import com.applozic.mobicomkit.api.conversation.service.ConversationService;
import com.applozic.mobicomkit.broadcast.ApplozicForegroundListenerService;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.MessageCommunicator;
import com.applozic.mobicomkit.uiwidgets.conversation.MobiComKitBroadcastReceiver;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.AudioMessageFragment;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.ConversationFragment;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.MobiComQuickConversationFragment;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.MultimediaOptionFragment;
import com.applozic.mobicomkit.uiwidgets.instruction.ApplozicPermissions;
import com.applozic.mobicomkit.InstructionUtil;
import com.applozic.mobicommons.commons.core.utils.PermissionsUtils;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.channel.Conversation;
import com.applozic.mobicommons.people.contact.Contact;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.rockerhieu.emojicon.EmojiconEditText;
import com.rockerhieu.emojicon.EmojiconGridFragment.OnEmojiconClickedListener;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by devashish on 6/25/2015.
 */
public class ConversationActivity extends AppCompatActivity implements MessageCommunicator, MobiComKitActivityInterface, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ActivityCompat.OnRequestPermissionsResultCallback, EmojiconsFragment.OnEmojiconBackspaceClickedListener, OnEmojiconClickedListener {

    public static final int LOCATION_SERVICE_ENABLE = 1001;
    public static final String TAKE_ORDER = "takeOrder";
    public static final String CONTACT = "contact";
    public static final String CHANNEL = "channel";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String GOOGLE_API_KEY_META_DATA = "com.google.android.geo.API_KEY";
    public static final String ACTIVITY_TO_OPEN_ONCLICK_OF_CALL_BUTTON_META_DATA = "activity.open.on.call.button.click";
    protected static final long UPDATE_INTERVAL = 500;
    protected static final long FASTEST_INTERVAL = 1;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final String API_KYE_STRING = "YOUR_GEO_API_KEY";
    private static final String CAPTURED_IMAGE_URI = "capturedImageUri";
    private static final String CAPTURED_VIDEO_URI = "capturedVideoUri";

    private static final String SHARE_TEXT = "share_text";
    private static Uri capturedImageUri;
    private static String inviteMessage;
    protected ConversationFragment conversation;
    protected MobiComQuickConversationFragment quickConversationFragment;
    protected MobiComKitBroadcastReceiver mobiComKitBroadcastReceiver;
    private MobiComMessageService messageService;
    protected ActionBar mActionBar;
    protected GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Contact contact;
    private Channel channel;
    private static int retry;
    public LinearLayout layout;
    String geoApiKey;
    String activityToOpenOnClickOfCallButton;
    int resourceId;
    RelativeLayout childFragmentLayout;
    boolean isTakePhoto;
    boolean isAttachment;
    private BaseContactService baseContactService;
    private ApplozicPermissions applozicPermission;
    private ApplozicSetting applozicSetting;
    Integer currentConversationId;
    private Uri videoFileUri;
    public EmojiconEditText mEditEmojicon;
    ConversationUIService conversationUIService;

    public ConversationActivity() {

    }

    public Snackbar snackbar;

    @Override
    public void showErrorMessageView(String message) {
        layout.setVisibility(View.VISIBLE);
        snackbar = Snackbar.make(layout, message, Snackbar.LENGTH_LONG);
        snackbar.setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.setDuration(Snackbar.LENGTH_LONG);
        ViewGroup group = (ViewGroup) snackbar.getView();
        TextView textView = (TextView) group.findViewById(R.id.snackbar_action);
        textView.setTextColor(Color.YELLOW);
        group.setBackgroundColor(getResources().getColor(R.color.error_background_color));
        TextView txtView = (TextView) group.findViewById(R.id.snackbar_text);
        txtView.setMaxLines(5);
        snackbar.show();
    }

    @Override
    public void retry() {
        retry++;
    }

    @Override
    public int getRetryCount() {
        return retry;
    }

    public void dismissErrorMessage() {
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    public static void addFragment(FragmentActivity fragmentActivity, Fragment fragmentToAdd, String fragmentTag) {
        FragmentManager supportFragmentManager = fragmentActivity.getSupportFragmentManager();

        // Fragment activeFragment = UIService.getActiveFragment(fragmentActivity);
        FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction();
        fragmentTransaction.replace(R.id.layout_child_activity, fragmentToAdd,
                fragmentTag);

        if (supportFragmentManager.getBackStackEntryCount() > 1
                && !ConversationUIService.MESSGAE_INFO_FRAGMENT.equalsIgnoreCase(fragmentTag) &&  !ConversationUIService.USER_PROFILE_FRAMENT.equalsIgnoreCase(fragmentTag)){
            supportFragmentManager.popBackStack();
        }
        fragmentTransaction.addToBackStack(fragmentTag);
      /*if (activeFragment != null) {
            fragmentTransaction.hide(activeFragment);
        }*/
        fragmentTransaction.commitAllowingStateLoss();
        supportFragmentManager.executePendingTransactions();
        //Log.i(TAG, "BackStackEntryCount: " + supportFragmentManager.getBackStackEntryCount());
    }

    @Override
    protected void onStop() {
        super.onStop();
       /* final String userKeyString = MobiComUserPreference.getInstance(this).getSuUserKeyString();
        Intent intent = new Intent(this, ApplozicMqttIntentService.class);
        intent.putExtra(ApplozicMqttIntentService.USER_KEY_STRING, userKeyString);
        startService(intent);*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mobiComKitBroadcastReceiver, BroadcastService.getIntentFilter());
       /* Intent subscribeIntent = new Intent(this, ApplozicMqttIntentService.class);
        subscribeIntent.putExtra(ApplozicMqttIntentService.SUBSCRIBE, true);
        startService(subscribeIntent);*/

        if (!Utils.isInternetAvailable(this)) {
            String errorMessage = getResources().getString(R.string.internet_connection_not_available);
            showErrorMessageView(errorMessage);
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mobiComKitBroadcastReceiver);
        //ApplozicMqttService.getInstance(this).unSubscribe();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(CONTACT, contact);
        savedInstanceState.putSerializable(CHANNEL, channel);
        savedInstanceState.putSerializable(CONVERSATION_ID, currentConversationId);

        if (capturedImageUri != null) {
            savedInstanceState.putString(CAPTURED_IMAGE_URI, capturedImageUri.toString());
        }
        if (videoFileUri != null) {
            savedInstanceState.putString(CAPTURED_VIDEO_URI, videoFileUri.toString());
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Boolean takeOrder = getIntent().getBooleanExtra(TAKE_ORDER, false);
        if (takeOrder) {
            this.finish();
        }
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            Utils.toggleSoftKeyBoard(this, true);
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseContactService =  new AppContactService(this);
        applozicSetting = ApplozicSetting.getInstance(this);
        messageService = new MobiComMessageService(this, MessageIntentService.class);
        conversationUIService =  new ConversationUIService(this);

        setContentView(R.layout.quickconversion_activity);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Intent chatService = new Intent(this, ApplozicForegroundListenerService.class);
        startService(chatService);
        geoApiKey = Utils.getMetaDataValue(this, GOOGLE_API_KEY_META_DATA);
        activityToOpenOnClickOfCallButton = Utils.getMetaDataValue(this, ACTIVITY_TO_OPEN_ONCLICK_OF_CALL_BUTTON_META_DATA);
        layout = (LinearLayout) findViewById(R.id.footerAd);
        applozicPermission = new ApplozicPermissions(this, layout);
        childFragmentLayout = (RelativeLayout) findViewById(R.id.layout_child_activity);

        if (Utils.hasMarshmallow()) {
            applozicPermission.checkRuntimePermissionForStorage();
        }
        mActionBar = getSupportActionBar();
        inviteMessage = Utils.getMetaDataValue(getApplicationContext(), SHARE_TEXT);
        retry = 0;
        if (savedInstanceState != null) {
            capturedImageUri = savedInstanceState.getString(CAPTURED_IMAGE_URI) != null ?
                    Uri.parse(savedInstanceState.getString(CAPTURED_IMAGE_URI)) : null;
            videoFileUri = savedInstanceState.getString(CAPTURED_VIDEO_URI) != null ?
                    Uri.parse(savedInstanceState.getString(CAPTURED_VIDEO_URI)) : null;

            contact = (Contact) savedInstanceState.getSerializable(CONTACT);
            channel = (Channel) savedInstanceState.getSerializable(CHANNEL);
            currentConversationId = savedInstanceState.getInt(CONVERSATION_ID);
            if (channel != null) {
                conversation = new ConversationFragment(null, channel,currentConversationId);
            } else {
                conversation = new ConversationFragment(contact, null,currentConversationId);
            }
            addFragment(this, conversation, ConversationUIService.CONVERSATION_FRAGMENT);
        } /*else {
            quickConversationFragment = new MobiComQuickConversationFragment();
            addFragment(this, quickConversationFragment, ConversationUIService.QUICK_CONVERSATION_FRAGMENT);
        }*/

        mobiComKitBroadcastReceiver = new MobiComKitBroadcastReceiver(this);
        InstructionUtil.showInfo(this, R.string.info_message_sync, BroadcastService.INTENT_ACTIONS.INSTRUCTION.toString());

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) finish();
            }
        });
        mActionBar.setTitle(R.string.conversations);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        onNewIntent(getIntent());
        Boolean takeOrder = getIntent().getBooleanExtra(TAKE_ORDER, false);

        if(!takeOrder){
            new MobiComConversationService(this).processLastSeenAtStatus();
            //UserService.getInstance(this).processSyncUserBlock();
        }
        if (ApplozicClient.getInstance(this).isAccountClosed() || ApplozicClient.getInstance(this).isNotAllowed()) {
            snackbar = Snackbar.make(layout, ApplozicClient.getInstance(this).isAccountClosed() ?
                            R.string.applozic_account_closed : R.string.applozic_free_version_not_allowed_on_release_build,
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
            SyncCallService.getInstance(this).checkAccountStatus();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //setIntent(intent);
        if (!MobiComUserPreference.getInstance(this).isLoggedIn()) {
            //user is not logged in
            Log.i("AL", "user is not logged in yet.");
            return;
        }

        try {
           if(intent.getExtras() != null){
               BroadcastService.setContextBasedChat(intent.getExtras().getBoolean(ConversationUIService.CONTEXT_BASED_CHAT));
            }
            conversationUIService.checkForStartNewConversation(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showActionBar() {
        mActionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        showActionBar();
        //return false;
        getMenuInflater().inflate(R.menu.mobicom_basic_menu_for_normal_message, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        conversationUIService.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SERVICE_ENABLE) {
            if (((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                googleApiClient.connect();
            } else {
                Toast.makeText(ConversationActivity.this, R.string.unable_to_fetch_location, Toast.LENGTH_LONG).show();
            }
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionsUtils.REQUEST_STORAGE) {
            if (PermissionsUtils.verifyPermissions(grantResults)) {
                showSnackBar(R.string.storage_permission_granted);
                if(isAttachment){
                    isAttachment = false;
                    processAttachment();
                }
            } else {
                showSnackBar(R.string.storage_permission_not_granted);
            }

        } else if (requestCode == PermissionsUtils.REQUEST_LOCATION) {
            if (PermissionsUtils.verifyPermissions(grantResults)) {
                showSnackBar(R.string.location_permission_granted);
                processingLocation();
            } else {
                showSnackBar(R.string.location_permission_not_granted);
            }

        } else if (requestCode == PermissionsUtils.REQUEST_PHONE_STATE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.phone_state_permission_granted);
            } else {
                showSnackBar(R.string.phone_state_permission_not_granted);
            }
        }
        else if (requestCode == PermissionsUtils.REQUEST_CALL_PHONE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.phone_call_permission_granted);
                processCall(contact,currentConversationId);
            } else {
                showSnackBar(R.string.phone_call_permission_not_granted);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_AUDIO_RECORD) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.record_audio_permission_granted);
                showAudioRecordingDialog();
            } else {
                showSnackBar(R.string.record_audio_permission_not_granted);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.phone_camera_permission_granted);
                if (isTakePhoto) {
                    processCameraAction();
                } else {
                    processVideoRecording();
                }
            } else {
                showSnackBar(R.string.phone_camera_permission_not_granted);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CONTACT) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.contact_permission_granted);
                processContact();
            } else {
                showSnackBar(R.string.contact_permission_not_granted);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public void processingLocation() {
        if (ApplozicSetting.getInstance(this).isLocationSharingViaMap() && !TextUtils.isEmpty(geoApiKey) && !API_KYE_STRING.equals(geoApiKey)) {
            Intent toMapActivity = new Intent(this, MobicomLocationActivity.class);
            startActivityForResult(toMapActivity, MultimediaOptionFragment.REQUEST_CODE_SEND_LOCATION);
            Log.i("test", "Activity for result strarted");

        } else {
            //================= START GETTING LOCATION WITHOUT LOADING MAP AND SEND LOCATION AS TEXT===============

            if (!((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.location_services_disabled_title)
                        .setMessage(R.string.location_services_disabled_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.location_service_settings, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(intent, LOCATION_SERVICE_ENABLE);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                Toast.makeText(ConversationActivity.this, R.string.location_sending_cancelled, Toast.LENGTH_LONG).show();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                googleApiClient.disconnect();
                googleApiClient.connect();
            }

            //=================  END ===============

        }

    }

    public void processLocation() {
        if (Utils.hasMarshmallow()) {
            new ApplozicPermissions(ConversationActivity.this, layout).checkRuntimePermissionForLocation();
        } else {
            processingLocation();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.start_new) {
            conversationUIService.startContactActivityForResult();
        } else if (id == R.id.conversations) {
            Intent intent = new Intent(this, ChannelCreateActivity.class);
            startActivity(intent);
        } else if (id == R.id.refresh) {
            messageService.syncMessagesWithServer(this.getString(R.string.info_message_sync));
        } else if (id == R.id.shareOptions) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setAction(Intent.ACTION_SEND)
                    .setType("text/plain").putExtra(Intent.EXTRA_TEXT, inviteMessage);
            startActivity(Intent.createChooser(intent, "Share Via"));
            return super.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public void onQuickConversationFragmentItemClick(View view, Contact contact, Channel channel,Integer conversationId) {
        conversation = new ConversationFragment(contact, channel,conversationId);
        addFragment(this, conversation, ConversationUIService.CONVERSATION_FRAGMENT);
        this.channel = channel;
        this.contact = contact;
        this.currentConversationId = conversationId;
    }

    @Override
    public void startContactActivityForResult() {
        conversationUIService.startContactActivityForResult();
    }

    @Override
    public void addFragment(ConversationFragment conversationFragment) {
        addFragment(this, conversationFragment, ConversationUIService.CONVERSATION_FRAGMENT);
        conversation = conversationFragment;
    }

    @Override
    public void onBackPressed() {
        Boolean takeOrder = getIntent().getBooleanExtra(TAKE_ORDER, false);
        ConversationFragment conversationFragment = (ConversationFragment) getSupportFragmentManager().findFragmentByTag(ConversationUIService.CONVERSATION_FRAGMENT);

        if (conversationFragment != null && conversationFragment.isVisible() && conversationFragment.isAttachmentOptionsOpen() ) {
            conversationFragment.handleAttachmentToggle();
            return;
        }
        if (takeOrder){
            this.finish();
        }else {
            super.onBackPressed();
        }

    }


    @Override
    public void onEmojiconBackspaceClicked(View v) {
         EmojiconsFragment.backspace(mEditEmojicon);
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(mEditEmojicon, emojicon);
    }

    @Override
    public void updateLatestMessage(Message message, String formattedContactNumber) {
        conversationUIService.updateLatestMessage(message, formattedContactNumber);

    }

    @Override
    public void removeConversation(Message message, String formattedContactNumber) {
        conversationUIService.removeConversation(message, formattedContactNumber);
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (mCurrentLocation == null) {
                Toast.makeText(this, R.string.waiting_for_current_location, Toast.LENGTH_SHORT).show();
                locationRequest = new LocationRequest();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(UPDATE_INTERVAL);
                locationRequest.setFastestInterval(FASTEST_INTERVAL);
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            }
            if (mCurrentLocation != null && conversation != null) {
                conversation.attachLocation(mCurrentLocation);
            }
        } catch (Exception e) {
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(((Object) this).getClass().getSimpleName(),
                "onConnectionSuspended() called.");

    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            if (conversation != null && location != null) {
                conversation.attachLocation(location);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            showErrorDialog(connectionResult.getErrorCode());
        }

    }

    public void setChildFragmentLayoutBG(){

        childFragmentLayout.setBackgroundResource(R.color.conversation_list_all_background);
    }
    public void setChildFragmentLayoutBGToTransparent(){

        childFragmentLayout.setBackgroundResource(android.R.color.transparent);
    }

    void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, this,
                CONNECTION_FAILURE_RESOLUTION_REQUEST).show();
    }

    public Contact getContact() {
        return contact;
    }

    public Channel getChannel() {
        return channel;
    }

    public Integer getConversationId() {
        return currentConversationId;
    }

    public static Uri getCapturedImageUri() {
        return capturedImageUri;
    }

    public static void setCapturedImageUri(Uri capturedImageUri) {
        ConversationActivity.capturedImageUri = capturedImageUri;
    }

    public void showSnackBar(int resId) {
        snackbar = Snackbar.make(layout, resId,
                Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    public Uri getVideoFileUri() {
        return videoFileUri;
    }

    public void setVideoFileUri(Uri videoFileUri) {
        this.videoFileUri = videoFileUri;
    }

    public void isTakePhoto(boolean takePhoto) {
        this.isTakePhoto = takePhoto;
    }

    public void isAttachment(boolean attachment) {
        this.isAttachment = attachment;
    }


    public void showAudioRecordingDialog() {

            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfPermissionForAudioRecording(this)) {
                new ApplozicPermissions(this, layout).requestAudio();
            } else if(PermissionsUtils.isAudioRecordingPermissionGranted(this)) {

                FragmentManager supportFragmentManager = getSupportFragmentManager();
                DialogFragment fragment = AudioMessageFragment.newInstance();

                FragmentTransaction fragmentTransaction = supportFragmentManager
                        .beginTransaction().add(fragment, "dialog");

                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commitAllowingStateLoss();

            }else{

                if(ApplozicSetting.getInstance(this).getTextForAudioPermissionNotFound()==null){
                    showSnackBar(R.string.applozic_audio_permission_missing);
                }else{
                    snackbar = Snackbar.make(layout,ApplozicSetting.getInstance(this).getTextForAudioPermissionNotFound(),
                            Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }

            }
    }


    public void processCall(Contact contactObj, Integer conversationId) {
        this.contact = baseContactService.getContactById(contactObj.getContactIds());
        this.currentConversationId = conversationId;
        try {

            if(ApplozicClient.getInstance(getApplicationContext()).isIPCallEnabled()){
                //Audio Call
                Class activityToOpen =  Class.forName("com.applozic.audiovideo.activity.AudioCallActivity");
                Intent intent = new Intent(this, activityToOpen);
                intent.putExtra("CONTACT_ID", contact.getUserId());
                startActivity(intent);
                return;
            }
            if (activityToOpenOnClickOfCallButton != null) {
                Intent callIntent = new Intent(this, Class.forName(activityToOpenOnClickOfCallButton));
                if (currentConversationId != null) {
                    Conversation conversation = ConversationService.getInstance(this).getConversationByConversationId(currentConversationId);
                    callIntent.putExtra(ConversationUIService.TOPIC_ID, conversation.getTopicId());
                }
                callIntent.putExtra(ConversationUIService.CONTACT, contact);
                startActivity(callIntent);
            } else if (applozicSetting.isActionDialWithoutCallingEnabled()){
                if(!TextUtils.isEmpty(contact.getContactNumber())) {
                    Intent callIntent;
                    String uri = "tel:" + contact.getContactNumber().trim();
                    callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse(uri));
                    startActivity(callIntent);
                }
            }else {
                if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCallPermission(this)) {
                    applozicPermission.requestCallPermission();
                } else if(PermissionsUtils.isCallPermissionGranted(this)){
                    if (!TextUtils.isEmpty(contact.getContactNumber())) {
                        Intent callIntent;
                        String uri = "tel:" + contact.getContactNumber().trim();
                        callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse(uri));
                        startActivity(callIntent);
                    }
                } else {
                    snackbar = Snackbar.make(layout,R.string.phone_call_permission_not_granted ,
                            Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }

        } catch (Exception e) {
            Log.i("ConversationActivity", "Call permission is not added in androidManifest");
        }
    }


    public void processCameraAction() {
        try {
            if (PermissionsUtils.isCameraPermissionGranted(this)) {
                imageCapture();
            } else {
                if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(this)) {
                    applozicPermission.requestCameraPermission();
                } else {
                    imageCapture();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processVideoRecording() {
        try{
            if(PermissionsUtils.isCameraPermissionGranted(this)){
                showVideoCapture();
            }else {
                if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(this)) {
                    applozicPermission.requestCameraPermission();
                } else {
                    showVideoCapture();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void processContact(){
        if(Utils.hasMarshmallow() && PermissionsUtils.checkSelfForContactPermission(this)){
            applozicPermission.requestContactPermission();
        }else {
            Intent contactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            contactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            startActivityForResult(contactIntent, MultimediaOptionFragment.REQUEST_CODE_CONTACT_SHARE);
        }
    }


    public void imageCapture() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (cameraIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_" + ".jpeg";

            photoFile = FileClientService.getFilePath(imageFileName, this, "image/jpeg");

            // Continue only if the File was successfully created
            if (photoFile != null) {
                capturedImageUri = Uri.fromFile(photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
                startActivityForResult(cameraIntent, MultimediaOptionFragment.REQUEST_CODE_TAKE_PHOTO);
            }
        }
    }

    public void processAttachment(){
        if(Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission(this)){
            applozicPermission.requestStoragePermissions();
        }else {
            Intent intentPick = new Intent(this, MobiComAttachmentSelectorActivity.class);
            startActivityForResult(intentPick, MultimediaOptionFragment.REQUEST_MULTI_ATTCAHMENT);
        }
    }

    public void showVideoCapture() {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "VID_" + timeStamp + "_" + ".mp4";

        File fileUri = FileClientService.getFilePath(imageFileName, this, "video/mp4");
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileUri));
        videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        videoFileUri = Uri.fromFile(fileUri);
        startActivityForResult(videoIntent, MultimediaOptionFragment.REQUEST_CODE_CAPTURE_VIDEO_ACTIVITY);
    }
}