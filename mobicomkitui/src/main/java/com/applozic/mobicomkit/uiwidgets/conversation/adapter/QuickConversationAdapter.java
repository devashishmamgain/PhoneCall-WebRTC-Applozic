package com.applozic.mobicomkit.uiwidgets.conversation.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.attachment.FileMeta;
import com.applozic.mobicomkit.api.attachment.VideoCallNotificationHelper;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.database.MessageDatabaseService;
import com.applozic.mobicomkit.channel.database.ChannelDatabaseService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicomkit.uiwidgets.alphanumbericcolor.AlphaNumberColorUtil;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.MobiComKitActivityInterface;
import com.applozic.mobicomkit.InstructionUtil;
import com.applozic.mobicommons.commons.core.utils.DateUtils;
import com.applozic.mobicommons.commons.image.ImageLoader;
import com.applozic.mobicommons.commons.image.ImageUtils;
import com.applozic.mobicommons.emoticon.EmojiconHandler;
import com.applozic.mobicommons.emoticon.EmoticonUtils;
import com.applozic.mobicommons.file.FileUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.channel.ChannelUtils;
import com.applozic.mobicommons.people.contact.Contact;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by adarsh on 4/7/15.
 */
public class QuickConversationAdapter extends BaseAdapter {

    private static Map<Short, Integer> messageTypeColorMap = new HashMap<Short, Integer>();

    static {
        messageTypeColorMap.put(Message.MessageType.INBOX.getValue(), R.color.message_type_inbox);
        messageTypeColorMap.put(Message.MessageType.OUTBOX.getValue(), R.color.message_type_outbox);
        messageTypeColorMap.put(Message.MessageType.OUTBOX_SENT_FROM_DEVICE.getValue(), R.color.message_type_outbox_sent_from_device);
        messageTypeColorMap.put(Message.MessageType.MT_INBOX.getValue(), R.color.message_type_mt_inbox);
        messageTypeColorMap.put(Message.MessageType.MT_OUTBOX.getValue(), R.color.message_type_mt_outbox);
        messageTypeColorMap.put(Message.MessageType.CALL_INCOMING.getValue(), R.color.message_type_incoming_call);
        messageTypeColorMap.put(Message.MessageType.CALL_OUTGOING.getValue(), R.color.message_type_outgoing_call);
    }

    public ImageLoader contactImageLoader,channelImageLoader;
    private Context context;
    private MessageDatabaseService messageDatabaseService;
    private List<Message> messageList;
    private BaseContactService contactService;
    private EmojiconHandler emojiconHandler;
    private TextView messageTextView;
    private ImageView attachmentIcon;
    private long deviceTimeOffset = 0;

