package com.easymeeting.entity.dto;

import java.io.Serializable;

public class TokenUserInfoDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private String token;
    private String userId;
    private String nickName;
    private Integer gender;
    private String currentMeetingId;
    private String currentNickName;
    private String myMeetingNo;
    private Boolean admin;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

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

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getCurrentMeetingId() {
        return currentMeetingId;
    }

    public void setCurrentMeetingId(String currentMeetingId) {
        this.currentMeetingId = currentMeetingId;
    }

    public String getCurrentNickName() {
        return currentNickName;
    }

    public void setCurrentNickName(String currentNickName) {
        this.currentNickName = currentNickName;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public String getMyMeetingNo() {
        return myMeetingNo;
    }

    public void setMyMeetingNo(String myMeetingNo) {
        this.myMeetingNo = myMeetingNo;
    }
}
