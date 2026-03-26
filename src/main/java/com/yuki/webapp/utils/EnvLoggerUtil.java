//package com.yuki.webapp.utils;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class EnvLoggerUtil implements CommandLineRunner{
//    //在控制台和日志中打印数据库连接信息。
//
//    private static final Logger logger = LoggerFactory.getLogger(EnvLoggerUtil.class);
//
//    @Value("${DATABASE_URL}")
//    private String databaseUrl;
//
//    @Value("${DATABASE_USERNAME}")
//    private String databaseUsername;
//
//    @Value("${DATABASE_PASSWORD}")
//    private String databasePassword;
//
//    @Override
//    public void run(String... args) throws Exception {
//        logger.info("DATABASE_URL: {}", databaseUrl);
//        logger.info("DATABASE_USERNAME: {}", databaseUsername);
//        logger.info("DATABASE_PASSWORD: {}", databasePassword);
//    }
//}
