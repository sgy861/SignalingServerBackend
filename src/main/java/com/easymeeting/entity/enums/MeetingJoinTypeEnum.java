package com.easymeeting.entity.enums;

public enum MeetingJoinTypeEnum {

    NO_PASSWORD(0 , "不需要密码") ,
    PASSWORD(1 , "需要密码");

    private Integer type;
    private String desc;

    private MeetingJoinTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public MeetingJoinTypeEnum getByType(Integer code) {
        for (MeetingJoinTypeEnum item : MeetingJoinTypeEnum.values()) {
            if (item.type.equals(code)) {
                return item;
            }
        }
        return null;
    }
    public Integer getType() {
        return type;
    }
    public String getDesc() {
        return desc;
    }
}
