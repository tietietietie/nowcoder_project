package com.nowcoder.community.util;

public interface CommunityConstant {
    int ACTIVATION_SUCCESS = 0;
    int ACTIVATION_REPEAT = 1;
    int ACTIVATION_FAILURE = 2;


    //默认状态的登录凭证超时时间
    int DEFALUT_EXPIRED_SECONDS = 3600 * 12;

    //
    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;

    //实体类型：帖子
    int ENTITY_TYPE_POST = 1;
    //实体类型：评论
    int ENTITY_TYPE_COMMENT = 2;
    //实体类型：用户
    int ENTITY_TYPE_USER = 3;

    //主题
    String TOPIC_COMMENT = "comment";
    String TOPIC_LIKE = "like";
    String TOPIC_FOLLOW = "follow";
    String TOPIC_PUBLISH = "publish";
    String TOPIC_DELETE = "delete";
    //系统用户ID
    int SYSTEM_USER_ID = 1;
    //权限：用户，管理员，版主
    String AUTHORITY_USER = "user";
    String AUTHORITY_ADMIN = "ADMIN";
    String AUTHORITY_MODERATOR = "moderator";

}
