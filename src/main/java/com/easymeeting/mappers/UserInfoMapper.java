package com.easymeeting.mappers;

import com.easymeeting.entity.po.UserInfo;
import org.apache.ibatis.annotations.Param;

/**
 *  数据库操作接口
 */
public interface UserInfoMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据UserId更新
	 */
	 Integer updateByUserId(@Param("bean") T t,@Param("userId") String userId);


	/**
	 * 根据UserId删除
	 */
	 Integer deleteByUserId(@Param("userId") String userId);
	 Integer updateByEmail(@Param("bean") T t, @Param("email") String email);

	/**
	 * 根据UserId获取对象
	 */
	 T selectByEmail(@Param("email") String email);

	 void deleteByEmail(@Param("email") String email);

}
