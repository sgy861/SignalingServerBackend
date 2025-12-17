package com.easymeeting.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easymeeting.config.AppConfig;
import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.UserStatus;
import com.easymeeting.entity.vo.UserInfoVO;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.redis.RedisUtils;
import com.easymeeting.utils.CopyTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easymeeting.entity.enums.PageSize;
import com.easymeeting.entity.query.UserInfoQuery;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.query.SimplePage;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.service.UserInfoService;
import com.easymeeting.utils.StringTools;


/**
 *  业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	@Resource
	private AppConfig appConfig;
    @Autowired
    private RedisUtils redisUtils;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByEmail(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		StringTools.checkParam(bean);
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	@Override
	public void deleteUserInfoByEmail(String email) {
		this.userInfoMapper.deleteByEmail(email);
	}

	@Override
	public void register(String email, String nickName, String password) {
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if (userInfo != null) {
			throw new BusinessException("邮箱账号已存在");
		}

		Date now = new Date();

		userInfo = new UserInfo();
		String userId = StringTools.getRandomNumber(Constants.LENGTH_12);
		userInfo.setUserId(userId);
		userInfo.setEmail(email);
		userInfo.setNickName(nickName);
		userInfo.setPassword(StringTools.encodeMD5(password));
		userInfo.setCreateTime(now);
		userInfo.setLastOffTime(now.getTime());
		userInfo.setMeetingNo(StringTools.getMeetingNumber());
		userInfo.setStatus(UserStatus.ENABLE.getStatus());
		this.userInfoMapper.insert(userInfo);
	}

	@Override
	public UserInfoVO login(String email, String password) {
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if (userInfo == null || !userInfo.getPassword().equals(StringTools.encodeMD5(password)) ) {
			throw new BusinessException("账号或密码不正确");
		}
		if(!UserStatus.ENABLE.getStatus().equals(userInfo.getStatus())){
			throw new BusinessException("账户被禁用");
		}

		if(userInfo.getLastLoginTime() != null && userInfo.getLastOffTime() <= userInfo.getLastLoginTime()) {
			throw new BusinessException("此账号已在别处登录，请退出后再登录");
		}
		TokenUserInfoDto tokenInfoDto = CopyTools.copy(userInfo , TokenUserInfoDto.class);

		String token = StringTools.encodeMD5(userInfo.getUserId() + StringTools.getRandomString(Constants.LENGTH_20));
		tokenInfoDto.setToken(token);
		tokenInfoDto.setMyMeetingNo(userInfo.getMeetingNo());

		if (appConfig.getAdminEmails().contains(email)) {
			tokenInfoDto.setAdmin(true);
		}

		redisUtils.saveTokenUserInfoDto(tokenInfoDto);

		UserInfoVO userInfoVO = CopyTools.copy(tokenInfoDto, UserInfoVO.class);
		userInfoVO.setAdmin(tokenInfoDto.getAdmin());
		userInfoVO.setToken(token);


		return userInfoVO;


	}



}