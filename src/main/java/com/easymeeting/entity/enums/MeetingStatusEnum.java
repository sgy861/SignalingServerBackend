package com.easymeeting.entity.enums;

public enum MeetingStatusEnum {
    RUNNING(0 , "会议进行中"),
    FINISHED(1 , "会议结束");

    private Integer status;
    private String description;

    MeetingStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    public MeetingStatusEnum getByStatus(Integer status) {
        for (MeetingStatusEnum meetingStatusEnum : MeetingStatusEnum.values()) {
            if (meetingStatusEnum.getStatus().equals(status)) {
                return meetingStatusEnum;
            }
        }
        return null;
    }
    public String getDescription() {
        return description;
    }

    public Integer getStatus() {
        return status;
    }
}
