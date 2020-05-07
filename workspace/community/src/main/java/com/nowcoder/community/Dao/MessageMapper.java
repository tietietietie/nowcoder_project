package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    //查询会话列表一页数据，每个会话，只返回一条最新的私信
    List<Message> selectConversations(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit);

    //查询会话列表总行数
    int selectConversationCount(@Param("userId") int userId);

    //查询详情的总消息数
    List<Message> selectLetters(@Param("conversationId") String conversationId, @Param("offset") int offset, @Param("limit") int limit);

    //查询详情的当前页面消息
    int selectLetterCount(@Param("conversationId") String conversationId);

    //查询未读消息数量
    int selectLetterUnreadCount(@Param("userId") int userId, @Param("conversationId") String conversationId);

    //新增消息
    int insertMessage(Message message);

    //修改状态
    int updateStatus(@Param("ids") List<Integer> ids, @Param("status") int status);
}
