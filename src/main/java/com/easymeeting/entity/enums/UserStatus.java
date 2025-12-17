package com.easymeeting.entity.enums;

public enum UserStatus {
    DISABLE(0 , "禁用") , ENABLE(1 , "启用") ;

    private Integer status;

    private String description;
    UserStatus(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    public UserStatus getByStatus(Integer status) {
        for (UserStatus userStatus : UserStatus.values()) {
            if (userStatus.getStatus().equals(status)) {
                return userStatus;
            }
        }
        return null;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }


}
