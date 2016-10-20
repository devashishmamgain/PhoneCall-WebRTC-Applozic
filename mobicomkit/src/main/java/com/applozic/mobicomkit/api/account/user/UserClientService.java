package com.applozic.mobicomkit.api.account.user;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.api.HttpRequestUtils;
import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.conversation.ApplozicMqttIntentService;
import com.applozic.mobicomkit.api.conversation.database.MessageDatabaseService;
import com.applozic.mobicomkit.database.MobiComDatabaseHelper;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicomkit.feed.SyncBlockUserApiResponse;
import com.applozic.mobicomkit.feed.UserDetailListFeed;
import com.applozic.mobicommons.json.GsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by devashish on 24/12/14.
 */
public class UserClientService extends MobiComKitClientService {

    private static final String TAG = "UserClientService";
    public static final String SHARED_PREFERENCE_VERSION_UPDATE_KEY = "mck.version.update";
    public static final String PHONE_NUMBER_UPDATE_URL = "/rest/ws/registration/phone/number/update";
    public static final String NOTIFY_CONTACTS_ABOUT_JOINING_MT = "/rest/ws/registration/notify/contacts";
    public static final String VERIFICATION_CONTACT_NUMBER_URL = "/rest/ws/verification/number";
    public static final String VERIFICATION_CODE_CONTACT_NUMBER_URL = "/rest/ws/verification/code";
    public static final String APP_VERSION_UPDATE_URL = "/rest/ws/register/version/update";
    public static final String SETTING_UPDATE_URL = "/rest/ws/setting/single/update";
    public static final String TIMEZONE_UPDATAE_URL = "/rest/ws/setting/updateTZ";
    public static final String USER_INFO_URL = "/rest/ws/user/info?";
    public static final Short MOBICOMKIT_VERSION_CODE = 106;
    public static final String USER_DISPLAY_NAME_UPDATE = "/rest/ws/user/name?";
    public static final String BLOCK_USER_URL = "/rest/ws/user/block";
    public static final String BLOCK_USER_SYNC_URL = "/rest/ws/user/blocked/sync";
    public static final String UNBLOCK_USER_SYNC_URL = "/rest/ws/user/unblock";
    public static final String USER_DETAILS_URL = "/rest/ws/user/detail?";
    public static final String ONLINE_USER_LIST_URL = "/rest/ws/user/ol/list";
    public static final String REGISTERED_USER_LIST_URL = "/rest/ws/user/filter";
    public static final String USER_PROFILE_UPDATE_URL = "/rest/ws/user/update";
    public static final String USER_DETAILS_LIST_POST_URL = "/rest/ws/user/detail";
    public static final String USER_READ_URL = "/rest/ws/user/read";

    public static final int BATCH_SIZE = 60;

    private HttpRequestUtils httpRequestUtils;

    public UserClientService(Context context) {
        super(context);
        this.httpRequestUtils = new HttpRequestUtils(context);
    }

    public String getPhoneNumberUpdateUrl() {
        return getBaseUrl() + PHONE_NUMBER_UPDATE_URL;
    }

    public String getUserProfileUpdateUrl() {
        return getBaseUrl() + USER_PROFILE_UPDATE_URL;
    }

    public String getNotifyContactsAboutJoiningMt() {
        return getBaseUrl() + NOTIFY_CONTACTS_ABOUT_JOINING_MT;
    }

    public String getVerificationContactNumberUrl() {
        return getBaseUrl() + VERIFICATION_CONTACT_NUMBER_URL;
    }

    public String getVerificationCodeContactNumberUrl() {
        return getBaseUrl() + VERIFICATION_CODE_CONTACT_NUMBER_URL;
    }

    public String getAppVersionUpdateUrl() {
        return getBaseUrl() + APP_VERSION_UPDATE_URL;
    }

    public String getUpdateUserDisplayNameUrl() {
        return getBaseUrl() + USER_DISPLAY_NAME_UPDATE;
    }

    public String getSettingUpdateUrl() {
        return getBaseUrl() + SETTING_UPDATE_URL;
    }

