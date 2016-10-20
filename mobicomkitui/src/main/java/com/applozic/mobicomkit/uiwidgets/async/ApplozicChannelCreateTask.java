package com.applozic.mobicomkit.uiwidgets.async;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.people.ChannelInfo;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicommons.people.channel.Channel;

import java.util.List;

/**
 * Created by sunil on 17/5/16.
 */
public class ApplozicChannelCreateTask extends AsyncTask<Void, Void, Boolean> {
    Context context;
    String groupName;
    List<String> groupMemberList;
    ChannelService channelService;
    Channel channel;
    ChannelInfo channelInfo;
    Exception exception;
    ChannelCreateListener channelCreateListener;
    String groupImageLink;
    String clientGroupId;


    public ApplozicChannelCreateTask(Context context, ChannelCreateListener channelCreateListener, String groupName, List<String> groupMemberList, String groupImageLink) {
        this.context = context;
        this.groupName = groupName;
        this.groupMemberList = groupMemberList;
        this.groupImageLink = groupImageLink;
        this.channelCreateListener = channelCreateListener;
        this.channelService = ChannelService.getInstance(context);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            if (!TextUtils.isEmpty(groupName) && groupName.trim().length() != 0 && groupMemberList != null && groupMemberList.size() > 0) {
                channelInfo = new ChannelInfo(groupName.trim(), groupMemberList, groupImageLink);
                if (!TextUtils.isEmpty(clientGroupId)) {
                    channelInfo.setClientGroupId(clientGroupId);
                }
                channel = channelService.createChannel(channelInfo);
                return true;
            } else {
                throw new Exception(context.getString(R.string.applozic_channel_error_info_in_logs));
            }
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean resultBoolean) {
        super.onPostExecute(resultBoolean);

        if (resultBoolean && channel != null && channelCreateListener != null) {
            channelCreateListener.onSuccess(channel, context);
        } else if (exception != null && !resultBoolean && channelCreateListener != null) {
            channelCreateListener.onFailure(exception, context);
        }

    }

    public interface ChannelCreateListener {
        void onSuccess(Channel channel, Context context);

        void onFailure(Exception e, Context context);
    }
}