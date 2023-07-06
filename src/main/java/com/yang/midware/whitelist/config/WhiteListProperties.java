package com.yang.midware.whitelist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description:
 * @author：杨超
 * @date: 2023/7/6
 * @Copyright：
 */
@ConfigurationProperties("yang.whitelist")
public class WhiteListProperties {

    private String users;

    public String getUsers() {
        return users;
    }

    public void setUsers(String users) {
        this.users = users;
    }
}