    public String getTimezoneUpdataeUrl() {
        return getBaseUrl() + TIMEZONE_UPDATAE_URL;
    }

    public String getUserInfoUrl() {
        return getBaseUrl() + USER_INFO_URL;
    }

    public String getBlockUserUrl() {
        return getBaseUrl() + BLOCK_USER_URL;
    }

    public String getBlockUserSyncUrl() {
        return getBaseUrl() + BLOCK_USER_SYNC_URL;
    }

    public String getUnBlockUserSyncUrl() {
        return getBaseUrl() + UNBLOCK_USER_SYNC_URL;
    }
    public String getUserDetailsListUrl() {
        return getBaseUrl() + USER_DETAILS_URL;
    }

    public String getOnlineUserListUrl() {
        return getBaseUrl() + ONLINE_USER_LIST_URL;
    }

    public String getRegisteredUserListUrl() {
        return getBaseUrl() + REGISTERED_USER_LIST_URL;
    }

    public String getUserReadUrl() {
        return getBaseUrl() + USER_READ_URL;
    }

    public void logout() {
        logout(false);
    }

    public void logout(boolean fromLogin) {
        MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(context);
        final String userKeyString = mobiComUserPreference.getSuUserKeyString();
        String url = mobiComUserPreference.getUrl();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        mobiComUserPreference.clearAll();
        MessageDatabaseService.recentlyAddedMessage.clear();
        MobiComDatabaseHelper.getInstance(context).delDatabase();

        mobiComUserPreference.setUrl(url);

        if (!fromLogin) {
            Intent intent = new Intent(context, ApplozicMqttIntentService.class);
            intent.putExtra(ApplozicMqttIntentService.USER_KEY_STRING, userKeyString);
            context.startService(intent);
        }
    }

