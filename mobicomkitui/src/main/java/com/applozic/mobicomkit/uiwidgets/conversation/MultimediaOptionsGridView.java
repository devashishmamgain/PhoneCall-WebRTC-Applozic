package com.applozic.mobicomkit.uiwidgets.conversation;

import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.PopupWindow;

import com.applozic.mobicomkit.api.attachment.FileClientService;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ConversationActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.MobiComAttachmentSelectorActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.MultimediaOptionFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by reytum on 19/3/16.
 */
public class MultimediaOptionsGridView {
    private Uri capturedImageUri;
    public PopupWindow showPopup;
    FragmentActivity context;
    GridView multimediaOptions;

    public MultimediaOptionsGridView(FragmentActivity context, GridView multimediaOptions) {
        this.context = context;
        this.multimediaOptions = multimediaOptions;
    }

    public void setMultimediaClickListener() {
        capturedImageUri = null;
        multimediaOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        ((ConversationActivity) context).processLocation();
                        break;
                    case 1:
                        ((ConversationActivity) context).isTakePhoto(true);
                        ((ConversationActivity) context).processCameraAction();

                        break;
                    case 2:
                        ((ConversationActivity) context).isAttachment(true);
                        ((ConversationActivity) context).processAttachment();
                        break;
                    case 3:
                        ((ConversationActivity) context).showAudioRecordingDialog();
                        break;
                    case 4:
                        ((ConversationActivity) context).isTakePhoto(false);
                        ((ConversationActivity) context).processVideoRecording();
                        break;
                    case 5:
                        //Sharing contact.
                        ((ConversationActivity) context).processContact();
                        break;

                    case 6:
                        new ConversationUIService(context).sendPriceMessage();
                        break;
                    default:

                }
                multimediaOptions.setVisibility(View.GONE);
            }
        });
    }

}
