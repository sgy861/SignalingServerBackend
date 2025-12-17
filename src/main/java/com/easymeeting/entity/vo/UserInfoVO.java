package com.easymeeting.entity.vo;

import java.io.Serializable;

public class UserInfoVO implements Serializable {
    private String userId;
    private String nickName;
    private String sex;
    private String token;
    private String messageNo;
    private Boolean admin;
//    private Boolean success;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessageNo() {
        return messageNo;
    }

    public void setMessageNo(String messageNo) {
        this.messageNo = messageNo;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }


}
