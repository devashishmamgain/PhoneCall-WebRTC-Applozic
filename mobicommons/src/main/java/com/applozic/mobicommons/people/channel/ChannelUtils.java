package com.applozic.mobicommons.people.channel;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.applozic.mobicommons.people.contact.ContactUtils;

/**
 * Created by devashish on 17/12/14.
 */
public class ChannelUtils {

    public static Channel fetchGroup(Context context, Integer groupId, String groupName) {
        Channel channel = new Channel(groupId, groupName);

        String where = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "=" + groupId
                + " AND "
                + ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + "='"
                + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'";
        String[] projection = new String[]{ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID, ContactsContract.Data.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, where, null,
                ContactsContract.Data.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
        while (cursor.moveToNext()) {
            channel.getContacts().add(ContactUtils.getContact(context, cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID))));
        }
        cursor.close();

        return channel;
    }

    public static Channel fetchGroup(Context context, Integer groupId) {
        String groupName = "";
        //Todo: fetch group name

        return fetchGroup(context, groupId, groupName);
    }

    public static String getChannelTitleName(Channel channel, String loggedInUserId) {

        if (!TextUtils.isEmpty(loggedInUserId)) {
            if (Channel.GroupType.SELLER.getValue().equals(channel.getType())) {
                String[] userIdSplit = new String[1];
                if (!TextUtils.isEmpty(channel.getName())) {
                    userIdSplit = channel.getName().split(":");
                }
                if (loggedInUserId.equals(channel.getAdminKey())) {
                    return channel.getName();
                } else {
                    return userIdSplit[0];
                }
            } else {
                return channel.getName();
            }
        }
        return "";
    }

    public static boolean isAdminUserId(String userId,Channel channel){
        if(channel != null &&  !TextUtils.isEmpty(channel.getAdminKey()) && !TextUtils.isEmpty(userId)){
            return channel.getAdminKey().equals(userId);
        }
        return false;
    }

}
