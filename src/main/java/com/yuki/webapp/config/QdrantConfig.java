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

@Configuration
public class QdrantConfig {

    @Value("${qdrant.host}")
    private String host;

    @Value("${qdrant.port}")
    private int port;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    private static final int VECTOR_SIZE = 1024;

    @Bean
    public QdrantClient qdrantClient() throws ExecutionException, InterruptedException {
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build()
        );
        createCollectionIfNotExists(client);
        return client;
    }

    private void createCollectionIfNotExists(QdrantClient client)
            throws ExecutionException, InterruptedException {

        List<String> collections = client.listCollectionsAsync().get();
        if (collections.contains(collectionName)) {
            return;
        }

        client.createCollectionAsync(
                collectionName,
                VectorParams.newBuilder()
                        .setSize(VECTOR_SIZE)
                        .setDistance(Distance.Cosine)
                        .build()
        ).get();
    }
}