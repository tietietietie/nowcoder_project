package com.nowcoder.community.config;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import quartz.PostScoreReferenceJob;

@Configuration
public class QuartzConfig {
    //FactoryBean，可以简化Bean的实例化过程
    //1.通过factoryBean封装Bean的实例化过程
    //2.将FactorBean装配到容器里
    //3.FactoryBean注入给其他的Bean，
    //4.其他的Bean得到了FactoryBean所管理的实例对象

//    @Bean
//    public JobDetailFactoryBean alphaJobDetail() {
//        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
//        factoryBean.setJobClass(AlphaJob.class);
//        factoryBean.setName("alphaJob");
//        factoryBean.setGroup("alphaGroup");
//        //长久保存
//        factoryBean.setDurability(true);
//        factoryBean.setRequestsRecovery(true);
//        return factoryBean;
//    }
//
//    @Bean
//    public SimpleTriggerFactoryBean simpleTrigger(JobDetail alphaJobDetail) {
//        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
//        factoryBean.setJobDetail(alphaJobDetail);
//        factoryBean.setName("alphaTrigger");
//        factoryBean.setGroup("alphaTriggerGroup");
//        factoryBean.setRepeatInterval(3000);
//        factoryBean.setJobDataMap(new JobDataMap());
//        return factoryBean;
//    }

    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreReferenceJob.class);
        factoryBean.setName("postScoreReferenceJob");
        factoryBean.setGroup("communityJobGroup");
        //长久保存
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    //刷新帖子分数
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }
}




