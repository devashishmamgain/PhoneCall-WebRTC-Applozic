package com.applozic.mobicomkit.channel.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.database.MobiComDatabaseHelper;
import com.applozic.mobicomkit.feed.GroupInfoUpdate;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.channel.ChannelUserMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sunil on 28/12/15.
 */
public class ChannelDatabaseService {

    private static final String TAG = "ChannelDatabaseService";
    private static final String CHANNEL = "channel";
    private static final String CHANNEL_USER_X = "channel_User_X";
    private static ChannelDatabaseService channelDatabaseService;
    private Context context;
    private MobiComUserPreference mobiComUserPreference;
    private MobiComDatabaseHelper dbHelper;

    private ChannelDatabaseService(Context context) {
        this.context = context;
        this.mobiComUserPreference = MobiComUserPreference.getInstance(context);
        this.dbHelper = MobiComDatabaseHelper.getInstance(context);
    }

    public synchronized static ChannelDatabaseService getInstance(Context context) {
        if (channelDatabaseService == null) {
            channelDatabaseService = new ChannelDatabaseService(context);
        }
        return channelDatabaseService;
    }

    public static ChannelUserMapper getChannelUser(Cursor cursor) {
        ChannelUserMapper channelUserMapper = new ChannelUserMapper();
        channelUserMapper.setUserKey(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.USERID)));
        channelUserMapper.setKey(cursor.getInt(cursor.getColumnIndex(MobiComDatabaseHelper.CHANNEL_KEY)));
        channelUserMapper.setUnreadCount(cursor.getShort(cursor.getColumnIndex(MobiComDatabaseHelper.UNREAD_COUNT)));
        return channelUserMapper;
    }

    public static List<ChannelUserMapper> getListOfUsers(Cursor cursor) {
        List<ChannelUserMapper> channelUserMapper = new ArrayList<ChannelUserMapper>();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            do {
                channelUserMapper.add(getChannelUser(cursor));
            } while (cursor.moveToNext());
        }
        return channelUserMapper;
    }

    public void addChannel(Channel channel) {
        try {
            ContentValues contentValues = prepareChannelValues(channel);
            dbHelper.getWritableDatabase().insertWithOnConflict(CHANNEL, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbHelper.close();
        }
    }

    public ContentValues prepareChannelValues(Channel channel) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME, channel.getName());
        contentValues.put(MobiComDatabaseHelper.CHANNEL_KEY, channel.getKey());
        contentValues.put(MobiComDatabaseHelper.CLIENT_GROUP_ID, channel.getClientGroupId());
        contentValues.put(MobiComDatabaseHelper.TYPE, channel.getType());
        contentValues.put(MobiComDatabaseHelper.ADMIN_ID, channel.getAdminKey());
        if (!TextUtils.isEmpty(channel.getImageUrl())) {
            contentValues.put(MobiComDatabaseHelper.CHANNEL_IMAGE_URL, channel.getImageUrl());
        }
        if (!TextUtils.isEmpty(channel.getLocalImageUri())) {
            contentValues.put(MobiComDatabaseHelper.CHANNEL_IMAGE_LOCAL_URI, channel.getLocalImageUri());
        }
        if (channel.getUserCount() != 0) {
            contentValues.put(MobiComDatabaseHelper.USER_COUNT, channel.getUserCount());
        }
        if (channel.getUnreadCount() != 0) {
            contentValues.put(MobiComDatabaseHelper.UNREAD_COUNT, channel.getUnreadCount());
        }
        return contentValues;
    }

    public void addChannelUserMapper(ChannelUserMapper channelUserMapper) {
        try {
            ContentValues contentValues = prepareChannelUserMapperValues(channelUserMapper);
            dbHelper.getWritableDatabase().insertWithOnConflict(CHANNEL_USER_X, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbHelper.close();
        }
    }

    public ContentValues prepareChannelUserMapperValues(ChannelUserMapper channelUserMapper) {
        ContentValues contentValues = new ContentValues();
        if (channelUserMapper != null) {
            if (channelUserMapper.getKey() != null) {
                contentValues.put(MobiComDatabaseHelper.CHANNEL_KEY, channelUserMapper.getKey());
            }
            if (channelUserMapper.getUserKey() != null) {
                contentValues.put(MobiComDatabaseHelper.USERID, channelUserMapper.getUserKey());
            }
            if (channelUserMapper.getUserKey() != null) {
                contentValues.put(MobiComDatabaseHelper.UNREAD_COUNT, channelUserMapper.getUnreadCount());
            }
            if (channelUserMapper.getStatus() != 0) {
                contentValues.put(MobiComDatabaseHelper.STATUS, channelUserMapper.getStatus());
            }
        }
        return contentValues;
    }

    public  StringBuffer  getAllChannelsNamesByUserId(String userId){

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        StringBuffer buf = new StringBuffer("SELECT ").
                append(MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME).
                append(" FROM ").append(MobiComDatabaseHelper.CHANNEL).
                append(" join  ").append(MobiComDatabaseHelper.CHANNEL_USER_X).
                append(" on ").append(MobiComDatabaseHelper.CHANNEL + "." + MobiComDatabaseHelper.CHANNEL_KEY + " = ").
                append(MobiComDatabaseHelper.CHANNEL_USER_X + "." + MobiComDatabaseHelper.CHANNEL_KEY).
                append(" WHERE ").
                append(MobiComDatabaseHelper.CHANNEL_USER_X + "." + MobiComDatabaseHelper.USERID).
                append(" = '").
                append(userId.replaceAll("'","''")+"'");

        Cursor cursor = database.rawQuery(buf.toString(),null);
        cursor.moveToFirst();
        StringBuffer sb =  new StringBuffer();
        if (cursor.getCount() > 0) {
            do {
                sb = sb.append(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME))+",");
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        dbHelper.close();
        return sb;
    }

    public Channel getChannelByClientGroupId(String clientGroupId) {
        Channel channel = null;
        try {
            String structuredNameWhere = MobiComDatabaseHelper.CLIENT_GROUP_ID + " =?";
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor cursor = db.query(CHANNEL, null, structuredNameWhere, new String[]{String.valueOf(clientGroupId)}, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    channel = getChannel(cursor);
                }
                cursor.close();
            }
            dbHelper.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return channel;
    }

    public Channel getChannelByChannelKey(final Integer channelKey) {
        Channel channel = null;
        try {
            String structuredNameWhere = MobiComDatabaseHelper.CHANNEL_KEY + " =?";
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor cursor = db.query(CHANNEL, null, structuredNameWhere, new String[]{String.valueOf(channelKey)}, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    channel = getChannel(cursor);
                }
                cursor.close();

            }
            dbHelper.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return channel;
    }

    public List<ChannelUserMapper> getChannelUserList(Integer channelKey) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String structuredNameWhere = "";

            structuredNameWhere += "channelKey = ?";
            Cursor cursor = db.query(CHANNEL_USER_X, null, structuredNameWhere, new String[]{String.valueOf(channelKey)}, null, null, null);

            List<ChannelUserMapper> channelUserMappers = getListOfUsers(cursor);

            cursor.close();
            dbHelper.close();

            return channelUserMappers;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Channel getChannel(Cursor cursor) {
        Channel channel = new Channel();
        channel.setKey(cursor.getInt(cursor.getColumnIndex(MobiComDatabaseHelper.CHANNEL_KEY)));
        channel.setClientGroupId(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.CLIENT_GROUP_ID)));
        channel.setName(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME)));
        channel.setAdminKey(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.ADMIN_ID)));
        channel.setType(cursor.getShort(cursor.getColumnIndex(MobiComDatabaseHelper.TYPE)));
        channel.setImageUrl(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.CHANNEL_IMAGE_URL)));
        channel.setLocalImageUri(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.CHANNEL_IMAGE_LOCAL_URI)));
        int count = cursor.getInt(cursor.getColumnIndex(MobiComDatabaseHelper.UNREAD_COUNT));
        if (count > 0) {
            channel.setUnreadCount(count);
        }
        return channel;
    }

    public List<Channel> getAllChannels() {
        List<Channel> contactList = null;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor cursor = db.query(CHANNEL, null, null, null, null, null, MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME + " asc");
            contactList = getChannelList(cursor);
            cursor.close();
            dbHelper.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contactList;
    }

    public List<Channel> getChannelList(Cursor cursor) {

        List<Channel> channelList = new ArrayList<Channel>();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            do {
                channelList.add(getChannel(cursor));
            } while (cursor.moveToNext());
        }
        return channelList;
    }

    public void updateChannel(Channel channel) {
        ContentValues contentValues = prepareChannelValues(channel);
        dbHelper.getWritableDatabase().update(CHANNEL, contentValues, MobiComDatabaseHelper.CHANNEL_KEY + "=?", new String[]{String.valueOf(channel.getKey())});
        dbHelper.close();
    }

    public void updateChannel(ChannelUserMapper channelUserMapper) {
        ContentValues contentValues = prepareChannelUserMapperValues(channelUserMapper);
        dbHelper.getWritableDatabase().update(CHANNEL_USER_X, contentValues, MobiComDatabaseHelper.CHANNEL_KEY + "=?  and " + MobiComDatabaseHelper.USERID + "=?", new String[]{String.valueOf(channelUserMapper.getKey()), String.valueOf(channelUserMapper.getUserKey())});
        dbHelper.close();
    }

    public boolean isChannelPresent(Integer channelKey) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(
                "SELECT COUNT(*) FROM channel WHERE channelKey=?", new String[]{String.valueOf(channelKey)});
        cursor.moveToFirst();
        boolean present = cursor.getInt(0) > 0;
        if (cursor != null) {
            cursor.close();
        }
        dbHelper.close();
        return present;
    }

    public void updateChannelLocalImageURI(Integer channelKey ,String channelLocalURI){
        ContentValues contentValues =  new ContentValues();
        contentValues.put(MobiComDatabaseHelper.CHANNEL_IMAGE_LOCAL_URI,channelLocalURI);
        int updatedRow =  dbHelper.getWritableDatabase().update(CHANNEL,contentValues, MobiComDatabaseHelper.CHANNEL_KEY + "=?", new String[]{String.valueOf(channelKey)});

    }

    public boolean isChannelUserPresent(Integer channelKey, String userId) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(
                "SELECT COUNT(*) FROM channel_User_X WHERE " + MobiComDatabaseHelper.CHANNEL_KEY + "=? and " + MobiComDatabaseHelper.USERID + "=?",
                new String[]{String.valueOf(channelKey), String.valueOf(userId)});
        cursor.moveToFirst();
        boolean present = cursor.getInt(0) > 0;
        if (cursor != null) {
            cursor.close();
        }
        dbHelper.close();
        return present;
    }
    public int removeMemberFromChannel(String clientGroupId, String userId) {
        Channel channel = getChannelByClientGroupId(clientGroupId);
        return removeMemberFromChannel(channel.getKey(), userId);
    }

    public int removeMemberFromChannel(Integer channelKey, String userId) {
        int deleteUser = 0;
        try {
            deleteUser = dbHelper.getWritableDatabase().delete(MobiComDatabaseHelper.CHANNEL_USER_X, "channelKey=? AND userId= ?", new String[]{String.valueOf(channelKey), userId});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deleteUser;
    }

    public int leaveMemberFromChannel(String clientGroupId, String userId) {
        Channel channel = getChannelByClientGroupId(clientGroupId);
        return leaveMemberFromChannel(channel.getKey(), userId);
    }

    public int leaveMemberFromChannel(Integer channelKey, String userId) {
        int deletedRows = 0;
        try {
            deletedRows = dbHelper.getWritableDatabase().delete(MobiComDatabaseHelper.CHANNEL_USER_X, "channelKey=? AND userId= ?", new String[]{String.valueOf(channelKey),userId });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deletedRows;
    }

    public int updateChannel(GroupInfoUpdate groupInfoUpdate) {
        int rowUpdated = 0;
        try {
            ContentValues values = new ContentValues();
            if(groupInfoUpdate != null){
                if(groupInfoUpdate.getNewName() != null){
                    values.put("channelName", groupInfoUpdate.getNewName());
                }
                if(groupInfoUpdate.getImageUrl() != null){
                    values.put("channelImageURL", groupInfoUpdate.getImageUrl());
                }
            }
            rowUpdated = dbHelper.getWritableDatabase().update("channel", values, "channelKey=" + groupInfoUpdate.getGroupId(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rowUpdated;
    }
    public int deleteChannel(Integer channelKey){
        int deletedRows = 0;
        try {
            deletedRows = dbHelper.getWritableDatabase().delete(MobiComDatabaseHelper.CHANNEL, "channelKey=?", new String[]{String.valueOf(channelKey)});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deletedRows;
    }

    public int deleteChannelUserMappers(Integer channelKey){
        int deletedRows = 0;
        try {
            deletedRows = dbHelper.getWritableDatabase().delete(MobiComDatabaseHelper.CHANNEL_USER_X, "channelKey=?", new String[]{String.valueOf(channelKey)});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deletedRows;
    }


    public Loader<Cursor> getSearchCursorForGroupsLoader(final String searchString) {

        return new CursorLoader(context, null, null, null, null, MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME + " asc") {
            @Override
            public Cursor loadInBackground() {

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor;

                StringBuffer stringBuffer = new StringBuffer();

                stringBuffer.append("SELECT ").append(MobiComDatabaseHelper._ID).append(",").append(MobiComDatabaseHelper.CHANNEL_KEY).append(",").append(MobiComDatabaseHelper.CLIENT_GROUP_ID).append(",").append(MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME).append(",").
                        append(MobiComDatabaseHelper.ADMIN_ID).append(",").append(MobiComDatabaseHelper.TYPE).append(",").append(MobiComDatabaseHelper.UNREAD_COUNT).append(",").append(MobiComDatabaseHelper.CHANNEL_IMAGE_URL).append(",").append(MobiComDatabaseHelper.CHANNEL_IMAGE_LOCAL_URI).
                        append(" FROM ").append(MobiComDatabaseHelper.CHANNEL);


                if (!TextUtils.isEmpty(searchString)) {
                    stringBuffer.append(" where " + MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME + " like '%" + searchString.replaceAll("'", "''") + "%'");
                }
                stringBuffer.append(" order by "+MobiComDatabaseHelper.CHANNEL_DISPLAY_NAME +" asc ");
                cursor = db.rawQuery(stringBuffer.toString(), null);

                return cursor;

            }
        };
    }



}
