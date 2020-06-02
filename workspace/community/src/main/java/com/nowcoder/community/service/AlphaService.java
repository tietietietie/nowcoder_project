package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

@Service
//@Scope("prototype")
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService() {
        System.out.println("实例化AlphaService");
    }

    @PostConstruct
    public void init() {
        System.out.println("初始化AlphaService");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("销毁AlphaService");
    }

    public String find() {
        return alphaDao.select();
    }

    //新增用户+自动发一个新人贴
    //传播机制解释：两个事务交叉在一起，如何管理
    //REQUIRED：支持当前事务（支持外部事务）
    //REQUIRES_NEW
    //NESTED:如果当前存在事务（外部事务），则嵌套在该事务中执行
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1() {
        //新建用户
        User user = new User();
        user.setUsername("aplha");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("Alpha@qq.com");
        user.setHeaderUrl("http://image/nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        //新建帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("新人贴");
        post.setContent("新人报到，多多指教");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);
        //报错前会把数据插入吗？
        Integer.valueOf("abc");
        return "ok";
    }

    public Object save2() {
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute(new TransactionCallback<Object>() {

            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                //新建用户
                User user = new User();
                user.setUsername("beta");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("beta@qq.com");
                user.setHeaderUrl("http://image/nowcoder.com/head/999.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);
                //新建帖子
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setTitle("新人贴,我叫beta");
                post.setContent("新人报到，多多指教~");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);
                //报错前会把数据插入吗？
                Integer.valueOf("abc");
                return "ok";
            }
        });
    }

    @Async
    public void execute1() {
        System.out.println("当前执行线程为" + Thread.currentThread().getName());
    }

//    @Scheduled(initialDelay = 10000, fixedDelay = 1000)
//    public void scheduled1() {
//        System.out.println("当前执行定时任务，执行线程为 --->" + Thread.currentThread().getName());
//    }
}
