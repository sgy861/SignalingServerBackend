package com.easymeeting.controller;

import com.easymeeting.entity.vo.CheckCodeVO;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.entity.vo.UserInfoVO;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.redis.RedisComponent;
import com.easymeeting.service.UserInfoService;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@RestController
@RequestMapping("account")
@Validated
@Slf4j
public class AccountController extends ABaseController {
    @Resource
    private UserInfoService userInfoService;

    @Resource
    private RedisComponent redisComponent;
    private NotEmptyValidatorForMap notEmptyValidatorForMap;

    @RequestMapping("checkCode")
    public ResponseVO checkCode(){
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100,42);
        String code = captcha.text();
        String checkCodeKey = redisComponent.saveCheckCode(code);
        String checkCodeBase64 = captcha.toBase64();
        log.info("code:{}" , code);
        CheckCodeVO checkCodeVO = new CheckCodeVO();
        checkCodeVO.setCheckCode(checkCodeBase64);
        checkCodeVO.setCheckCodeKey(checkCodeKey);
        return getSuccessResponseVO(checkCodeVO);
    }

    @RequestMapping("register")
    public ResponseVO register(//@NotEmpty String checkCodeKey,
                               @NotEmpty @Email String email,
                               @NotEmpty @Size(max = 20) String nickName,
                               @NotEmpty @Size(max = 20) String password
                               ){
        try{
            /**
             * 删除验证码功能便于测试
             */
//            if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){
//                throw new BusinessException("图片验证码不正确");
//            }
            this.userInfoService.register(email , nickName , password);
            return getSuccessResponseVO(null);
        }finally {
//            redisComponent.deleteCheckCode(checkCodeKey);
        }

    }

    @RequestMapping("login")
    public ResponseVO login(
                            @NotEmpty @Email String email,
                               @NotEmpty @Size(max = 20) String password
                               ){
        try{
            /**
             * 删除验证码功能便于测试
             */
//            if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){
//                throw new BusinessException("图片验证码不正确");
//            }
            UserInfoVO userInfoVo = this.userInfoService.login(email, password);
            return getSuccessResponseVO(userInfoVo);
        }finally {
//            redisComponent.deleteCheckCode(checkCodeKey);
        }

    }

}
