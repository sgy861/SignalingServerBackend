package com.easymeeting.entity.dto;

public class MeetingMemberDto {
    private String userId;
    private String nickName;
    private String avatar;
    private Long joinTime;
    private Integer memberType;
    private Integer status;
    private Boolean videoOpen;
    private Integer sex;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(Long joinTime) {
        this.joinTime = joinTime;
    }

    public Integer getMemberType() {
        return memberType;
    }

    public void setMemberType(Integer memberType) {
        this.memberType = memberType;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getVideoOpen() {
        return videoOpen;
    }

    public void setVideoOpen(Boolean videoOpen) {
        this.videoOpen = videoOpen;
    }
}
