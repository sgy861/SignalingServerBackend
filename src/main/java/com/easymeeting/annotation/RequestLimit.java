package com.easymeeting.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestLimit {

    /**
     * 时间窗口，单位：秒
     */
    int timeWindow() default 10;

    /**
     * 最大允许访问次数
     */
    int maxCount() default 5;

    /**
     * 是否按用户限流（true: 使用 userId；false: 使用 IP）
     */
    boolean byUser() default true;
}

