package com.applozic.mobicomkit.uiwidgets.people.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.UserService;
import com.applozic.mobicomkit.api.attachment.FileClientService;

import com.applozic.mobicomkit.contact.AppContactService;

import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicommons.commons.image.ImageLoader;
import com.applozic.mobicommons.file.FilePathFinder;
import com.applozic.mobicommons.file.FileUtils;
import com.applozic.mobicommons.people.contact.Contact;

import com.soundcloud.android.crop.Crop;

import java.io.File;


public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ATTACH_PHOTO = 101;
    private static final String TAG = "ProfileActivity";
    public static final int PROFILE_UPDATED = 1001;
    private ImageView img_profile;
    private ImageView selectImageProfileIcon;
    private ActionBar mActionBar;
    private Uri imageChangeUri;
    private EditText editTextDisplayName;
    private EditText editTextPhoneNo;
    private EditText editTextStatus;
    private ImageButton displayNameBtn;
    private ImageButton editPhoneNoBtn;
    private ImageButton editStatus;
    private ImageLoader mImageLoader; // Handles loading the contact image in a background thread
    AppContactService contactService;
    Contact userContact;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setTitle(R.string.applozic_user_profile_heading);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        img_profile = (ImageView) findViewById(R.id.applozic_user_profile);
        selectImageProfileIcon = (ImageView) findViewById(R.id.applozic_user_profile_camera);

        editTextDisplayName =  (EditText) findViewById(R.id.applozic_profile_display_name);
        editTextPhoneNo =  (EditText) findViewById(R.id.applozic_profile_phone_no);
        editTextStatus =  (EditText) findViewById(R.id.applozic_profile_status);

        displayNameBtn =  (ImageButton) findViewById(R.id.applozic_profile_display_name_btn);
        editPhoneNoBtn =  (ImageButton) findViewById(R.id.applozic_profile_phone_no_btn);
        editStatus =  (ImageButton) findViewById(R.id.applozic_profile_status_btn);


        contactService  = new AppContactService(getApplicationContext());
        userContact =  contactService.getContactById(MobiComUserPreference.getInstance(getApplicationContext()).getUserId());
        editTextDisplayName.setText(userContact.getDisplayName());
        editTextPhoneNo.setText(userContact.getContactNumber());
        editTextStatus.setText(userContact.getStatus());
        editTextDisplayName.setEnabled(false);
        editTextPhoneNo.setEnabled(false);
        editTextStatus.setEnabled(false);
        mImageLoader = new ImageLoader(getApplicationContext(),img_profile.getHeight()) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return contactService.downloadContactImage(getApplicationContext(), (Contact) data);
            }
        };
        //For profile image
        selectImageProfileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             launchImagePicker();
            }
        });

        displayNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editTextPhoneNo.setEnabled(false);
                editTextStatus.setEnabled(false);
                //editTextDisplayName.setFocusable(!editTextDisplayName.isEnabled());
                editTextDisplayName.setEnabled(!editTextDisplayName.isEnabled());
                if(editTextDisplayName.isEnabled()){
                    displayNameBtn.setImageResource(R.drawable.applozic_ic_action_message_sent);
                }else{
                    displayNameBtn.setImageResource(R.drawable.applozic_ic_action_edit);
                }

            }
        });


        editPhoneNoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextPhoneNo.setEnabled(true);
                editTextDisplayName.setFocusable(false);
                editTextPhoneNo.setFocusable(true);
            }
        });

        editStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextPhoneNo.setEnabled(false);
                editTextDisplayName.setEnabled(false);
                //editTextStatus.setFocusable(!editTextStatus.isEnabled());
                editTextStatus.setEnabled(!editTextStatus.isEnabled());
                if(editTextStatus.isEnabled()){
                    editStatus.setImageResource(R.drawable.applozic_ic_action_message_sent);
                }else{
                    editStatus.setImageResource(R.drawable.applozic_ic_action_edit);

                }
            }
        });

        mImageLoader.setImageFadeIn(false);
        mImageLoader.setLoadingImage(R.drawable.applozic_ic_contact_picture_180_holo_light);
        mImageLoader.loadImage(userContact,img_profile);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);

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
                img_profile.setImageDrawable(null); // <--- added to force redraw of ImageView
                img_profile.setImageURI(imageChangeUri);
            }catch (Exception e){
                Log.i(TAG, "exception in profile image");
            }
        }

    }


    public void launchImagePicker(){
        Intent getContentIntent = FileUtils.createGetContentIntent();
        getContentIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        Intent intentPick = Intent.createChooser(getContentIntent, getString(R.string.select_file));
        startActivityForResult(intentPick, REQUEST_CODE_ATTACH_PHOTO);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.applozic_profile_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.applozic_user_profile_save) {
            new ProfilePictureUpload(imageChangeUri,ProfileActivity.this).execute((Void[]) null);
        }

        if(item.getItemId() == android.R.id.home){
            Intent intent = new Intent();
            setResult(RESULT_CANCELED,intent);
            finish();
        }
        return true;
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(), "profile.jpeg"));
        Crop.of(source, destination).asSquare().start(this);
    }


    class ProfilePictureUpload extends AsyncTask<Void, Void, Boolean> {

        Context context;
        Uri fileUri;
        String displayName;
        String status;
        private ProgressDialog progressDialog;

        public ProfilePictureUpload( Uri fileUri ,Context context) {
            this.context = context;
            this.fileUri=fileUri;
            this.displayName =editTextDisplayName.getText().toString();
            this.status =editTextStatus.getText().toString();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(context, "",
                    context.getString(R.string.applozic_contacts_loading_info), true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            FileClientService fileClientService =new FileClientService(ProfileActivity.this);
            UserService userService =  UserService.getInstance(context);
            try {
                String response =null;
                String filePath=null;
                if(fileUri!=null){
                    filePath = FilePathFinder.getPath(ProfileActivity.this, fileUri);
                    response= fileClientService.uploadProfileImage(filePath);
                }
                userService.updateDisplayNameORImageLink(displayName,response,filePath,status);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(ProfileActivity.class.getName(),  "Exception");

            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            progressDialog.dismiss();
            Intent intent = getIntent();
            setResult(Activity.RESULT_OK,intent);
            finish();
        }

    }



}
