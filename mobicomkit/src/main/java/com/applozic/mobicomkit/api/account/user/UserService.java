package com.applozic.mobicomkit.api.account.user;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.account.sync.SyncClientService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicomkit.feed.RegisteredUsersApiResponse;
import com.applozic.mobicomkit.feed.SyncApiResponse;
import com.applozic.mobicomkit.feed.SyncBlockUserApiResponse;
import com.applozic.mobicomkit.feed.SyncPxy;
import com.applozic.mobicomkit.sync.SyncUserBlockFeed;
import com.applozic.mobicomkit.sync.SyncUserBlockListFeed;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.contact.Contact;
import com.google.gson.reflect.TypeToken;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sunil on 17/3/16.
 */
public class UserService {

    private static final String TAG = "UserService";

    Context context;
    UserClientService userClientService;
    private static UserService userService;
    private MobiComUserPreference userPreference;
    private BaseContactService baseContactService;
    private SyncClientService syncClientService;

    private UserService(Context context) {
        this.context = context;
        userClientService = new UserClientService(context);
        userPreference = MobiComUserPreference.getInstance(context);
        baseContactService = new AppContactService(context);
        this.syncClientService = new SyncClientService(context);
    }

    public static UserService getInstance(Context context) {
        if (userService == null) {
            userService = new UserService(context);
        }
        return userService;
    }

