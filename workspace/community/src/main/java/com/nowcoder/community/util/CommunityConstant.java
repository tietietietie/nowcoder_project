package com.nowcoder.community.util;

public interface CommunityConstant {
    int ACTIVATION_SUCCESS = 0;
    int ACTIVATION_REPEAT = 1;
    int ACTIVATION_FAILURE = 2;


    //默认状态的登录凭证超时时间
    int DEFALUT_EXPIRED_SECONDS = 3600 * 12;

    //
    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;
}
