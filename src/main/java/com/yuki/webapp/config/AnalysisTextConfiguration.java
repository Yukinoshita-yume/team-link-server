package com.yuki.webapp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 {@link TextAnalysisProperties} 绑定，无需修改主应用类。
 */
@Configuration
@EnableConfigurationProperties(TextAnalysisProperties.class)
public class AnalysisTextConfiguration {
}
