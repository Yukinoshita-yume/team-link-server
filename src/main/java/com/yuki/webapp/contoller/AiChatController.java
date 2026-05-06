package com.yuki.webapp.contoller;

import com.alibaba.fastjson.JSONArray;
import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.utils.DashScopeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 多轮对话接口
 * POST /ai/chat
 */
@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private DashScopeUtil dashScopeUtil;

    /**
     * 系统提示词
     */
    private static final String SYSTEM_PROMPT =
            "你是 CompeteHub 平台的专属 AI 竞赛助手，由通义千问驱动。\n" +
                    "CompeteHub 是一个面向高校学生的竞赛组队平台，帮助学生发现竞赛、组建队伍、参加比赛。\n\n" +
                    "你的核心能力：\n" +
                    "1. 竞赛推荐：根据用户的专业、技能、兴趣推荐合适的竞赛类型\n" +
                    "2. 组队策略：帮助用户分析队伍组成，提供招募队友的建议\n" +
                    "3. 申请指导：协助撰写报名申请书、自我介绍、项目说明\n" +
                    "4. 备赛规划：制定竞赛备战时间表和学习路线\n" +
                    "5. 经验分享：提供各类竞赛的常见技巧和注意事项\n\n" +
                    "回答风格：\n" +
                    "- 简洁专业，友好亲切，使用中文\n" +
                    "- 适当使用编号列表提升可读性\n" +
                    "- 回答长度适中，重点突出，不冗余\n" +
                    "- 如涉及具体竞赛报名截止日期等实时数据，提示用户在平台上查看最新信息";

    /**
     * 多轮对话主接口
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, Object> body) {
        try {
            Object messagesObj = body.get("messages");
            if (messagesObj == null) {
                return Result.error("messages 不能为空");
            }

            JSONArray messages = JSONArray.parseArray(JSONArray.toJSONString(messagesObj));
            if (messages == null || messages.isEmpty()) {
                return Result.error("messages 不能为空");
            }

            // 验证最后一条必须是用户消息
            String lastRole = messages.getJSONObject(messages.size() - 1).getString("role");
            if (!"user".equals(lastRole)) {
                return Result.error("最后一条消息必须是用户消息");
            }

            String reply = dashScopeUtil.chatWithHistory(SYSTEM_PROMPT, messages);
            return Result.success(reply);

        } catch (Exception e) {
            return Result.error("AI 服务暂时不可用，请稍后重试");
        }
    }
}