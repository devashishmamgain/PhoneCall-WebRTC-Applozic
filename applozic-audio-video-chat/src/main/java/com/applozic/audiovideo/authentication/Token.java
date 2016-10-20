package com.applozic.audiovideo.authentication;

/**
 * Created by anuranjit on 16/7/16.
 */
public class Token {

    public String identity;
    public String token;

    public Token(String identity, String token) {
        this.identity = identity;
        this.token = token;
    }

    @Override
    public String toString() {
        return "Token{" +
                "identity='" + identity + '\'' +
                ", token='" + token + '\'' +
                '}';
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }


}


