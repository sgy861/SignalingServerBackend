package com.easymeeting.controller;

import com.easymeeting.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Slf4j
@RequestMapping("/userContact")
public class UserContactController extends ABaseController{

    @RequestMapping("/loadContactApplyDealWithCount")
    public ResponseVO loadContactApplyDealWithCount() {

        return getSuccessResponseVO(0);
    }
}