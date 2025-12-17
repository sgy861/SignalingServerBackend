package com.easymeeting.service.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;


import com.easymeeting.entity.dto.*;
import com.easymeeting.entity.enums.*;
import com.easymeeting.entity.po.MeetingMember;
import com.easymeeting.entity.query.MeetingMemberQuery;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.mappers.MeetingMemberMapper;
import com.easymeeting.redis.RedisUtils;
import com.easymeeting.utils.JsonUtils;
import com.easymeeting.websocket.ChannelContextUtils;
import com.easymeeting.websocket.message.MessageHandler;
import jdk.nashorn.internal.parser.Token;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.query.MeetingInfoQuery;
import com.easymeeting.entity.po.MeetingInfo;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.mappers.MeetingInfoMapper;
import com.easymeeting.service.MeetingInfoService;
import com.easymeeting.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 *  业务接口实现
 */
@Service("meetingInfoService")
@Slf4j
public class MeetingInfoServiceImpl implements MeetingInfoService {

	@Resource
	private ChannelContextUtils channelContextUtils;

	@Resource
	private MeetingInfoMapper<MeetingInfo, MeetingInfoQuery> meetingInfoMapper;

	@Resource
	private MeetingMemberMapper meetingMemberMapper;

    @Resource
    private RedisUtils redisUtils;


