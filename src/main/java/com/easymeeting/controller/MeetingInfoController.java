package com.easymeeting.controller;

import com.easymeeting.annotation.GlobalInterceptor;
import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.MeetingMemberStatusEnum;
import com.easymeeting.entity.enums.MeetingStatusEnum;
import com.easymeeting.entity.enums.MessageSend2TypeEnum;
import com.easymeeting.entity.po.MeetingInfo;
import com.easymeeting.entity.query.MeetingInfoQuery;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.service.MeetingInfoService;
import com.easymeeting.utils.StringTools;
import com.easymeeting.websocket.message.MessageHandler;
import jdk.nashorn.internal.parser.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@RestController
@Validated
@Slf4j
@RequestMapping("/meeting")
public class MeetingInfoController extends ABaseController{

    @Resource
    private MeetingInfoService meetingInfoService;

    @Resource
    private MessageHandler messageHandler;


    @RequestMapping("/loadingMeeting")
    public ResponseVO loadingMeeting(Integer pageNo) {

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
        meetingInfoQuery.setPageNo(pageNo);
//        System.out.println(tokenUserInfoDto.getUserId());
        meetingInfoQuery.setMeetingId(tokenUserInfoDto.getUserId());
        meetingInfoQuery.setOrderBy("m.create_time desc");
        meetingInfoQuery.setQueryMemberCount(true);
        PaginationResultVO resultVO = this.meetingInfoService.findListByPage(meetingInfoQuery);

        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/quickMeeting")
    @GlobalInterceptor()
    public ResponseVO quickMeeting(@NotNull Integer meetingNoType ,
                                   @NotEmpty @Size(max = 100) String meetingName ,
                                   @NotNull Integer joinType , @Max(5) String joinPassword) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        if(tokenUserInfoDto.getCurrentMeetingId() != null) {
            throw new BusinessException("你有未结束的会议，无法创建新的会议");
        }

        MeetingInfo meetingInfo = new MeetingInfo();
        meetingInfo.setMeetingName(meetingName);
        meetingInfo.setMeetingNo(meetingNoType == 0 ? tokenUserInfoDto.getMyMeetingNo() : StringTools.getMeetingNumber());
        meetingInfo.setJoinType(joinType);
        meetingInfo.setJoinPassword(joinPassword);
        meetingInfo.setCreateUserId(tokenUserInfoDto.getUserId());
        meetingInfoService.quickMeeting(meetingInfo , tokenUserInfoDto.getNickName());
        tokenUserInfoDto.setCurrentMeetingId(meetingInfo.getMeetingId());
        tokenUserInfoDto.setCurrentNickName(tokenUserInfoDto.getNickName());
        resetTokenUserInfo(tokenUserInfoDto);
        return getSuccessResponseVO(meetingInfo.getMeetingId());
    }

    @RequestMapping("/preJoinMeeting")
    public ResponseVO preJoinMeeting(@NotEmpty String meetingNo , @NotEmpty String nickName , String password) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        meetingNo = meetingNo.replace(" " , "");
        tokenUserInfoDto.setNickName(nickName);
        String meetingId = meetingInfoService.preJoinMeeting(meetingNo , tokenUserInfoDto , password);
        return getSuccessResponseVO(meetingId); // 将meetingId存到redis中去,并且返回前端meetingId
    }

    @RequestMapping("/joinMeeting")
    public ResponseVO joinMeeting(@NotNull Boolean videoOpen) {
        log.info("获取用户信息中");
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        log.info("获取用户信息完成，加入会议中");
        meetingInfoService.joinMeeting(tokenUserInfoDto.getCurrentMeetingId() , tokenUserInfoDto.getUserId() ,
                tokenUserInfoDto.getNickName() , tokenUserInfoDto.getGender() , videoOpen);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/testSendMessage")
    public ResponseVO testSendMessage() {
        MessageSendDto messageSendDto = new MessageSendDto();
        messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
        messageSendDto.setReceiveUserId("280478398138");
        messageSendDto.setMessageContent("现在的时间是" + System.currentTimeMillis());
        messageHandler.sendMessage(messageSendDto);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/exitMeeting")
    public ResponseVO exitMeeting() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        meetingInfoService.exitMeetingRoom(tokenUserInfoDto , MeetingMemberStatusEnum.EXIT_MEETING);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/kickOutMeeting")
    public ResponseVO kickOutMeeting(@NotEmpty String userId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        meetingInfoService.forceExitRoom(tokenUserInfoDto , userId , MeetingMemberStatusEnum.KICK_OUT);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/blackMeeting")
    public ResponseVO blackMeeting(@NotEmpty String userId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        meetingInfoService.forceExitRoom(tokenUserInfoDto , userId ,MeetingMemberStatusEnum.BLACKLIST);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/getCurrentMeeting")
    public ResponseVO getCurrentMeeting() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        if(StringTools.isEmpty(tokenUserInfoDto.getCurrentMeetingId())) {
            return getSuccessResponseVO(null);
        }

        MeetingInfo meetingInfo = meetingInfoService.getMeetingInfoByMeetingId(tokenUserInfoDto.getCurrentMeetingId());
        if(MeetingStatusEnum.FINISHED.getStatus().equals(meetingInfo.getStatus())) {
            return getSuccessResponseVO(null);
        }
        return getSuccessResponseVO(meetingInfo);
    }

    @RequestMapping("/finishMeeting")
    public ResponseVO finishMeeting() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
        meetingInfoService.finishMeeting(tokenUserInfoDto.getCurrentMeetingId() , tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
