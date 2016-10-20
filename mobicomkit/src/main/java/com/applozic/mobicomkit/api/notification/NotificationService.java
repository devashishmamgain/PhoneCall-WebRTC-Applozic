package com.applozic.mobicomkit.api.notification;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.attachment.FileClientService;
import com.applozic.mobicomkit.api.attachment.FileMeta;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.commons.image.ImageUtils;
import com.applozic.mobicommons.file.FileUtils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.channel.ChannelUtils;
import com.applozic.mobicommons.people.contact.Contact;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: devashish
 * Date: 17/3/13
 * Time: 7:36 PM
 */
public class NotificationService {

    private static final int NOTIFICATION_ID = 1000;
    private static final String NOTIFICATION_SMALL_ICON_METADATA = "com.applozic.mobicomkit.notification.smallIcon";
    private Context context;
    private int iconResourceId;
    private int wearable_action_title;
    private int wearable_action_label;
    private int wearable_send_icon;
    private AppContactService appContactService;
    private ApplozicClient applozicClient;
    private String activityToOpen;

    public NotificationService(int iconResourceID, Context context, int wearable_action_label, int wearable_action_title, int wearable_send_icon) {
        this.context = context;
        this.iconResourceId = iconResourceID;
        this.wearable_action_label = wearable_action_label;
        this.wearable_action_title = wearable_action_title;
        this.wearable_send_icon = wearable_send_icon;
        this.applozicClient =  ApplozicClient.getInstance(context);
        this.appContactService = new AppContactService(context);
        activityToOpen = Utils.getMetaDataValue(context,"activity.open.on.notification");
    }

    public void notifyUser(Contact contact, Channel channel, Message message) {
        String title;
        String notificationText;
        Contact displayNameContact = null;
        if (message.getGroupId() != null) {
            title = ChannelUtils.getChannelTitleName(channel, MobiComUserPreference.getInstance(context).getUserId());
            displayNameContact = appContactService.getContactById(message.getTo());
        } else {
            title = contact.getDisplayName();
        }

        if (message.getContentType() == Message.ContentType.LOCATION.getValue()) {
            notificationText = MobiComKitConstants.LOCATION;
        } else if (message.getContentType() == Message.ContentType.AUDIO_MSG.getValue()) {
            notificationText = MobiComKitConstants.AUDIO;
        } else if (message.getContentType() == Message.ContentType.VIDEO_MSG.getValue()){
            notificationText = MobiComKitConstants.VIDEO;
        } else if(message.hasAttachment() && TextUtils.isEmpty(message.getMessage())){
            notificationText = MobiComKitConstants.ATTACHMENT;
        }else {
            notificationText = message.getMessage();
        }

        Class activity = null;
        try {
            activity = Class.forName(activityToOpen);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Integer smallIconResourceId = Utils.getMetaDataValueForResources(context, NOTIFICATION_SMALL_ICON_METADATA) != null ? Utils.getMetaDataValueForResources(context, NOTIFICATION_SMALL_ICON_METADATA) : iconResourceId;
        Intent intent = new Intent(context, activity);
        intent.putExtra(MobiComKitConstants.MESSAGE_JSON_INTENT, GsonUtils.getJsonFromObject(message, Message.class));
        if(applozicClient.isChatListOnNotificationIsHidden()) {
            intent.putExtra("takeOrder",true);
        }
        if(applozicClient.isContextBasedChat()){
            intent.putExtra("contextBasedChat",true);
        }
        intent.putExtra("sms_body", "text");
        intent.setType("vnd.android-dir/mms-sms");
        PendingIntent pendingIntent;

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        if (!isRunning(context)) {
            stackBuilder.addParentStack(activity);
            stackBuilder.addNextIntent(intent);
            pendingIntent = stackBuilder.getPendingIntent((int) (System.currentTimeMillis() & 0xfffffff), PendingIntent.FLAG_UPDATE_CURRENT);
        }else {
            pendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff),
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(smallIconResourceId)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), iconResourceId))
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle(title)
                        .setContentText(channel != null ? displayNameContact.getDisplayName() + ": " + notificationText : notificationText)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);
        if (message.hasAttachment()) {
            try {
                InputStream in;
                FileMeta fileMeta = message.getFileMetas();
                HttpURLConnection httpConn = null;
                if (fileMeta.getThumbnailUrl() != null) {
                    httpConn = new MobiComKitClientService(context).openHttpConnection(fileMeta.getThumbnailUrl());
                    int response = httpConn.getResponseCode();
                    if (response == HttpURLConnection.HTTP_OK) {
                        in = httpConn.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(in);
                        String imageName = fileMeta.getBlobKeyString() + "." + FileUtils.getFileFormat(fileMeta.getName());
                        File file = new FileClientService(context).getFilePath(imageName, context, "image", true);
                        ImageUtils.saveImageToInternalStorage(file, bitmap);
                        mBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        WearableNotificationWithVoice notificationWithVoice =
                new WearableNotificationWithVoice(mBuilder, wearable_action_title,
                        wearable_action_label, wearable_send_icon, message.getGroupId() != null ? String.valueOf(message.getGroupId()).hashCode() : message.getContactIds().hashCode());
        notificationWithVoice.setCurrentContext(context);
        notificationWithVoice.setPendingIntent(pendingIntent);

        try {
            notificationWithVoice.sendNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning(Context ctx) {
        try {
            ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
            return tasks != null && tasks.size() > 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}