	@Resource
	private MessageHandler messageHandler;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MeetingInfo> findListByParam(MeetingInfoQuery param) {
		return this.meetingInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(MeetingInfoQuery param) {
		return this.meetingInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MeetingInfo> findListByPage(MeetingInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MeetingInfo> list = this.findListByParam(param);
		PaginationResultVO<MeetingInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(MeetingInfo bean) {
		return this.meetingInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<MeetingInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<MeetingInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(MeetingInfo bean, MeetingInfoQuery param) {
		StringTools.checkParam(param);
		return this.meetingInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(MeetingInfoQuery param) {
		StringTools.checkParam(param);
		return this.meetingInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据MeetingId获取对象
	 */
	@Override
	public MeetingInfo getMeetingInfoByMeetingId(String meetingId) {
		return this.meetingInfoMapper.selectByMeetingId(meetingId);
	}

	/**
	 * 根据MeetingId修改
	 */
	@Override
	public Integer updateMeetingInfoByMeetingId(MeetingInfo bean, String meetingId) {
		return this.meetingInfoMapper.updateByMeetingId(bean, meetingId);
	}

	/**
	 * 根据MeetingId删除
	 */
	@Override
	public Integer deleteMeetingInfoByMeetingId(String meetingId) {
		return this.meetingInfoMapper.deleteByMeetingId(meetingId);
	}

	@Override
	public void quickMeeting(MeetingInfo meetingInfo, String nickName) {
		Date currentDate = new Date();
		meetingInfo.setCreateTime(currentDate);
		meetingInfo.setMeetingId(StringTools.getMeetingNumber());
		meetingInfo.setStartTime(currentDate);
		meetingInfo.setStatus(MeetingStatusEnum.RUNNING.getStatus());
		meetingInfoMapper.insert(meetingInfo);
	}

	/**
	 * 查看用户是否有权限进房间
	 * @param meetingId
	 * @param userId
	 */
	private void checkMeetingJoin(String meetingId , String userId) {
		MeetingMemberDto meetingMemberDto = redisUtils.getMeetingMemberDto(meetingId, userId);
		if(meetingMemberDto != null && MeetingMemberStatusEnum.BLACKLIST.getStatus().equals(meetingMemberDto.getStatus())) {
			throw new BusinessException("你已经被拉黑，无法加入会议");
		}
	}
	private void addMeetingMember(String meetingId , String userId , String nickName , Integer memberType){
		MeetingMember meetingMember = new MeetingMember();
		meetingMember.setMeetingId(meetingId);
		meetingMember.setUserId(userId);
		meetingMember.setNickName(nickName);
		meetingMember.setLastJoinTime(new Date());
		meetingMember.setMeetingStatus(MeetingMemberStatusEnum.NORMAL.getStatus());
		meetingMember.setMemberType(memberType);
		meetingMember.setMeetingStatus(MeetingStatusEnum.RUNNING.getStatus());
		this.meetingMemberMapper.insertOrUpdate(meetingMember);
	}

	private void add2Meeting(String meetingId , String userId , String nickName , Integer sex , Integer memberType ,
							 Boolean videoOpen){
		MeetingMemberDto meetingMemberDto = new MeetingMemberDto();
		meetingMemberDto.setUserId(userId);
		meetingMemberDto.setNickName(nickName);
		meetingMemberDto.setJoinTime(System.currentTimeMillis());
		meetingMemberDto.setMemberType(memberType);
		meetingMemberDto.setStatus(MeetingMemberStatusEnum.NORMAL.getStatus());
		meetingMemberDto.setSex(sex);
		meetingMemberDto.setVideoOpen(videoOpen);
		redisUtils.add2Meeting(meetingId , meetingMemberDto);
	}
	@Override
    public void joinMeeting(String meetingId, String userId, String nickName, Integer sex, Boolean videoOpen) {
		if(StringTools.isEmpty(meetingId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		MeetingInfo meetingInfo = this.meetingInfoMapper.selectByMeetingId(meetingId);
		if(meetingInfo == null || MeetingStatusEnum.FINISHED.equals(meetingInfo.getStatus())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//校验：是否被拉黑
		checkMeetingJoin(meetingId , userId);
		//加入成员
		MemberTypeEnum memberTypeEnum = meetingInfo.getCreateUserId().equals(userId) ? MemberTypeEnum.COMPERE :
				MemberTypeEnum.NORMAL;
		this.addMeetingMember(meetingId , userId , nickName , memberTypeEnum.getType());//数据库中加入成员
		//加入会议
		this.add2Meeting(meetingId , userId , nickName , sex , memberTypeEnum.getType() , videoOpen);//redis中加入成员
		//加入ws房间
		this.channelContextUtils.addMeetingRoom(meetingId, userId);

		//发送ws消息
		MeetingJoinDto meetingJoinDto = new MeetingJoinDto();
		meetingJoinDto.setNewMember(redisUtils.getMeetingMemberDto(meetingId, userId));
		meetingJoinDto.setMeetingMemberDtoList(redisUtils.getMeetingMembers(meetingId));

		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageType(MessageTypeEnum.ADD_MEETING_ROOM.getType());
		messageSendDto.setMeetingId(meetingId);
		messageSendDto.setMessageContent(meetingJoinDto);
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
		log.info("开始群发信息:{}" , messageSendDto);
		messageHandler.sendMessage(messageSendDto);
	}

	@Override
	public String preJoinMeeting(String meetingNo, TokenUserInfoDto tokenUserInfoDto, String password) {
		String userId = tokenUserInfoDto.getUserId();
		MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
		meetingInfoQuery.setMeetingNo(meetingNo);
		meetingInfoQuery.setStatus(MeetingStatusEnum.RUNNING.getStatus());
		meetingInfoQuery.setOrderBy("create_time desc");
		List<MeetingInfo> meetingInfoList = meetingInfoMapper.selectList(meetingInfoQuery);
		if(meetingInfoList.isEmpty()){
			throw new BusinessException("会议不存在");
		}
		MeetingInfo meetingInfo = meetingInfoList.get(0);
		if(!MeetingStatusEnum.RUNNING.getStatus().equals(meetingInfo.getStatus())) {
			throw new BusinessException("会议已经结束");
		}
		if(!StringTools.isEmpty(tokenUserInfoDto.getCurrentMeetingId()) &&
				!meetingInfo.getMeetingId().equals(tokenUserInfoDto.getCurrentMeetingId())) {
			throw new BusinessException("你有未结束的会议,无法加入其他");
		}

		checkMeetingJoin(meetingInfo.getMeetingId() , userId);

		if(MeetingJoinTypeEnum.PASSWORD.getType().equals(meetingInfo.getJoinType()) &&
		!meetingInfo.getJoinPassword().equals(password)) {
			throw new BusinessException("入会密码不正确");
		}

		tokenUserInfoDto.setCurrentMeetingId(meetingInfo.getMeetingId());
		redisUtils.saveTokenUserInfoDto(tokenUserInfoDto);
		return meetingInfo.getMeetingId();
	}

	@Override
	public void exitMeetingRoom(TokenUserInfoDto tokenUserInfoDto, MeetingMemberStatusEnum memberStatusEnum) {
		String meetingId = tokenUserInfoDto.getCurrentMeetingId();
		if(StringTools.isEmpty(meetingId)) {
			return ;
		}
		String userId = tokenUserInfoDto.getUserId();
		Boolean isExit = redisUtils.exitMeeting(meetingId , userId , memberStatusEnum);
		if(!isExit){
			tokenUserInfoDto.setCurrentMeetingId(null);
			redisUtils.saveTokenUserInfoDto(tokenUserInfoDto);
			return ;
		}

		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageType(MessageTypeEnum.EXIT_MEETING_ROOM.getType());
		tokenUserInfoDto.setCurrentMeetingId(null);
		redisUtils.saveTokenUserInfoDto(tokenUserInfoDto);

		//群发信息告诉大家我退出了
		List<MeetingMemberDto> meetingMemberDtoList = redisUtils.getMeetingMembers(meetingId);
		MeetingExitDto meetingExitDto = new MeetingExitDto();
		meetingExitDto.setMeetingMemberDtoList(meetingMemberDtoList);
		meetingExitDto.setExitUserId(userId);
		meetingExitDto.setExitStatus(memberStatusEnum.getStatus());
		messageSendDto.setMessageContent(JsonUtils.convertObjectToJson(meetingExitDto));
		messageSendDto.setMeetingId(meetingId);
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
		messageHandler.sendMessage(messageSendDto);

		//获取还在会议中的人，如果没人了就关闭会议
		List<MeetingMemberDto> onLineMemberList = meetingMemberDtoList.stream().filter(item ->
				MeetingMemberStatusEnum.NORMAL.getStatus().equals(item.getStatus())).collect(Collectors.toList());
		if(onLineMemberList.isEmpty()){
			finishMeeting(meetingId , userId);
			return ;
		}

		//如果该用户有被拉黑，更新他的信息
		if(ArrayUtils.contains(new Integer[]{MeetingMemberStatusEnum.BLACKLIST.getStatus() ,
				MeetingMemberStatusEnum.KICK_OUT.getStatus()} , memberStatusEnum.getStatus())) {
			MeetingMember meetingMember = new MeetingMember();
			meetingMember.setStatus(memberStatusEnum.getStatus());
			meetingMemberMapper.updateByMeetingIdAndUserId(meetingMember , meetingId , userId);

		}
	}

	@Override
	public void forceExitRoom(TokenUserInfoDto tokenUserInfoDto, String userId ,
							  MeetingMemberStatusEnum memberStatusEnum) {
		MeetingInfo meetingInfo = meetingInfoMapper.selectByMeetingId(tokenUserInfoDto.getCurrentMeetingId());
		if(!meetingInfo.getCreateUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		TokenUserInfoDto userInfoDto = this.redisUtils.getTokenUserInfoDto(userId);
		exitMeetingRoom(userInfoDto , memberStatusEnum);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void finishMeeting(String currentMeetingId, String userId) {
		MeetingInfo meetingInfo = meetingInfoMapper.selectByMeetingId(currentMeetingId);
		if(userId != null && !meetingInfo.getCreateUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		MeetingInfo updateInfo = new MeetingInfo();
		updateInfo.setStatus(MeetingStatusEnum.FINISHED.getStatus());
		updateInfo.setEndTime(new Date());
		meetingInfoMapper.updateByMeetingId(updateInfo , currentMeetingId);

		//群发送一个结束会议消息
		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setMessageType(MessageTypeEnum.FINISH_MEETING.getType());
		messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.GROUP.getType());
		messageSendDto.setMeetingId(currentMeetingId);
		messageHandler.sendMessage(messageSendDto);

		//批量将群成员的状态设置为结束会议
		MeetingMember meetingMember = new MeetingMember();
		meetingMember.setStatus(MeetingStatusEnum.FINISHED.getStatus());
		MeetingMemberQuery meetingMemberQuery = new MeetingMemberQuery();
		meetingMemberQuery.setMeetingId(currentMeetingId);
		meetingMemberMapper.updateByParam(meetingMember , meetingMemberQuery);

		//TODO 更新预约会议列表
		//更新成员的会议信息
		List<MeetingMemberDto> meetingMemberDtoList = redisUtils.getMeetingMembers(currentMeetingId);
		for(MeetingMemberDto meetingMemberDto : meetingMemberDtoList){
			TokenUserInfoDto tokenUserInfoDto = this.redisUtils.getTokenUserInfoDto(meetingMemberDto.getUserId());
			tokenUserInfoDto.setCurrentMeetingId(null);
			redisUtils.saveTokenUserInfoDto(tokenUserInfoDto);
		}
		//移除所有的人
		redisUtils.removeAllMeetingMembers(currentMeetingId);
	}
}