    public QuickConversationAdapter(final Context context, List<Message> messageList, EmojiconHandler emojiconHandler) {
        this.context = context;
        this.emojiconHandler = emojiconHandler;
        this.contactService = new AppContactService(context);
        this.messageDatabaseService = new MessageDatabaseService(context);
        this.messageList = messageList;
        contactImageLoader = new ImageLoader(context, ImageUtils.getLargestScreenDimension((Activity) context)) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return contactService.downloadContactImage(context, (Contact) data);
            }
        };
        channelImageLoader = new ImageLoader(context, ImageUtils.getLargestScreenDimension((Activity) context)) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return contactService.downloadGroupImage(context, (Channel) data);
            }
        };
        if(context.getClass().isInstance(FragmentActivity.class)){

            contactImageLoader.addImageCache(((FragmentActivity) context).getSupportFragmentManager(), 0.1f);
            channelImageLoader.addImageCache(((FragmentActivity) context).getSupportFragmentManager(), 0.1f);

        }else{
            contactImageLoader.addImageCache(((Activity) context).getFragmentManager(), 0.1f);
            channelImageLoader.addImageCache(((Activity) context).getFragmentManager(), 0.1f);
        }
        contactImageLoader.setImageFadeIn(false);
        channelImageLoader.setImageFadeIn(false);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        deviceTimeOffset = MobiComUserPreference.getInstance(context).getDeviceTimeOffset();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.mobicom_message_row_view, parent, false);
        TextView smTime = (TextView) customView.findViewById(R.id.smTime);
        smTime.setVisibility(View.GONE);
        final Message message = getItem(position);
        if (message != null) {

            TextView smReceivers = (TextView) customView.findViewById(R.id.smReceivers);
            TextView createdAtTime = (TextView) customView.findViewById(R.id.createdAtTime);
            messageTextView = (TextView) customView.findViewById(R.id.message);
            //ImageView contactImage = (ImageView) customView.findViewById(R.id.contactImage);
            CircleImageView contactImage = (CircleImageView) customView.findViewById(R.id.contactImage);
            TextView alphabeticTextView = (TextView) customView.findViewById(R.id.alphabeticImage);
            TextView onlineTextView = (TextView) customView.findViewById(R.id.onlineTextView);
            ImageView sentOrReceived = (ImageView) customView.findViewById(R.id.sentOrReceivedIcon);
            TextView attachedFile = (TextView) customView.findViewById(R.id.attached_file);
            attachmentIcon = (ImageView) customView.findViewById(R.id.attachmentIcon);
            TextView unReadCountTextView = (TextView) customView.findViewById(R.id.unreadSmsCount);
            List<String> items = null;
            List<String> userIds = null;

            final Channel channel = ChannelDatabaseService.getInstance(context).getChannelByChannelKey(message.getGroupId());

            if (channel == null && message.getGroupId() == null) {
                items = Arrays.asList(message.getTo().split("\\s*,\\s*"));
                if (!TextUtils.isEmpty(message.getContactIds())) {
                    userIds = Arrays.asList(message.getContactIds().split("\\s*,\\s*"));
                }
            }

            final Contact contactReceiver = contactService.getContactReceiver(items, userIds);

            if (contactReceiver != null) {
                String contactInfo = contactReceiver.getDisplayName();
                if (items.size() > 1) {
                    Contact contact2 = contactService.getContactById(items.get(1));
                    contactInfo = TextUtils.isEmpty(contactReceiver.getFirstName()) ? contactReceiver.getContactNumber() : contactReceiver.getFirstName() + ", "
                            + (TextUtils.isEmpty(contact2.getFirstName()) ? contact2.getContactNumber() : contact2.getFirstName()) + (items.size() > 2 ? " & others" : "");
                }
                smReceivers.setText(contactInfo);
            }
            if (message.getGroupId() == null) {
                contactImageLoader.setLoadingImage(R.drawable.applozic_ic_contact_picture_holo_light);
            }else {
                channelImageLoader.setLoadingImage(R.drawable.applozic_group_icon);

            }
            String contactNumber = "";
            char firstLetter = 0;
            if (channel != null && message.getGroupId() != null) {
                smReceivers.setText(ChannelUtils.getChannelTitleName(channel, MobiComUserPreference.getInstance(context).getUserId()));
                if (!TextUtils.isEmpty(channel.getImageUrl())) {
                    channelImageLoader.loadImage(channel, contactImage);
                }else if(channel.isBroadcastMessage()){
                    contactImage.setImageResource(R.drawable.ic_applozic_broadcast);
                }else {
                    channelImageLoader.setLoadingImage(R.drawable.applozic_group_icon);
                }
            } else if (contactReceiver != null) {
                contactNumber = contactReceiver.getDisplayName().toUpperCase();
                firstLetter = contactReceiver.getDisplayName().toUpperCase().charAt(0);

                if (contactReceiver != null) {
                    if (firstLetter != '+') {
                        alphabeticTextView.setText(String.valueOf(firstLetter));
                    } else if (contactNumber.length() >= 2) {
                        alphabeticTextView.setText(String.valueOf(contactNumber.charAt(1)));
                    }
                    Character colorKey = AlphaNumberColorUtil.alphabetBackgroundColorMap.containsKey(firstLetter) ? firstLetter : null;
                /*alphabeticTextView.setTextColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetTextColorMap.get(colorKey)));
                alphabeticTextView.setBackgroundResource(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(colorKey));*/
                    GradientDrawable bgShape = (GradientDrawable) alphabeticTextView.getBackground();
                    bgShape.setColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(colorKey)));
                }
                if (contactReceiver.isDrawableResources()) {
                    int drawableResourceId = context.getResources().getIdentifier(contactReceiver.getrDrawableName(), "drawable", context.getPackageName());
                    contactImage.setImageResource(drawableResourceId);
                } else {
                    if(TextUtils.isEmpty(contactReceiver.getImageURL())){
                        alphabeticTextView.setVisibility(View.VISIBLE);
                        contactImage.setVisibility(View.GONE);
                    }else {
                        contactImageLoader.loadImage(contactReceiver, contactImage, alphabeticTextView);
                    }
                }
            }
            if (ApplozicSetting.getInstance(context).isOnlineStatusInMasterListVisible()) {
                onlineTextView.setVisibility(contactReceiver != null && contactReceiver.isOnline() ? View.VISIBLE : View.GONE);
            }

            customView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    InstructionUtil.hideInstruction(context, R.string.instruction_open_conversation_thread);
                    ((MobiComKitActivityInterface) context).onQuickConversationFragmentItemClick(view, contactReceiver, channel,message.getConversationId());
                }
            });

            if (attachedFile != null) {
                attachedFile.setText("");
                attachedFile.setVisibility(View.GONE);
            }

            if (attachmentIcon != null) {
                attachmentIcon.setVisibility(View.GONE);
            }

            if (message.hasAttachment() && attachmentIcon != null && !(message.getContentType() == Message.ContentType.TEXT_URL.getValue())) {
                //Todo: handle it for fileKeyStrings when filePaths is empty
                if (message.getFileMetas() == null && message.getFilePaths() != null) {
                    String mimType = FileUtils.getMimeType(new File(message.getFilePaths().get(0)));
                    setUpAttachmentMessage(message.getMessage(),mimType,messageTextView,attachmentIcon);
                }else {
                    FileMeta fileMeta = message.getFileMetas();

                    if(fileMeta != null){
                        setUpAttachmentMessage(message.getMessage(),fileMeta.getContentType(),messageTextView,attachmentIcon);
                    }
                }
            } else if (attachmentIcon != null && message.getContentType() == Message.ContentType.LOCATION.getValue()) {
                attachmentIcon.setVisibility(View.VISIBLE);
                attachmentIcon.setImageResource(R.drawable.mobicom_notification_location_icon);
                messageTextView.setText("Location");
            } else if (message.getContentType() == Message.ContentType.PRICE.getValue()) {
                messageTextView.setText(EmoticonUtils.getSmiledText(context, ConversationUIService.FINAL_PRICE_TEXT + message.getMessage(), emojiconHandler));
            } else {
                messageTextView.setText(EmoticonUtils.getSmiledText(context, message.getMessage(), emojiconHandler));
            }

          /*  if (contactReceiver != null && new Support(context).isSupportNumber(contactReceiver.getContactNumber()) && (!message.isTypeOutbox())) {
                contactImage.setImageResource(R.drawable.mobicom_ic_launcher);
            }*/
            if (sentOrReceived != null) {
                if (message.isCall()) {
                    sentOrReceived.setImageResource(R.drawable.applozic_ic_action_call_holo_light);
                    messageTextView.setTextColor(context.getResources().getColor(message.isIncomingCall() ? R.color.incoming_call : R.color.outgoing_call));
                } else if (getItemViewType(position) == 0) {
                    sentOrReceived.setImageResource(R.drawable.mobicom_social_forward);
                } else {
                    sentOrReceived.setImageResource(R.drawable.mobicom_social_reply);
                }
            }
            if (createdAtTime != null) {
                createdAtTime.setText(DateUtils.getFormattedDateAndTime(message.getCreatedAtTime()));
            }
            int messageUnReadCount = 0;
            if (message.getGroupId() == null && contactReceiver != null && !TextUtils.isEmpty(contactReceiver.getContactIds())) {
                messageUnReadCount = messageDatabaseService.getUnreadMessageCountForContact(contactReceiver.getContactIds());

            } else if (channel != null && channel.getKey() != null && channel.getKey() != 0) {
                messageUnReadCount = messageDatabaseService.getUnreadMessageCountForChannel(channel.getKey());
            }
            if (messageUnReadCount > 0) {
                unReadCountTextView.setVisibility(View.VISIBLE);
                unReadCountTextView.setText(String.valueOf(messageUnReadCount));
            } else {
                unReadCountTextView.setVisibility(View.GONE);
            }
            if(message.isVideoCallMessage()){
                createVideoCallView(message);
            }
        }


        return customView;
    }


    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public Message getItem(int position) {
        return messageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public int getItemViewType(int position) {
        return getItem(position).isTypeOutbox() ? 1 : 0;
    }

    public void setUpAttachmentMessage(String message,String mimType ,TextView messageTextView,ImageView attachmentIcon ){
        if (!TextUtils.isEmpty(mimType)) {
            if (mimType.startsWith("image")) {
                attachmentIcon.setVisibility(View.VISIBLE);
                attachmentIcon.setImageResource(R.drawable.applozic_ic_action_camera);
                if (TextUtils.isEmpty(message)) {
                    messageTextView.setText("Photo");
                } else {
                    messageTextView.setText(message);
                }

            } else if (mimType.startsWith("video")) {
                attachmentIcon.setVisibility(View.VISIBLE);
                attachmentIcon.setImageResource(R.drawable.applozic_ic_action_video);
                if (TextUtils.isEmpty(message)) {
                    messageTextView.setText("Video");
                } else {
                    messageTextView.setText(message);
                }
            } else {
                attachmentIcon.setVisibility(View.VISIBLE);
                messageTextView.setText("Attachment");
            }
        }
    }

    public void createVideoCallView(Message message ){

        messageTextView.setText(VideoCallNotificationHelper.getStatus(message.getMetadata()));
        attachmentIcon.setVisibility(View.VISIBLE);
        if (VideoCallNotificationHelper.isMissedCall(message)) {
            attachmentIcon.setImageResource(R.drawable.ic_communication_call_missed);
            attachmentIcon.setColorFilter(R.color.applozic_red_color);
        } else if(VideoCallNotificationHelper.isAudioCall(message)) {
            attachmentIcon.setImageResource(R.drawable.applozic_ic_action_call_holo_light);
            attachmentIcon.setColorFilter(R.color.applozic_red_color);
        }else{
            attachmentIcon.setImageResource(R.drawable.ic_videocam_white_24px);
            attachmentIcon.setColorFilter(R.color.applozic_green_color);
        }

    }

}