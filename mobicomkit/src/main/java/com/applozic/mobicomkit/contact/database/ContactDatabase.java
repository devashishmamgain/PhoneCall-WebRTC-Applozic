package com.applozic.mobicomkit.contact.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.database.MobiComDatabaseHelper;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.people.contact.Contact;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by adarsh on 9/7/15.
 */
public class ContactDatabase {

    public static final String CONTACT = "contact";
    private static final String TAG = "ContactDatabaseService";
    Context context = null;
    private MobiComUserPreference userPreferences;
    private MobiComDatabaseHelper dbHelper;

    public ContactDatabase(Context context) {
        this.context = context;
        this.userPreferences = MobiComUserPreference.getInstance(context);
        this.dbHelper = MobiComDatabaseHelper.getInstance(context);
    }


    public Contact getContact(Cursor cursor) {
        return getContact(cursor, null);
    }

    /**
     * Form a single contact from cursor
     *
     * @param cursor
     * @return
     */
    public Contact getContact(Cursor cursor, String primaryKeyAliash) {

        Contact contact = new Contact();
        contact.setFullName(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.FULL_NAME)));
        contact.setUserId(cursor.getString(cursor.getColumnIndex(primaryKeyAliash == null ? MobiComDatabaseHelper.USERID : primaryKeyAliash)));
        contact.setLocalImageUrl(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.CONTACT_IMAGE_LOCAL_URI)));
        contact.setImageURL(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.CONTACT_IMAGE_URL)));
        contact.setContactNumber(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.CONTACT_NO)));
        contact.setApplicationId(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.APPLICATION_ID)));
        Long connected = cursor.getLong(cursor.getColumnIndex(MobiComDatabaseHelper.CONNECTED));
        contact.setConnected(connected != 0 && connected.intValue() == 1);
        contact.setLastSeenAt(cursor.getLong(cursor.getColumnIndex(MobiComDatabaseHelper.LAST_SEEN_AT_TIME)));
        contact.setContactType(cursor.getInt(cursor.getColumnIndex(MobiComDatabaseHelper.CONTACT_TYPE)));
        /*Boolean applozicType = (cursor.getInt(cursor.getColumnIndex(MobiComDatabaseHelper.APPLOZIC_TYPE)) == 1);
        contact.setApplozicType(applozicType);*/
        contact.processContactNumbers(context);
        contact.setUnreadCount(cursor.getInt(cursor.getColumnIndex(MobiComDatabaseHelper.UNREAD_COUNT)));
        Boolean userBlocked = (cursor.getInt(cursor.getColumnIndex(MobiComDatabaseHelper.BLOCKED)) == 1);
        contact.setBlocked(userBlocked);
        Boolean userBlockedBy = (cursor.getInt(cursor.getColumnIndex(MobiComDatabaseHelper.BLOCKED_BY)) == 1);
        contact.setBlockedBy(userBlockedBy);
        contact.setStatus(cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.STATUS)));
        String phoneDisplayName =  getContactName(contact.getFormattedContactNumber());
        contact.setPhoneDisplayName(!TextUtils.isEmpty(phoneDisplayName)?phoneDisplayName:cursor.getString(cursor.getColumnIndex(MobiComDatabaseHelper.PHONE_CONTACT_DISPLAY_NAME)));
        return contact;
    }


    private String getContactName(String contactNumber) {
        String contactName = null;
        try{
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contactNumber));
            Cursor cursor = context.getContentResolver().query(uri, new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME },null,null, null);
            if (cursor != null && cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            }
            if (cursor != null) {
                cursor.close();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return  contactName;
    }

    /**
     * Form a single contact details from cursor
     *
     * @param cursor
     * @return
     */
    public List<Contact> getContactList(Cursor cursor) {
        List<Contact> contactList = new ArrayList<Contact>();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            do {
                contactList.add(getContact(cursor));
            } while (cursor.moveToNext());
        }
        return contactList;
    }

    public List<Contact> getAllContactListExcludingLoggedInUser() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String structuredNameWhere = MobiComDatabaseHelper.USERID + " != ? AND " + MobiComDatabaseHelper.CONTACT_TYPE + " == 2   AND " + MobiComDatabaseHelper.BLOCKED + " == 0 AND "+ MobiComDatabaseHelper.BLOCKED_BY + " == 0";
        Cursor cursor = db.query(CONTACT, null, structuredNameWhere, new String[]{MobiComUserPreference.getInstance(context).getUserId()}, null, null, MobiComDatabaseHelper.FULL_NAME + " asc");
        List<Contact> contactList = getContactList(cursor);
        cursor.close();
        dbHelper.close();
        return contactList;
    }

    public List<Contact> getContacts(Contact.ContactType contactType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String structuredNameWhere = MobiComDatabaseHelper.CONTACT_TYPE + " = ?";
        Cursor cursor = db.query(CONTACT, null, structuredNameWhere, new String[]{String.valueOf(contactType.getValue())}, null, null, MobiComDatabaseHelper.FULL_NAME + " asc");
        List<Contact> contactList = getContactList(cursor);
        cursor.close();
        dbHelper.close();
        return contactList;
    }

    public List<Contact> getAllContact() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(CONTACT, null, null, null, null, null, MobiComDatabaseHelper.FULL_NAME + " asc");
        List<Contact> contactList = getContactList(cursor);
        cursor.close();
        dbHelper.close();
        return contactList;
    }

    public Contact getContactById(String id) {
        String structuredNameWhere = MobiComDatabaseHelper.USERID + " =?";
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(CONTACT, null, structuredNameWhere, new String[]{id}, null, null, null);
        Contact contact = null;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                contact = getContact(cursor);
            }
            cursor.close();
        }
        dbHelper.close();
        return contact;

    }

    public void updateContact(Contact contact) {
        Contact existingContact = getContactById(contact.getUserId());
        if (contact.getContactType() != null && !contact.getContactType().equals(existingContact.getContactType())) {
            contact.setContactType(Contact.ContactType.DEVICE_AND_APPLOZIC.getValue());
        }
        ContentValues contentValues = prepareContactValues(contact);
        dbHelper.getWritableDatabase().update(CONTACT, contentValues, MobiComDatabaseHelper.USERID + "=?", new String[]{contact.getUserId()});
        dbHelper.close();
    }

    public void updateLocalImageUri(Contact contact){
        ContentValues contentValues =  new ContentValues();
        contentValues.put(MobiComDatabaseHelper.CONTACT_IMAGE_LOCAL_URI,contact.getLocalImageUrl());
        int updatedRow =  dbHelper.getWritableDatabase().update(CONTACT,contentValues, MobiComDatabaseHelper.USERID + "=?", new String[]{contact.getUserId()});
    }

    public void updateContactByPhoneNumber(Contact contact) {
        try {
            if (contact == null || TextUtils.isEmpty(contact.getFormattedContactNumber())) {
                return;
            }

            if (contact.getContactType() == Contact.ContactType.APPLOZIC.getValue() ||  contact.getContactType() == Contact.ContactType.DEVICE_AND_APPLOZIC.getValue()) {
                //applozic contact
                if (isContactPresent(contact.getFormattedContactNumber(), Contact.ContactType.DEVICE)) {
                    List<Contact> contactDevice = getContactsByContactNumberAndType(contact.getFormattedContactNumber(), Contact.ContactType.DEVICE.getValue());
                    for (Contact c : contactDevice) {
                        contact.setPhoneDisplayName(c.getPhoneDisplayName());
                    }
                    contact.setContactType(Contact.ContactType.DEVICE_AND_APPLOZIC.getValue());
                }
                saveOrUpdate(contact);
            } else if (contact.getContactType() == Contact.ContactType.DEVICE.getValue()) {
                //device contact
                if (isContactPresent(contact.getFormattedContactNumber(), Contact.ContactType.DEVICE_AND_APPLOZIC)) {//do nothing
                } else if (isContactPresent(contact.getFormattedContactNumber(), Contact.ContactType.APPLOZIC)) {
                    List<Contact> contactListApplozic = getContactsByContactNumberAndType(contact.getFormattedContactNumber(), Contact.ContactType.APPLOZIC.getValue());
                    for (Contact c : contactListApplozic) {
                        c.setContactType(Contact.ContactType.DEVICE_AND_APPLOZIC.getValue());
                        updateContact(c);
                    }
                } else if (isContactPresent(contact.getFormattedContactNumber(), Contact.ContactType.DEVICE)) {
                    saveOrUpdate(contact);
                } else {
                    addContact(contact);
                }
            } else {
                saveOrUpdate(contact);
            }

            if (isContactPresent(contact.getFormattedContactNumber(), Contact.ContactType.DEVICE_AND_APPLOZIC)) {
                if(!TextUtils.isEmpty(contact.getPhoneDisplayName())){
                    updatePhoneContactDisplayName(contact.getFormattedContactNumber(),contact.getPhoneDisplayName(),Contact.ContactType.DEVICE_AND_APPLOZIC.getValue());
                }
                deleteContactByPhoneNumber(contact.getFormattedContactNumber(), Contact.ContactType.DEVICE.getValue());
                deleteContactByPhoneNumber(contact.getFormattedContactNumber(), Contact.ContactType.APPLOZIC.getValue());
                //deleteContactByLookUpKey(contact.getContactNumber());
            }

            dbHelper.close();
        } catch (Throwable t) {
        }
    }

    public void saveOrUpdate(Contact contact) {
        Contact existingContact = getContactById(contact.getUserId());
        if (existingContact == null) {
            addContact(contact);
        } else {
            if (contact.getContactType() != Contact.ContactType.DEVICE_AND_APPLOZIC.getValue()) {
                contact.setContactType(existingContact.getContactType());
            }
            updateContact(contact);
        }
    }

    public void deleteContactByPhoneNumber(String conatctNumber ,int type ) {
        dbHelper.getWritableDatabase().delete(CONTACT, MobiComDatabaseHelper.CONTACT_NO + "=? AND "+MobiComDatabaseHelper.CONTACT_TYPE +"=?" , new String[]{conatctNumber, String.valueOf(type)});
        dbHelper.close();
    }

    public  List<Contact> getContactsByContactNumberAndType(String contactNumber,int contactType){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String structuredNameWhere = MobiComDatabaseHelper.CONTACT_NO + " = ? AND "+MobiComDatabaseHelper.CONTACT_TYPE + " = ? ";
        Cursor cursor = db.query(CONTACT, null, structuredNameWhere, new String[]{contactNumber,String.valueOf(contactType)}, null, null, null);
        List<Contact> contactList = getContactList(cursor);
        cursor.close();
        dbHelper.close();
        return contactList;
    }

    public void deleteContactByLookUpKey(String contactNumber){
        dbHelper.getWritableDatabase().delete(CONTACT, MobiComDatabaseHelper.CONTACT_NO + "=? AND "+MobiComDatabaseHelper.USERID+" like '%lkupkey-%'", new String[]{contactNumber});
        dbHelper.close();
    }

    public void deleteContactByPhoneNumber(Contact contact) {
        dbHelper.getWritableDatabase().delete(CONTACT, MobiComDatabaseHelper.CONTACT_NO + "=?", new String[]{contact.getFormattedContactNumber()});
        dbHelper.close();
    }

    public void updateConnectedOrDisconnectedStatus(String userId, Date date, boolean connected) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MobiComDatabaseHelper.CONNECTED, connected ? 1 : 0);
        contentValues.put(MobiComDatabaseHelper.LAST_SEEN_AT_TIME, date.getTime());

        try {
            dbHelper.getWritableDatabase().update(CONTACT, contentValues, MobiComDatabaseHelper.USERID + "=?", new String[]{userId});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbHelper.close();
        }
    }

    public void updateLastSeenTimeAt(String userId, long lastSeenTime) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MobiComDatabaseHelper.LAST_SEEN_AT_TIME, lastSeenTime);
            dbHelper.getWritableDatabase().update(CONTACT, contentValues, MobiComDatabaseHelper.USERID + "=?", new String[]{userId});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbHelper.close();
        }
    }

    public void updateUserBlockStatus(String userId, boolean userBlocked) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MobiComDatabaseHelper.BLOCKED, userBlocked ? 1 : 0);
            int row = dbHelper.getWritableDatabase().update(CONTACT, contentValues, MobiComDatabaseHelper.USERID + "=?", new String[]{userId});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbHelper.close();
        }
    }

    public void updateUserBlockByStatus(String userId, boolean userBlockedBy) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MobiComDatabaseHelper.BLOCKED_BY, userBlockedBy ? 1 : 0);
            int row = dbHelper.getWritableDatabase().update(CONTACT, contentValues, MobiComDatabaseHelper.USERID + "=?", new String[]{userId});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbHelper.close();
        }
    }

    public void addContact(Contact contact) {
        contact.processContactNumbers(context);
        if (contact.getContactType() == null) {
            contact.setContactType(Contact.ContactType.APPLOZIC.getValue());
        }

        ContentValues contentValues = prepareContactValues(contact);
        dbHelper.getWritableDatabase().insert(CONTACT, null, contentValues);
        dbHelper.close();

        /*if (isContactPresent(contact.getFormattedContactNumber(), Contact.ContactType.DEVICE_AND_APPLOZIC)) {
            deleteContactByPhoneNumber(contact.getFormattedContactNumber(), Contact.ContactType.DEVICE.getValue());
            deleteContactByPhoneNumber(contact.getFormattedContactNumber(), Contact.ContactType.APPLOZIC.getValue());
            deleteContactByLookUpKey(contact.getContactNumber());
        }*/
    }

    public ContentValues prepareContactValues(Contact contact) {
        ContentValues contentValues = new ContentValues();
        //Contact oldContact =  new Contact();
        contact.processContactNumbers(context);
        contentValues.put(MobiComDatabaseHelper.FULL_NAME, getFullNameForUpdate(contact));
        if(!TextUtils.isEmpty(contact.getFormattedContactNumber())){
            contentValues.put(MobiComDatabaseHelper.CONTACT_NO, contact.getFormattedContactNumber());
            //oldContact =  getContactByPhoneNo(contact.getFormattedContactNumber());
        }
        if (!TextUtils.isEmpty(contact.getImageURL())) {
            contentValues.put(MobiComDatabaseHelper.CONTACT_IMAGE_URL, contact.getImageURL());
        }
        if (!TextUtils.isEmpty(contact.getLocalImageUrl())) {
            contentValues.put(MobiComDatabaseHelper.CONTACT_IMAGE_LOCAL_URI, contact.getLocalImageUrl());
        }
        contentValues.put(MobiComDatabaseHelper.USERID, contact.getUserId());
        if (!TextUtils.isEmpty(contact.getEmailId())) {
            contentValues.put(MobiComDatabaseHelper.EMAIL, contact.getEmailId());
        }
        if (!TextUtils.isEmpty(contact.getApplicationId())) {
            contentValues.put(MobiComDatabaseHelper.APPLICATION_ID, contact.getApplicationId());
        }

        contentValues.put(MobiComDatabaseHelper.CONNECTED, contact.isConnected() ? 1 : 0);
        if (contact.getLastSeenAt() != 0) {
            contentValues.put(MobiComDatabaseHelper.LAST_SEEN_AT_TIME, contact.getLastSeenAt());
        }
        if (contact.getUnreadCount() != null && contact.getUnreadCount() != 0) {
            contentValues.put(MobiComDatabaseHelper.UNREAD_COUNT, contact.getUnreadCount());
        }

        if (contact.isBlocked()) {
            contentValues.put(MobiComDatabaseHelper.BLOCKED, contact.isBlocked());
        }
        if (contact.isBlockedBy()) {
            contentValues.put(MobiComDatabaseHelper.BLOCKED_BY, contact.isBlockedBy());
        }
        if (!TextUtils.isEmpty(contact.getPhoneDisplayName())) {
            contentValues.put(MobiComDatabaseHelper.PHONE_CONTACT_DISPLAY_NAME, contact.getPhoneDisplayName());
        }
        contentValues.put(MobiComDatabaseHelper.STATUS, contact.getStatus());

        if (contact.getContactType() != null) {
            contentValues.put(MobiComDatabaseHelper.CONTACT_TYPE, contact.getContactType());
            contentValues.put(MobiComDatabaseHelper.APPLOZIC_TYPE, contact.isApplozicType() ? 1 : 0);
        }

        /*if (oldContact != null && oldContact.getContactType() != contact.getContactType()) {
            contentValues.put(MobiComDatabaseHelper.CONTACT_TYPE, Contact.ContactType.DEVICE_AND_APPLOZIC.getValue());
            contentValues.put(MobiComDatabaseHelper.APPLOZIC_TYPE, 1);
        } else {
            contentValues.put(MobiComDatabaseHelper.CONTACT_TYPE, contact.getContactType());
            contentValues.put(MobiComDatabaseHelper.APPLOZIC_TYPE, contact.isApplozicType() ? 1 : 0);
        }*/

        return contentValues;
    }

    /**
     * This method will return full name of contact to be updated.
     * This is require to avoid updating fullname back to userId in case fullname is not set while updating contact.
     * @param contact
     * @return
     */
    private String getFullNameForUpdate(Contact contact) {

        String fullName = contact.getDisplayName();
        if (TextUtils.isEmpty(contact.getFullName())) {
            Contact contactFromDB = getContactById(contact.getUserId());
            if (contactFromDB != null) {
                fullName = contactFromDB.getFullName();
            }
        }
        return fullName;
    }

    private void updatePhoneContactDisplayName(String contactNumber,String displayName,int type){
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MobiComDatabaseHelper.PHONE_CONTACT_DISPLAY_NAME, displayName);
            int row = dbHelper.getWritableDatabase().update(CONTACT, contentValues, MobiComDatabaseHelper.CONTACT_NO + "=? AND "+MobiComDatabaseHelper.CONTACT_TYPE +"=?", new String[]{contactNumber, String.valueOf(type)});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbHelper.close();
        }
    }

    public boolean isContactPresent(String userId) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(
                "SELECT COUNT(*) FROM contact WHERE userId = ?",
                new String[]{userId});
        cursor.moveToFirst();
        boolean present = cursor.getInt(0) > 0;
        if (cursor != null) {
            cursor.close();
        }
        return present;
    }

    public void addAllContact(List<Contact> contactList) {
        for (Contact contact : contactList) {
            addContact(contact);
        }
    }

    public void deleteContact(Contact contact) {
        deleteContactById(contact.getUserId());
    }

    public void deleteContactById(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(CONTACT, "userId=?", new String[]{id});
        dbHelper.close();
    }

    public void deleteAllContact(List<Contact> contacts) {
        for (Contact contact : contacts) {
            deleteContact(contact);
        }
    }

    public int getChatUnreadCount(){
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            final Cursor cursor = db.rawQuery("SELECT COUNT(DISTINCT (userId)) FROM contact WHERE unreadCount > 0 ", null);
            cursor.moveToFirst();
            int chatCount = 0;
            if (cursor.getCount() > 0) {
                chatCount = cursor.getInt(0);
            }
            cursor.close();
            dbHelper.close();
            return chatCount;
        } catch (Exception ex) {
        }
        return 0;
    }

    public int getGroupUnreadCount(){
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            final Cursor cursor = db.rawQuery("SELECT COUNT(DISTINCT (channelKey)) FROM channel WHERE unreadCount > 0 ", null);
            cursor.moveToFirst();
            int groupCount = 0;
            if (cursor.getCount() > 0) {
                groupCount = cursor.getInt(0);
            }
            cursor.close();
            dbHelper.close();
            return groupCount;
        } catch (Exception ex) {
        }
        return 0;
    }

    public int getContactNumberCount(String  contactNumber) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            final Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM contact where  "+MobiComDatabaseHelper.CONTACT_NO+" = ? ", new String[]{contactNumber});
            cursor.moveToFirst();
            int count = 0;
            if (cursor.getCount() > 0) {
                count = cursor.getInt(0);
            }
            cursor.close();
            dbHelper.close();
            return count;
        } catch (Exception ex) {
        }
        return 0;
    }

    public boolean isContactPresent(String contactNumber, Contact.ContactType contactType){
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            final Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM contact where  " + MobiComDatabaseHelper.CONTACT_NO + " = ?  AND " + MobiComDatabaseHelper.CONTACT_TYPE + " = ? ", new String[]{contactNumber,String.valueOf(contactType.getValue())});

            cursor.moveToFirst();
            boolean present = cursor.getInt(0) > 0;

            if (cursor != null) {
                cursor.close();
            }
            dbHelper.close();
            return present;

        } catch (Exception ex) {
        }
        return false;
    }

    public Loader<Cursor> getSearchCursorLoader(final String searchString, final int totalNumber,final String[] userIdArray) {

        return new CursorLoader(context, null, null, null, null, MobiComDatabaseHelper.DISPLAY_NAME + " asc") {
            @Override
            public Cursor loadInBackground() {

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor;

                String query = "select userId as _id, fullName, contactNO, contactType,applozicType, " +
                        "displayName,contactImageURL,contactImageLocalURI,email," +
                        "applicationId,connected,lastSeenAt,unreadCount,blocked," +
                        "blockedBy,status,phoneContactDisplayName from " + CONTACT;

                if (totalNumber > 0) {
                    String placeHolderString = Utils.makePlaceHolders(userIdArray.length);
                    if (!TextUtils.isEmpty(searchString)) {
                        query = query + " where fullName like '%" + searchString.replaceAll("'","''") + "%' and  userId  IN (" + placeHolderString + ")";
                    } else {
                        query = query + " where userId IN (" + placeHolderString + ")";
                    }
                    query = query + " order by connected desc,lastSeenAt desc limit " + totalNumber;

                    cursor = db.rawQuery(query, userIdArray);
                } else {
                    if (!TextUtils.isEmpty(searchString)) {
                        query = query + " where phoneContactDisplayName like '%" + searchString.replaceAll("'","''") + "%' AND contactType != 0 AND userId != '"+ userPreferences.getUserId()+"'";
                    }else {
                        query = query + " where  contactType != 0 AND userId != '"+ userPreferences.getUserId()+"'";
                    }
                    query = query + " order by applozicType desc, phoneContactDisplayName COLLATE NOCASE,userId COLLATE NOCASE asc ";
                    cursor = db.rawQuery(query, null);
                }

                return cursor;

            }
        };
    }

    //Get contacts from device

    public Contact getContactByPhoneNo(String contactNO) {
        if(TextUtils.isEmpty(contactNO)){
            return null;
        }

        String structuredNameWhere = "contactNO = ?";
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(CONTACT, null, structuredNameWhere, new String[]{contactNO}, null, null, null);
        Contact contact = null;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                contact = getContact(cursor);
            }
            cursor.close();
        }
        dbHelper.close();
        return contact;
    }
}