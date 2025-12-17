package com.easymeeting.entity.enums;

public enum MeetingMemberStatusEnum {
    DEL_MEETING(0 , "删除会议") ,
    NORMAL(1 , "正常") ,
    EXIT_MEETING(2 , "退出会议") ,
    KICK_OUT(3 , "被提出会议") ,
    BLACKLIST(4 , "被拉黑");

    Integer status ;
    String desc;

    MeetingMemberStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static MeetingMemberStatusEnum getByStatus(Integer status) {
        for(MeetingMemberStatusEnum item : MeetingMemberStatusEnum.values()) {
            if(item.status.equals(status)) {
                return item;
            }
        }
        return null;
    }

    public String getDesc() {
        return desc;
    }

    public Integer getStatus() {
        return status;
    }
}
