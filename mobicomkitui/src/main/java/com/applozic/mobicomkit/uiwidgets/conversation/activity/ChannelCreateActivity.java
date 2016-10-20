package com.applozic.mobicomkit.uiwidgets.conversation.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.applozic.mobicomkit.api.attachment.FileClientService;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.file.FilePathFinder;
import com.applozic.mobicommons.file.FileUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.soundcloud.android.crop.Crop;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by sunil on 3/2/16.
 */


public class ChannelCreateActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ATTACH_PHOTO = 901;
    private static final String TAG = "ChannelCreateActivity";
    public static String GROUP_TYPE ="GroupType";
    private EditText channelName;
    private CircleImageView circleImageView;
    private View focus;
    private ActionBar mActionBar;
    public static Activity channelActivity;
    private ImageView uploadImageButton;
    private Uri imageChangeUri;
    private String groupIconImageLink;
    private Short groupType;
    private FinishActivityReceiver finishActivityReceiver;
    public static final String ACTION_FINISH_CHANNEL_CREATE =
            "channelCreateActivity.ACTION_FINISH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_create_activty_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        finishActivityReceiver= new FinishActivityReceiver();
        registerReceiver(finishActivityReceiver, new IntentFilter(ACTION_FINISH_CHANNEL_CREATE));
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        channelName = (EditText) findViewById(R.id.channelName);
        circleImageView = (CircleImageView) findViewById(R.id.channelIcon);
        uploadImageButton = (ImageView)  findViewById(R.id.applozic_channel_profile_camera);
        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchImagePicker();
            }
        });
        groupType = getIntent().getShortExtra(GROUP_TYPE, Channel.GroupType.PRIVATE.getValue());
        if(groupType.equals(Channel.GroupType.BROADCAST_ONE_BY_ONE.getValue()) || groupType.equals(Channel.GroupType.BROADCAST.getValue())){
            mActionBar.setTitle(R.string.new_broadcast_create_title);
            channelName.setHint(R.string.broadcast_name_hint);
            circleImageView.setImageResource(R.drawable.ic_applozic_broadcast);
            uploadImageButton.setVisibility(View.GONE);
        }else{
            channelName.setHint(R.string.group_name_hint);
            mActionBar.setTitle(R.string.channel_create_title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_create_menu, menu);
        menu.removeItem(R.id.Done);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.Next) {
            boolean check = true;
            if (channelName.getText().toString().trim().length() == 0 || TextUtils.isEmpty(channelName.getText().toString())) {
                focus = channelName;
                focus.requestFocus();
                check = false;
            }
            if (check) {
                Utils.toggleSoftKeyBoard(ChannelCreateActivity.this, true);
                Intent intent = new Intent(ChannelCreateActivity.this, ContactSelectionActivity.class);
                intent.putExtra(ContactSelectionActivity.CHANNEL, channelName.getText().toString());
                if(!TextUtils.isEmpty(groupIconImageLink)){
                    intent.putExtra(ContactSelectionActivity.IMAGE_LINK, groupIconImageLink);
                }
                intent.putExtra(GROUP_TYPE, groupType);
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(), "new_group_profile.jpeg"));
        Crop.of(source, destination).asSquare().start(this);
    }

    public void launchImagePicker(){
        Intent getContentIntent = FileUtils.createGetContentIntent();
        getContentIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        Intent intentPick = Intent.createChooser(getContentIntent, getString(R.string.select_file));
        startActivityForResult(intentPick, REQUEST_CODE_ATTACH_PHOTO);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Uri selectedFileUri=null;
        if(requestCode == REQUEST_CODE_ATTACH_PHOTO  && resultCode == RESULT_OK){
            selectedFileUri = (intent == null ? null : intent.getData());
            Log.i(TAG, "selectedFileUri :: " + selectedFileUri);
            beginCrop(selectedFileUri);
        }
        if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            try{
                imageChangeUri = Crop.getOutput(intent);
                circleImageView.setImageDrawable(null); // <--- added to force redraw of ImageView
                circleImageView.setImageURI(imageChangeUri);
                new ProfilePictureUpload(imageChangeUri,ChannelCreateActivity.this).execute((Void[]) null);
            }catch (Exception e){
                Log.i(TAG, "exception in profile image");
            }
        }

    }

    class ProfilePictureUpload extends AsyncTask<Void, Void, Boolean> {

        Context context;
        Uri fileUri;
        String displayName;
        private ProgressDialog progressDialog;

        public ProfilePictureUpload( Uri fileUri , Context context) {
            this.context = context;
            this.fileUri=fileUri;

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(context, "",
                    context.getString(R.string.applozic_contacts_loading_info), true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            FileClientService fileClientService =new FileClientService(context);
            try {
                if(fileUri!=null){
                   String filePath = FilePathFinder.getPath(context, fileUri);
                    groupIconImageLink= fileClientService.uploadProfileImage(filePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(ChannelCreateActivity.class.getName(),  "Exception");

            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            progressDialog.dismiss();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finishActivityReceiver);
    }

    private final class FinishActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_FINISH_CHANNEL_CREATE))
                finish();
        }
    }
}
