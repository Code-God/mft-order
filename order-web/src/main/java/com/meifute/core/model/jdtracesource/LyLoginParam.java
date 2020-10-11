package com.meifute.core.model.jdtracesource;

import java.io.Serializable;

/**
 * @Classname LyLoginParam
 * @Description TODO
 * @Date 2020-06-09 16:13
 * @Created by MR. Xb.Wu
 */
public class LyLoginParam implements Serializable {

    private String A;

    private String username;

    private String password;

    public String getUsername() {
        return username;
    }

    public String getA() {
        return A;
    }

    public void setA(String a) {
        A = a;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    private String app;
}
