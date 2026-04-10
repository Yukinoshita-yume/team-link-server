package com.yuki.webapp.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 单一 Qdrant 连接：与现有 {@code qdrant.host}/{@code qdrant.port} 配置一致，
 * 启动时确保「竞赛」collection 存在（同名已存在则跳过）。
 */
@Configuration
public class QdrantConfig {

    @Bean
    public QdrantClient qdrantClient(
            @Value("${qdrant.host}") String host,
            @Value("${qdrant.port}") int port,
            @Value("${qdrant.collection-name}") String competitionsCollection,
            @Value("${qdrant.vector-size:1024}") int vectorSize
    ) throws ExecutionException, InterruptedException {
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build()
        );
        createCollectionIfNotExists(client, competitionsCollection, vectorSize);
        return client;
    }

    private static void createCollectionIfNotExists(QdrantClient client, String collectionName, int vectorSize)
            throws ExecutionException, InterruptedException {
        List<String> collections = client.listCollectionsAsync().get();
        if (collections.contains(collectionName)) {
            return;
        }
        client.createCollectionAsync(
                collectionName,
                VectorParams.newBuilder()
                        .setSize(vectorSize)
                        .setDistance(Distance.Cosine)
                        .build()
        ).get();
    }
}