    public synchronized void processSyncUserBlock() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SyncBlockUserApiResponse apiResponse = userClientService.getSyncUserBlockList(userPreference.getUserBlockSyncTime());
                    if (apiResponse != null && SyncBlockUserApiResponse.SUCCESS.equals(apiResponse.getStatus())) {
                        SyncUserBlockListFeed syncUserBlockListFeed = apiResponse.getResponse();
                        if (syncUserBlockListFeed != null) {
                            List<SyncUserBlockFeed> blockedToUserList = syncUserBlockListFeed.getBlockedToUserList();
                            List<SyncUserBlockFeed> blockedByUserList = syncUserBlockListFeed.getBlockedByUserList();
                            if (blockedToUserList != null && blockedToUserList.size() > 0) {
                                for (SyncUserBlockFeed syncUserBlockedFeed : blockedToUserList) {
                                    Contact contact = new Contact();
                                    if (syncUserBlockedFeed.getUserBlocked() != null && !TextUtils.isEmpty(syncUserBlockedFeed.getBlockedTo())) {
                                        if(baseContactService.isContactExists(syncUserBlockedFeed.getBlockedTo())){
                                            baseContactService.updateUserBlocked(syncUserBlockedFeed.getBlockedTo(),syncUserBlockedFeed.getUserBlocked());
                                        }else {
                                            contact.setContactType(Contact.ContactType.APPLOZIC.getValue());
                                            contact.setBlocked(syncUserBlockedFeed.getUserBlocked());
                                            contact.setUserId(syncUserBlockedFeed.getBlockedTo());
                                            baseContactService.upsert(contact);
                                            baseContactService.updateUserBlocked(syncUserBlockedFeed.getBlockedTo(),syncUserBlockedFeed.getUserBlocked());
                                        }
                                    }
                                }
                            }
                            if (blockedByUserList != null && blockedByUserList.size() > 0) {
                                for (SyncUserBlockFeed syncUserBlockByFeed : blockedByUserList) {
                                    Contact contact = new Contact();
                                    if (syncUserBlockByFeed.getUserBlocked() != null && !TextUtils.isEmpty(syncUserBlockByFeed.getBlockedBy())) {
                                        if(baseContactService.isContactExists(syncUserBlockByFeed.getBlockedBy())){
                                            baseContactService.updateUserBlockedBy(syncUserBlockByFeed.getBlockedBy(),syncUserBlockByFeed.getUserBlocked());
                                        }else {
                                            contact.setContactType(Contact.ContactType.APPLOZIC.getValue());
                                            contact.setBlockedBy(syncUserBlockByFeed.getUserBlocked());
                                            contact.setUserId(syncUserBlockByFeed.getBlockedBy());
                                            baseContactService.upsert(contact);
                                            baseContactService.updateUserBlockedBy(syncUserBlockByFeed.getBlockedBy(),syncUserBlockByFeed.getUserBlocked());
                                        }
                                    }
                                }
                            }
                        }
                        userPreference.setUserBlockSyncTime(apiResponse.getGeneratedAt());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    public ApiResponse processUserBlock(String userId, boolean block) {
        ApiResponse apiResponse = userClientService.userBlock(userId, block);
        if (apiResponse != null && apiResponse.isSuccess()) {
            baseContactService.updateUserBlocked(userId, block);
            return apiResponse;
        }
        return null;
    }

    public synchronized void processUserDetail(Set<UserDetail> userDetails) {
        if (userDetails != null && userDetails.size() > 0) {
            for (UserDetail userDetail : userDetails) {
                processUser(userDetail);
            }
        }
    }

    public synchronized void processUserDetails(String userId) {
        Set<String> userIds = new HashSet<String>();
        userIds.add(userId);
        processUserDetails(userIds);
    }

    public synchronized void processUserDetails(Set<String> userIds) {
        String response = userClientService.getUserDetails(userIds);
        if (!TextUtils.isEmpty(response)) {
            UserDetail[] userDetails = (UserDetail[]) GsonUtils.getObjectFromJson(response, UserDetail[].class);
            for (UserDetail userDetail : userDetails) {
                processUser(userDetail);
            }
        }
    }

    public synchronized void processUser(UserDetail userDetail) {
        processUser(userDetail, Contact.ContactType.APPLOZIC);
    }

    public synchronized void processUser(UserDetail userDetail, Contact.ContactType contactType) {
        Contact contact = new Contact();
        contact.setUserId(userDetail.getUserId());
        contact.setContactNumber(userDetail.getPhoneNumber());
        contact.setConnected(userDetail.isConnected());
        contact.setFullName(userDetail.getDisplayName());
        contact.setLastSeenAt(userDetail.getLastSeenAtTime());
        contact.setStatus(userDetail.getStatusMessage());
        contact.setUnreadCount(0);
        contact.setContactType(contactType.getValue());
        if (!TextUtils.isEmpty(userDetail.getImageLink())) {
            contact.setImageURL(userDetail.getImageLink());
        }
        baseContactService.upsert(contact);
    }

    public synchronized void processUserDetailsByContactNos(Set<String> contactNumbers){
         userClientService.postUserDetailsByContactNos(contactNumbers);
    }

    public synchronized String[] getOnlineUsers(int numberOfUser) {
        try {
            Map<String, String> userMapList = userClientService.getOnlineUserList(numberOfUser);
            if (userMapList != null && userMapList.size() > 0) {
                String[] userIdArray = new String[userMapList.size()];
                Set<String> userIds = new HashSet<String>();
                int i = 0;
                for (Map.Entry<String, String> keyValue : userMapList.entrySet()) {
                    userIdArray[i] = keyValue.getKey();
                    userIds.add(keyValue.getKey());
                    i++;
                }
                processUserDetails(userIds);
                return userIdArray;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void processUserDetailsByPhoneNumber(String response){
        if(!TextUtils.isEmpty(response)){
            List<UserDetail> userDetails = (List<UserDetail>) GsonUtils.getObjectFromJson(response, new TypeToken<List<UserDetail>>() {}.getType());
            for (UserDetail userDetail : userDetails) {
                processUserForDeviceContactSync(userDetail);
            }
        }
    }

    public synchronized void processUserForDeviceContactSync(UserDetail userDetail){
        Contact contact = new Contact();
        contact.setUserId(userDetail.getUserId());
        contact.setContactNumber(userDetail.getPhoneNumber());
        contact.setConnected(userDetail.isConnected());
        contact.setFullName(userDetail.getDisplayName());
        contact.setLastSeenAt(userDetail.getLastSeenAtTime());
        contact.setUnreadCount(0);
        contact.setContactType(Contact.ContactType.DEVICE_AND_APPLOZIC.getValue());
        if(!TextUtils.isEmpty(userDetail.getImageLink())){
            contact.setImageURL(userDetail.getImageLink());
        }
        baseContactService.upsert(contact);
    }


    public synchronized RegisteredUsersApiResponse getRegisteredUsersList(Long startTime, int pageSize) {
        String response = userClientService.getRegisteredUsers(startTime, pageSize);
        if (!TextUtils.isEmpty(response) && !MobiComKitConstants.ERROR.equals(response)) {
            RegisteredUsersApiResponse apiResponse = (RegisteredUsersApiResponse) GsonUtils.getObjectFromJson(response, RegisteredUsersApiResponse.class);
            if (apiResponse != null) {
                processUserDetail(apiResponse.getUsers());
                userPreference.setRegisteredUsersLastFetchTime(apiResponse.getLastFetchTime());
            }
            return apiResponse;
        }
        return null;
    }


    public void updateDisplayNameORImageLink( String displayName, String profileImageLink, String localURL, String status ){

        ApiResponse response = userClientService.updateDisplayNameORImageLink(displayName,profileImageLink,status);
        if(response.isSuccess()){
            Contact contact=   baseContactService.getContactById(MobiComUserPreference.getInstance(context).getUserId());
            contact.setFullName(displayName);
            contact.setImageURL(profileImageLink);
            contact.setLocalImageUrl(localURL);
            contact.setStatus(status);
            baseContactService.upsert(contact);
            Contact contact1=   baseContactService.getContactById(MobiComUserPreference.getInstance(context).getUserId());
            Log.i("UserService", contact1.getImageURL() + ", " +contact1.getDisplayName() + "," + contact.getStatus() );
        }
    }

    public void processContactSync() {
        Set<String> userIds = new HashSet<String>();
        SyncApiResponse apiResponse = syncClientService.getSyncCall(MobiComUserPreference.getInstance(context).getContactSyncTime(), SyncClientService.SyncType.CONTACT);
        if (apiResponse == null || apiResponse.getResponse() == null || apiResponse.getResponse().isEmpty()) {
            Log.i(TAG, "Contact Sync call response is empty.");
            return;
        }
        for (SyncPxy syncPxy: apiResponse.getResponse()) {
            userIds.add(syncPxy.getParam());
        }
        processUserDetails(userIds);
        MobiComUserPreference.getInstance(context).setContactSyncTime(apiResponse.getGeneratedAt());
    }

    public ApiResponse processUserReadConversation(){
        return  userClientService.getUserReadServerCall();
    }

}
