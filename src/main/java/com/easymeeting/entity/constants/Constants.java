package com.easymeeting.entity.constants;

public class Constants {
    public static final Integer LENGTH_11 = 11;
    public static final Integer LENGTH_12 = 12;
    public static final Integer LENGTH_20 = 20;
    public static final Integer LENGTH_30 = 30;
    public static final String PING = "ping";
    public static final String DEFAULT_AVATAR = "avatar/";
    public static final String VIDEO_SUFFIX = ".mp4";
    public static final String IMAGE_SUFFIX = ".jpg";
    public static final Long REDIS_KEY_CHECK_CODE_TIMEOUT_MIN = 60L;
    public static final Long REDIS_KEY_EXPIRE_DAY = 24 * 60 * 60 * 1000L;
    public static final String REDIS_KEY_PREFIX = "easymeeting:";
    public static final String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX + "checkCode:";
    public static final String REDIS_KEY_WS_TOKEN = REDIS_KEY_PREFIX + "ws:token:";
    public static final String REDIS_KEY_WS_TOKEN_USERID = REDIS_KEY_PREFIX + "ws:token:userid:";
    public static final String REDIS_KEY_WS_USER_HEART_BEAT = REDIS_KEY_PREFIX + "ws:user:heartbeat:";
    public static final String REDIS_KEY_MEETING_ROOM = REDIS_KEY_PREFIX + "meeting:room:";
    public static final String REDIS_KEY_INVITE_MEMBER = REDIS_KEY_PREFIX + "meeting:invite:member:";
    public static final String REDIS_KEY_SYS_SETTING = REDIS_KEY_PREFIX + "sysSetting:";
    public static final String MEETING_NO_PREFIX = "M";
    public static final String IMAGE_THUMB_NAIl_SUFFIX = "_thumbnail";
    public static final String VIDEO_CODE_HEVC = "hevc";
    public static final String APP_UPDATE_FOLDER = "/app/";
    public static final String APP_NAME = "EasyMeetingSetUp.";
    public static final String APP_EXE_SUFFIX = ".exe";
    public static final String MESSAGE_HANDLE_CHANNEL_KEY = "message.handle.channel";
    public static final String MESSAGE_HANDLE_CHANNEL_REDIS = "redis";
    public static final String MESSAGE_HANDLE_CHANNEL_RABBITMQ = "rabbitmq";


}
