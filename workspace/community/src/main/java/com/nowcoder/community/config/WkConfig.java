package com.nowcoder.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class WkConfig {

    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class);

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    //在服务启动的时候创建目录
    @PostConstruct
    public void init() {
        File file = new File(wkImageStorage);
        if (!file.exists()) {
            file.mkdir();
            logger.info("创建wk图片目录： " + wkImageStorage);
        }
    }


}
