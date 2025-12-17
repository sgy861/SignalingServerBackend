package com.easymeeting.config;

import com.sun.org.apache.bcel.internal.generic.INEG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
@Configuration
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Value("${ws.port}")
    private Integer wsPort;

    @Value("${project.folder}")
    private String projectFolder;

    @Value("${admin.emails}")
    private String adminEmails;

    public String getProjectFolder() {
        if(!StringUtils.isEmpty(projectFolder) && !projectFolder.endsWith("/")) {
            projectFolder = projectFolder + "/";
        }
        return projectFolder;
    }

    public Integer getWsPort() {
        return wsPort;
    }

    public String getAdminEmails() {
        return adminEmails;
    }



}
