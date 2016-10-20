package com.applozic.audiovideo.authentication;

import android.content.Context;
import android.util.Log;

import com.applozic.mobicomkit.api.HttpRequestUtils;
import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;

/**
 * Created by devashish on 30/07/16.
 */
public class TokenGenClientService extends MobiComKitClientService {

    private static final String TAG = "TokenGenClientService";
    //Todo: Take url based on env.
    public final String TOKEN_URL = "https://staging.applozic.com/twilio/token";

    public String getUrl() {
        return TOKEN_URL;
    }

    public TokenGenClientService(Context context) {
        super.context = context;
    }

    public String getGeneratedToken() {

        try {

            HttpRequestUtils httpRequestUtils = new HttpRequestUtils(context);
            MobiComUserPreference pref = MobiComUserPreference.getInstance(context);
            String identity = pref.getUserId();
            String device = pref.getDeviceKeyString();
            String data = "identity=" + identity + "&device=" + device;
            String response = httpRequestUtils.postData(getCredentials(), getUrl(), "application/x-www-form-urlencoded", null, data);
            Log.i(TAG, response);
            return response;

        } catch (Exception e) {
            Log.i(TAG, " Exception ::", e);
            return null;
        }
    }

}
