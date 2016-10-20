package com.applozic.mobicomkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import com.applozic.mobicomkit.api.MobiComKitClientService;
import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;

/**
 * Created by devashish on 8/21/2015.
 */
public class ApplozicClient {

    private static final String HANDLE_DISPLAY_NAME = "CLIENT_HANDLE_DISPLAY_NAME";
    private static final String HANDLE_DIAL = "CLIENT_HANDLE_DIAL";
    private static final String CHAT_LIST_HIDE_ON_NOTIFICATION = "CHAT_LIST_HIDE_ON_NOTIFICATION";
    private static final String CONTEXT_BASED_CHAT = "CONTEXT_BASED_CHAT";
    private static final String NOTIFICATION_SMALL_ICON= "NOTIFICATION_SMALL_ICON";
    private static final String APP_NAME = "APP_NAME";
    private static final String ENABLE_IP_CALL = "ENABLE_IP_CALL";


    public static ApplozicClient applozicClient;
    public SharedPreferences sharedPreferences;
    private Context context;

    private ApplozicClient(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(MobiComKitClientService.getApplicationKey(context), context.MODE_PRIVATE);
    }

    public static ApplozicClient getInstance(Context context) {
        if (applozicClient == null) {
            applozicClient = new ApplozicClient(context);
        }

        return applozicClient;
    }

    public boolean isHandleDisplayName() {
        return sharedPreferences.getBoolean(HANDLE_DISPLAY_NAME, true);
    }

    public ApplozicClient setHandleDisplayName(boolean enable) {
        sharedPreferences.edit().putBoolean(HANDLE_DISPLAY_NAME, enable).commit();
        return this;
    }

    public boolean isHandleDial() {
        return sharedPreferences.getBoolean(HANDLE_DIAL, false);
    }

    public ApplozicClient setHandleDial(boolean enable) {
        sharedPreferences.edit().putBoolean(HANDLE_DIAL, enable).commit();
        return this;
    }

    public ApplozicClient hideChatListOnNotification() {
        sharedPreferences.edit().putBoolean(CHAT_LIST_HIDE_ON_NOTIFICATION, true).apply();
        return this;
    }

    public boolean isChatListOnNotificationIsHidden() {
        return sharedPreferences.getBoolean(CHAT_LIST_HIDE_ON_NOTIFICATION, false);
    }

    public ApplozicClient setContextBasedChat(boolean enable){
        sharedPreferences.edit().putBoolean(CONTEXT_BASED_CHAT,enable).apply();
        return this;
    }

    public boolean isContextBasedChat() {
        return sharedPreferences.getBoolean(CONTEXT_BASED_CHAT, false);
    }

    public ApplozicClient hideNotificationSmallIcon() {
        sharedPreferences.edit().putBoolean(NOTIFICATION_SMALL_ICON, true).apply();
        return this;
    }

    public boolean isNotificationSmallIconHidden() {
      return sharedPreferences.getBoolean(NOTIFICATION_SMALL_ICON, false);
    }

    public boolean isNotAllowed() {
        MobiComUserPreference pref = MobiComUserPreference.getInstance(context);
        boolean isDebuggable =  ( 0 != ( context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );
        return !isDebuggable && (pref.getPricingPackage() == RegistrationResponse.PricingType.CLOSED.getValue()
                ||  pref.getPricingPackage() == RegistrationResponse.PricingType.BETA.getValue());
    }

    public boolean isAccountClosed() {
        return MobiComUserPreference.getInstance(context).getPricingPackage() == RegistrationResponse.PricingType.CLOSED.getValue();
    }

    public String getAppName() {
        return sharedPreferences.getString(APP_NAME, "Applozic");
    }

    public ApplozicClient setAppName(String notficationAppName) {
        sharedPreferences.edit().putString(APP_NAME, notficationAppName).commit();
        return this;
    }
    public void setIPCallEnabled(boolean iPCallEnabled) {
        sharedPreferences.edit().putBoolean(ENABLE_IP_CALL, iPCallEnabled).commit();
    }

    public boolean isIPCallEnabled() {
        return sharedPreferences.getBoolean(ENABLE_IP_CALL, false);
    }

}