    public String updateTimezone(String osuUserKeyString) {
        //Note: This can be used if user decides to change the timezone
        String response = null;
        try {
            response = httpRequestUtils.getStringFromUrl(getTimezoneUpdataeUrl() + "?suUserKeyString=" + osuUserKeyString +
                    "&timeZone=" + URLEncoder.encode(TimeZone.getDefault().getID(), "UTF-8"));
            Log.i(TAG, "Response from sendDeviceTimezoneToServer : " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public boolean sendVerificationCodeToServer(String verificationCode) {
        try {
            String response = httpRequestUtils.getResponse(getCredentials(), getVerificationCodeContactNumberUrl() + "?verificationCode=" + verificationCode, "application/json", "application/json");
            JSONObject json = new JSONObject(response);
            return json.has("code") && json.get("code").equals("200");
        } catch (Exception e) {
            Log.e("Verification Code", "Got Exception while submitting verification code to server: " + e);
        }
        return false;
    }

    public void updateCodeVersion(final String deviceKeyString) {
        String url = getAppVersionUpdateUrl() + "?appVersionCode=" + MOBICOMKIT_VERSION_CODE + "&deviceKey=" + deviceKeyString;
        String response = httpRequestUtils.getResponse(getCredentials(), url, "text/plain", "text/plain");
        Log.i(TAG, "Version update response: " + response);

    }

    public String updatePhoneNumber(String contactNumber) throws UnsupportedEncodingException {
        return httpRequestUtils.getResponse(getCredentials(), getPhoneNumberUpdateUrl() + "?phoneNumber=" + URLEncoder.encode(contactNumber, "UTF-8"), "text/plain", "text/plain");
    }

    public void notifyFriendsAboutJoiningThePlatform() {
        String response = httpRequestUtils.getResponse(getCredentials(), getNotifyContactsAboutJoiningMt(), "text/plain", "text/plain");
        Log.i(TAG, "Response for notify contact about joining MT: " + response);
    }

    public String sendPhoneNumberForVerification(String contactNumber, String countryCode, boolean viaSms) {
        try {
            String viaSmsParam = "";
            if (viaSms) {
                viaSmsParam = "&viaSms=true";
            }
            return httpRequestUtils.getResponse(getCredentials(), getVerificationContactNumberUrl() + "?countryCode=" + countryCode + "&contactNumber=" + URLEncoder.encode(contactNumber, "UTF-8") + viaSmsParam, "application/json", "application/json");
        } catch (Exception e) {
            Log.e("Verification Code", "Got Exception while submitting contact number for verification to server: " + e);
        }
        return null;
    }

    public void updateSetting(final String key, final String value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                   /* List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("key", key));
                    nameValuePairs.add(new BasicNameValuePair("value", value));
                    String response = httpRequestUtils.postData(getCredentials(), getSettingUpdateUrl(), "text/plain", "text/plain", null);
                    Log.i(TAG, "Response from setting update : " + response);*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public Map<String, String> getUserInfo(Set<String> userIds) throws JSONException, UnsupportedEncodingException {

        if (userIds == null && userIds.isEmpty()) {
            return new HashMap<>();
        }

        String userIdParam = "";
        for (String userId : userIds) {
            userIdParam += "&userIds" + "=" + URLEncoder.encode(userId, "UTF-8");
        }

        String response = httpRequestUtils.getResponse(getCredentials(), getUserInfoUrl() + userIdParam, "application/json", "application/json");
        Log.i(TAG, "Response: " + response);

        JSONObject jsonObject = new JSONObject(response);

        Map<String, String> info = new HashMap<String, String>();

        Iterator iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            String value = jsonObject.getString(key);
            info.put(key, value);
        }
        return info;
    }

    public void updateUserDisplayName(final String userId, final String displayName) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String parameters = "";
                try {
                    if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(displayName)) {
                        parameters = "userId=" + URLEncoder.encode(userId, "UTF-8") + "&displayName=" + URLEncoder.encode(displayName, "UTF-8");
                        String response = httpRequestUtils.getResponse(getCredentials(), getUpdateUserDisplayNameUrl() + parameters, "application/json", "application/json");

                        ApiResponse apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
                        Log.i(TAG, " Update display name Response :" + apiResponse.getStatus());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        thread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    public ApiResponse userBlock(String userId, boolean block) {
        String response = "";
        ApiResponse apiResponse = null;
        try {
            if (!TextUtils.isEmpty(userId)) {
                response = httpRequestUtils.getResponse(getCredentials(), (block ? getBlockUserUrl() : getUnBlockUserSyncUrl()) + "?userId=" + URLEncoder.encode(userId, "UTF-8"), "application/json", "application/json");
                apiResponse = (ApiResponse) GsonUtils.getObjectFromJson(response, ApiResponse.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    public SyncBlockUserApiResponse getSyncUserBlockList(String lastSyncTime){
        try {
            String url = getBlockUserSyncUrl() + "?lastSyncTime=" + lastSyncTime;
            String response = httpRequestUtils.getResponse(getCredentials(), url, "application/json", "application/json");

            if (response == null || TextUtils.isEmpty(response) || response.equals("UnAuthorized Access")) {
                return null;
            }
            return (SyncBlockUserApiResponse) GsonUtils.getObjectFromJson(response, SyncBlockUserApiResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getUserDetails(Set<String> userIds) {
        try {
            if (userIds != null && userIds.size() > 0) {
                String response = "";
                String userIdParam = "";
                for (String userId : userIds) {
                    userIdParam += "&userIds" + "=" + URLEncoder.encode(userId, "UTF-8");
                }
                response = httpRequestUtils.getResponse(getCredentials(), getUserDetailsListUrl() + userIdParam, "application/json", "application/json");
                Log.i(TAG, "User details response is :" + response);
                if (TextUtils.isEmpty(response) || response.contains("<html>")) {
                    return null;
                }
                return response;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String postUserDetailsByContactNos(Set<String> phoneNos) {
        try {
            if (phoneNos != null && phoneNos.size() > 0) {
                List<String> phoneNumberList = new ArrayList<>();
                String response = "";
                int count = 0;
                for (String phoneNo : phoneNos) {
                    count++;
                    phoneNumberList.add(phoneNo);
                    if( count% BATCH_SIZE==0){
                        UserDetailListFeed userDetailListFeed = new UserDetailListFeed();
                        userDetailListFeed.setContactSync(true);
                        userDetailListFeed.setPhoneNumberList(phoneNumberList);
                        String jsonFromObject = GsonUtils.getJsonFromObject(userDetailListFeed, userDetailListFeed.getClass());
                        Log.i(TAG,"Sending json:" + jsonFromObject);
                        response = httpRequestUtils.postData(getCredentials(), getUserDetailsListPostUrl() + "?contactSync=true", "application/json", "application/json", jsonFromObject);
                        phoneNumberList =  new ArrayList<String>();
                        if(!TextUtils.isEmpty(response)){
                            UserService.getInstance(context).processUserDetailsByPhoneNumber(response);
                        }
                    }
                }
                if(!phoneNumberList.isEmpty()&& phoneNumberList.size()>0) {
                    UserDetailListFeed userDetailListFeed = new UserDetailListFeed();
                    userDetailListFeed.setContactSync(true);
                    userDetailListFeed.setPhoneNumberList(phoneNumberList);
                    String jsonFromObject = GsonUtils.getJsonFromObject(userDetailListFeed, userDetailListFeed.getClass());
                    response = httpRequestUtils.postData(getCredentials(), getUserDetailsListPostUrl() + "?contactSync=true", "application/json", "application/json", jsonFromObject);

                    Log.i(TAG, "User details response is :" + response);
                    if (TextUtils.isEmpty(response) || response.contains("<html>")) {
                        return null;
                    }

                    if (!TextUtils.isEmpty(response)) {
                        UserService.getInstance(context).processUserDetailsByPhoneNumber(response);
                    }
                }
                return response;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUserDetailsListPostUrl() {
        return getBaseUrl() + USER_DETAILS_LIST_POST_URL;
    }

    public Map<String, String> getOnlineUserList(int numberOfUser) {
        Map<String, String> info = new HashMap<String, String>();
        try {
            String response = httpRequestUtils.getResponse(getCredentials(), getOnlineUserListUrl() + "?startIndex=0&pageSize=" + numberOfUser, "application/json", "application/json");
            if (response != null && !MobiComKitConstants.ERROR.equals(response)) {
                JSONObject jsonObject = new JSONObject(response);
                Iterator iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String key = (String) iterator.next();
                    String value = jsonObject.getString(key);
                    info.put(key, value);
                }
                return info;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    public String getRegisteredUsers(Long startTime, int pageSize) {
        String response = null;
        try {
            String url = "?pageSize=" + pageSize;
            if (startTime > 0) {
                url = url + "&startTime=" + startTime;
            }
            response = httpRequestUtils.getResponse(getCredentials(), getRegisteredUserListUrl() + url, "application/json", "application/json");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public ApiResponse updateDisplayNameORImageLink(String displayName, String profileImageLink,String status)  {

        JSONObject jsonFromObject = new JSONObject();
        try {
            User user = new User();
            if(!TextUtils.isEmpty(displayName) ){
                jsonFromObject.put("displayName",displayName);
            }
            if(!TextUtils.isEmpty(profileImageLink) ){
                jsonFromObject.put("imageLink",profileImageLink);
            }
            if(!TextUtils.isEmpty(status) ){
                jsonFromObject.put("statusMessage",status);
            }
            String response = httpRequestUtils.postData(getCredentials(), getUserProfileUpdateUrl() , "application/json", "application/json",jsonFromObject.toString());
            Log.i(TAG,response);
            return ((ApiResponse) GsonUtils.getObjectFromJson(response,ApiResponse.class));
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public ApiResponse getUserReadServerCall() {
        String response = null;
        ApiResponse apiResponse = null;
        try {
            response = httpRequestUtils.getResponse(getCredentials(), getUserReadUrl() , null, null);
            if(response != null){
                apiResponse = (ApiResponse)GsonUtils.getObjectFromJson(response,ApiResponse.class);
            }
            Log.i(TAG,"User read response: "+response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

}
