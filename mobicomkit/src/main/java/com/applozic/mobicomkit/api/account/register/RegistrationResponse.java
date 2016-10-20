package com.applozic.mobicomkit.api.account.register;

import android.text.TextUtils;

import com.applozic.mobicommons.json.JsonMarker;

/**
 * @author devashish
 */
public class RegistrationResponse extends JsonMarker {

    private String message;
    private String deviceKey;
    private String userKey;
    private Long lastSyncTime;
    private Long currentTimeStamp;
    private String notificationResponse;
    private String brokerUrl;
    private String displayName;
    private String imageLink;
    private String contactNumber;

    private Short pricingPackage = PricingType.STARTER.getValue();

    public static enum PricingType {

        CLOSED(Short.valueOf("-1")), BETA(Short.valueOf("0")), STARTER(Short.valueOf("1")), LAUNCH(Short.valueOf("2")), GROWTH(Short.valueOf("3")), ENTERPRISE(
                Short.valueOf("4"));
        private final Short value;

        private PricingType(Short c) {
            value = c;
        }

        public Short getValue() {
            return value;
        }
    };

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(String deviceKeyString) {
        this.deviceKey = deviceKeyString;
    }

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String suUserKeyString) {
        this.userKey = suUserKeyString;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Long getLastSyncTime() {
        return lastSyncTime == null ? 0L : lastSyncTime;
    }

    public void setLastSyncTime(Long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public Long getCurrentTimeStamp() {
        return currentTimeStamp == null ? 0L : currentTimeStamp;
    }

    public void setCurrentTimeStamp(Long currentTimeStamp) {
        this.currentTimeStamp = currentTimeStamp;
    }

    public String getNotificationResponse() {
        return notificationResponse;
    }

    public void setNotificationResponse(String notificationResponse) {
        this.notificationResponse = notificationResponse;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public boolean isPasswordInvalid(){
        return (! TextUtils.isEmpty(message) && ( "PASSWORD_INVALID".equals(message) || "PASSWORD_REQUIRED".equals(message) ));
    }

    public Short getPricingPackage() {
        return pricingPackage;
    }

    public void setPricingPackage(Short pricingPackage) {
        this.pricingPackage = pricingPackage;
    }


    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }


    @Override
    public String toString() {
        return "RegistrationResponse{" +
                "message='" + message + '\'' +
                ", deviceKey='" + deviceKey + '\'' +
                ", userKey='" + userKey + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", lastSyncTime='" + lastSyncTime + '\'' +
                ", currentTimeStamp='" + currentTimeStamp + '\'' +
                '}';
    }
}
