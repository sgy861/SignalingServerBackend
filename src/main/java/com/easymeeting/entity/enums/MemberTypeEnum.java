package com.easymeeting.entity.enums;

public enum MemberTypeEnum {
    NORMAL(0 , "普通成员") ,
    COMPERE(1 , "主理人")
    ;

    private Integer type;
    private String desc;

    MemberTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
    public static MemberTypeEnum getByType(Integer type) {
        for (MemberTypeEnum memberTypeEnum : MemberTypeEnum.values()) {
            if (memberTypeEnum.getType().equals(type)) {
                return memberTypeEnum;
            }
        }
        return null;
    }

    public String getDesc() {
        return desc;
    }

    public Integer getType() {
        return type;
    }
}
