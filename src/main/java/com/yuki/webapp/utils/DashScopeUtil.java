package com.yuki.webapp.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通义千问 API 工具类
 * - getEmbedding()：文本向量化，用于 Qdrant 存储和检索
 * - chat()：对话生成，用于查询改写、推荐理由生成等
 */
@Component
public class DashScopeUtil {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.embedding-model}")
    private String embeddingModel;

    @Value("${dashscope.chat-model}")
    private String chatModel;

    private static final String EMBEDDING_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 获取文本的向量表示（1536维）
     * 用于将竞赛信息写入Qdrant，以及将用户查询向量化后搜索
     */
    public List<Float> getEmbedding(String text) {
        JSONObject body = new JSONObject();
        body.put("model", embeddingModel);
        JSONObject input = new JSONObject();
        input.put("texts", List.of(text));
        body.put("input", input);
        JSONObject parameters = new JSONObject();
        parameters.put("text_type", "query");
        body.put("parameters", parameters);

        Request request = new Request.Builder()
                .url(EMBEDDING_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(body.toJSONString(), JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JSONObject result = JSON.parseObject(responseBody);
            JSONArray embeddings = result
                    .getJSONObject("output")
                    .getJSONArray("embeddings");
            JSONArray vector = embeddings.getJSONObject(0).getJSONArray("embedding");
            return vector.toJavaList(Float.class);
        } catch (IOException e) {
            throw new RuntimeException("调用通义千问Embedding接口失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用通义千问对话接口
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @param temperature  温度（0.0-1.0），越低越稳定
     * @return AI生成的文本
     */
    public String chat(String systemPrompt, String userMessage, double temperature) {
        JSONObject body = new JSONObject();
        body.put("model", chatModel);
        body.put("temperature", temperature);

        JSONArray messages = new JSONArray();
        JSONObject sys = new JSONObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        messages.add(sys);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        body.put("messages", messages);

        Request request = new Request.Builder()
                .url(CHAT_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(body.toJSONString(), JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JSONObject result = JSON.parseObject(responseBody);
            return result
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (IOException e) {
            throw new RuntimeException("调用通义千问Chat接口失败: " + e.getMessage(), e);
        }
    }
}
