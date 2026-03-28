package com.yuki.webapp.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Meilisearch 配置
 * 启动时自动创建 competitions 索引（如果不存在）
 */
@Configuration
public class MeilisearchConfig {

    @Value("${meilisearch.host}")
    private String host;

    @Value("${meilisearch.api-key}")
    private String apiKey;

    @Value("${meilisearch.index-name}")
    private String indexName;

    @Bean
    public Client meilisearchClient() {
        Client client = new Client(new Config(host, apiKey));
        createIndexIfNotExists(client);
        return client;
    }

    private void createIndexIfNotExists(Client client) {
        try {
            client.getIndex(indexName);
        } catch (Exception e) {
            // 索引不存在则创建，主键设为 competitionId
            try {
                client.createIndex(indexName, "competitionId");
                // 配置可搜索字段（title、tags、details 参与全文检索）
                client.getIndex(indexName).updateSearchableAttributesSettings(
                        new String[]{"title", "tag1", "tag2", "tag3", "tag4", "tag5",
                                "competitionDetails", "schoolRequirements"}
                );
                // 配置可过滤字段（用于截止日期、难度筛选）
                client.getIndex(indexName).updateFilterableAttributesSettings(
                        new String[]{"deadline", "maxParticipants"}
                );
            } catch (Exception ex) {
                throw new RuntimeException("创建 Meilisearch 索引失败: " + ex.getMessage(), ex);
            }
        }
    }
}