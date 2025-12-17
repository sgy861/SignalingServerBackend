package com.easymeeting.entity.enums;

import com.easymeeting.entity.dto.MessageSendDto;

public enum MessageTypeEnum {
    INIT(0 , "连接ws获取信息") ,
    ADD_MEETING_ROOM(1 , "加入房间") ,
    PEER(2 , "发送peer"),
    EXIT_MEETING_ROOM(3 , "退出房间") ,
    FINISH_MEETING(4 , "结束会议") ,
    CHAT_TEXT_MESSAGE(5 , "文本消息") ,
    CHAT_MEDIA_MESSAGE(6 , "媒体消息") ,
    CHAT_MEDIA_MESSAGE_UPDATE(7 , "媒体消息更新") ,
    USER_CONTACT_APPLY(8 , "好友申请消息") ,
    INVITE_MEMBER_MEETING(9 , "邀请入会") ,
    FORCE_OFF_LINE(10 , "强制下线") ,
    MEETING_USER_VIDEO_CHANGE(11 , "用户视频改变")
            ;

    private Integer type;
    private String desc;

    private MessageTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static MessageTypeEnum getByType(Integer type) {
        for(MessageTypeEnum e : MessageTypeEnum.values()) {
            if(e.type == type) {
                return e;
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
