package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                return operations.exec();
            }
        });
    }

    public void unFollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);
                return operations.exec();
            }
        });
    }

    //查询关注实体的数量
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().size(followeeKey);
    }

    //查询粉丝数量
    public long fingFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().size(followerKey);
    }

    //显示关注状态
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        Double status = redisTemplate.opsForZSet().score(followeeKey, entityId);
        return status != null;
    }

    //查询某个用户关注的人
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if (targetIds != null && !targetIds.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Integer targetId : targetIds) {
                Map<String, Object> map = new HashMap<>();
                User user = userService.findUserById(targetId);
                map.put("user", user);
                Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
                map.put("followTime", new Date(score.longValue()));
                list.add(map);
            }
            return list;
        }
        return null;
    }

    //查询某用户的粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        //set虽然是无序的，但是redis做了内置处理，变得有序
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        if (targetIds != null && !targetIds.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Integer targetId : targetIds) {
                Map<String, Object> map = new HashMap<>();
                User user = userService.findUserById(targetId);
                map.put("user", user);
                Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
                map.put("followTime", new Date(score.longValue()));
                list.add(map);
            }
            return list;
        }
        return null;
    }
}
