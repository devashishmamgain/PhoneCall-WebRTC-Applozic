package com.applozic.mobicommons.people.channel;

import com.applozic.mobicommons.people.contact.Contact;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by devashish on 5/9/14.
 */
public class Channel implements Serializable {

    private Integer key;
    private String clientGroupId;
    private String name;
    private String adminKey;
    private Short type;
    private int unreadCount;
    private int userCount;
    private String imageUrl;
    @Expose
    private String localImageUri;
    private Conversation conversationPxy;
    private List<Contact> contacts = new ArrayList<Contact>();

    public Channel() {

    }

    public Channel(Integer key, String name, String adminKey, Short type,int unreadCount,String imageUrl) {
        this.key = key;
        this.name = name;
        this.adminKey = adminKey;
        this.type = type;
        this.imageUrl = imageUrl;
        this.unreadCount = unreadCount;
    }

    public Channel(Integer key) {
        this.key = key;
    }

    public Channel(Integer key, String name) {
        this.key = key;
        this.name = name;

    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdminKey() {
        return adminKey;
    }

    public void setAdminKey(String adminKey) {
        this.adminKey = adminKey;
    }

    public Short getType() {
        return type;
    }

    public void setType(Short type) {
        this.type = type;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public Conversation getConversationPxy() {
        return conversationPxy;
    }

    public void setConversationPxy(Conversation conversationPxy) {
        this.conversationPxy = conversationPxy;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLocalImageUri() {
        return localImageUri;
    }

    public void setLocalImageUri(String localImageUri) {
        this.localImageUri = localImageUri;
    }

    public String getClientGroupId() {
        return clientGroupId;
    }

    public void setClientGroupId(String clientGroupId) {
        this.clientGroupId = clientGroupId;
    }

    public boolean isBroadcastMessage(){
       return type.equals(GroupType.BROADCAST.getValue()) || type.equals(GroupType.BROADCAST_ONE_BY_ONE.getValue());
    }

    public enum GroupType {

        VIRTUAL(0),
        PRIVATE(1),
        PUBLIC(2),
        SELLER(3),
        SELF(4),
        BROADCAST(5),
        BROADCAST_ONE_BY_ONE(106);

        private Integer value;

        GroupType(Integer value) {
            this.value = value;
        }

        public Short getValue() {
            return value.shortValue();
        }
    }

    @Override
    public String toString() {
        return "Channel{" +
                "key=" + key +
                ", name='" + name + '\'' +
                ", adminKey='" + adminKey + '\'' +
                ", type=" + type +
                ", unreadCount=" + unreadCount +
                ", userCount=" + userCount +
                ", imageUrl='" + imageUrl + '\'' +
                ", localImageUri='" + localImageUri + '\'' +
                ", conversationPxy=" + conversationPxy +
                ", contacts=" + contacts +
                '}';
    }
}
