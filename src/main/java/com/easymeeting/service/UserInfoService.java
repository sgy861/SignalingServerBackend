package com.easymeeting.service;

import java.util.List;

import com.easymeeting.entity.query.UserInfoQuery;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.vo.PaginationResultVO;
import com.easymeeting.entity.vo.UserInfoVO;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;


/**
 *  业务接口
 */
public interface UserInfoService {

	/**
	 * 根据条件查询列表
	 */
	List<UserInfo> findListByParam(UserInfoQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(UserInfoQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param);

	/**
	 * 新增
	 */
	Integer add(UserInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserInfo> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<UserInfo> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(UserInfo bean,UserInfoQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(UserInfoQuery param);

	/**
	 * 根据UserId查询对象
	 */
	UserInfo getUserInfoByUserId(String userId);


	/**
	 * 根据UserId修改
	 */
	Integer updateUserInfoByUserId(UserInfo bean,String userId);


	/**
	 * 根据UserId删除
	 */
	Integer deleteUserInfoByUserId(String userId);

	void register(@NotEmpty @Email String email, @NotEmpty @Size(max = 20) String nickName, @NotEmpty @Size(max = 20) String password);

	UserInfo getUserInfoByEmail(String email);

	Integer updateUserInfoByEmail(UserInfo bean, String email);

	void deleteUserInfoByEmail(String email);

	UserInfoVO login(String email , String password);
}