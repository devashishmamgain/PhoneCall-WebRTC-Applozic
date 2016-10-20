package com.applozic.mobicomkit.uiwidgets.conversation.activity;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.applozic.mobicomkit.feed.GroupInfoUpdate;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicommons.file.FilePathFinder;
import com.applozic.mobicommons.file.FileUtils;
import com.applozic.mobicommons.json.GsonUtils;
import com.soundcloud.android.crop.Crop;

import java.io.File;

/**
 * Created by sunil on 10/3/16.
 */
public class ChannelNameActivity extends AppCompatActivity {
    private static final String TAG = "ChannelNameActivity";
    private EditText channelName;
    private Button ok, cancel;
    public static final String CHANNEL_NAME = "CHANNEL_NAME";
    public static final String CHANNEL_IMAGE_URL = "IMAGE_URL";
    private static final int REQUEST_CODE_ATTACH_PHOTO = 701;
    String oldChannelName;
    ActionBar mActionBar;
    private ImageView selectImageProfileIcon;
    private ImageView applozicGroupProfileIcon;
    GroupInfoUpdate groupInfoUpdate;

    private Uri imageChangeUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_channel_name_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setTitle(getString(R.string.update_channel_title_name));
        selectImageProfileIcon = (ImageView) findViewById(R.id.applozic_group_profile_camera);
        applozicGroupProfileIcon = (ImageView) findViewById(R.id.applozic_group_profile);

        if (getIntent().getExtras() != null) {
            String groupInfoJson  = getIntent().getExtras().getString(ChannelInfoActivity.GROUP_UPDTAE_INFO);
            groupInfoUpdate = (GroupInfoUpdate) GsonUtils.getObjectFromJson(groupInfoJson,GroupInfoUpdate.class);
        }

        if( groupInfoUpdate!=null && !TextUtils.isEmpty(groupInfoUpdate.getLocalImagePath())){
            Uri uri =  Uri.fromFile(new File(groupInfoUpdate.getLocalImagePath()));
            if(uri!=null){
                Log.i("ChannelNameActivity::",   uri.toString());
                applozicGroupProfileIcon.setImageURI(uri);
            }
        }else{
            applozicGroupProfileIcon.setImageResource(R.drawable.applozic_group_icon);

        }
        channelName = (EditText) findViewById(R.id.newChannelName);
        channelName.setText( groupInfoUpdate.getNewName());
        ok = (Button) findViewById(R.id.channelNameOk);
        cancel = (Button) findViewById(R.id.channelNameCancel);
        selectImageProfileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchImagePicker();
            }
        });

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (groupInfoUpdate.getNewName().equals(channelName.getText().toString())&& imageChangeUri==null) {
                    ChannelNameActivity.this.finish();
                }
                if (TextUtils.isEmpty(channelName.getText().toString()) || channelName.getText().toString().trim().length() == 0) {

                    Toast.makeText(ChannelNameActivity.this, getString(R.string.channel_name_empty), Toast.LENGTH_SHORT).show();
                    ChannelNameActivity.this.finish();

                } else {
                    Intent intent = new Intent();
                    groupInfoUpdate.setNewName(channelName.getText().toString());
                    if(imageChangeUri!=null){
                        String filePath = FilePathFinder.getPath(ChannelNameActivity.this, imageChangeUri);
                        groupInfoUpdate.setNewlocalPath(filePath);
                    }
                    intent.putExtra(ChannelInfoActivity.GROUP_UPDTAE_INFO,GsonUtils.getJsonFromObject(groupInfoUpdate,GroupInfoUpdate.class));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChannelNameActivity.this.finish();

            }
        });
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
            beginCrop(selectedFileUri);
        }
        if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            try{
                imageChangeUri = Crop.getOutput(intent);
                applozicGroupProfileIcon.setImageDrawable(null); // <--- added to force redraw of ImageView
                applozicGroupProfileIcon.setImageURI(imageChangeUri);
            }catch (Exception e){
                Log.i(TAG, "exception in profile image");
            }
        }

    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(),groupInfoUpdate.getGroupId()+ "_profile_temp.jpeg"));
        Crop.of(source, destination).asSquare().start(this);
    }


}
