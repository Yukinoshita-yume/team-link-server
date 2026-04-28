package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.*;
import com.yuki.webapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 查询参与者的系统消息（审核通过/解散等）
    @GetMapping("/memberMessage")
    public Result<List<MessageDTO>> memberMessage(
            @RequestParam("userId") Integer userId,
            @RequestParam("isRead") Boolean isRead
    ) {
        return messageService.getMemberMessage(userId, isRead);
    }

    // 查询某一用户创建的竞赛的所有未录取的成员
    @GetMapping(value = "/unadmittedMembers", params = "userId")
    public Result<List<Map<String, Object>>> unadmittedMembers(Integer userId) {
        return Result.success(messageService.getUnadmittedMembers(userId));
    }

    // 查询某个竞赛的所有未录取的成员
    @GetMapping(value = "/unadmittedMembers", params = "competitionId")
    public Result<List<Map<String, Object>>> unadmittedMembersByCompetition(Integer competitionId) {
        return Result.success(messageService.getUnadmittedMembersByCompetition(competitionId));
    }

    // 创建系统消息
    @PostMapping("/createMessage")
    public Result createMessage(@RequestBody Message message) {
        messageService.createMessage(message);
        return Result.success();
    }

    // 将系统消息标记为已读
    @PutMapping("/read")
    public Result read(@RequestParam("messageId") Integer messageId) {
        messageService.read(messageId);
        return Result.success();
    }

    // ────────────── 私信接口 ──────────────

    // 发送私信
    @PostMapping("/direct/send")
    public Result sendDirect(@RequestBody Map<String, Object> body) {
        Integer senderId  = (Integer) body.get("senderId");
        Integer receiverId = (Integer) body.get("receiverId");
        String  content   = (String)  body.get("content");
        if (senderId == null || receiverId == null || content == null || content.isBlank()) {
            return Result.error("参数不完整");
        }
        messageService.sendDirectMessage(senderId, receiverId, content.trim());
        return Result.success();
    }

    // 查询与某人的对话记录，同时将对方发来的消息标记为已读
    @GetMapping("/direct/conversation")
    public Result<List<DirectMessageDTO>> conversation(
            @RequestParam("userId") Integer userId,
            @RequestParam("otherUserId") Integer otherUserId
    ) {
        // 打开对话即标记已读
        messageService.markConversationRead(userId, otherUserId);
        List<DirectMessageDTO> msgs = messageService.getConversation(userId, otherUserId);
        return Result.success(msgs);
    }

    // 查询我的私信会话列表
    @GetMapping("/direct/sessions")
    public Result<List<ChatSessionDTO>> sessions(@RequestParam("userId") Integer userId) {
        return Result.success(messageService.getChatSessions(userId));
    }

    // 查询私信未读总数（用于徽章）
    @GetMapping("/direct/unreadCount")
    public Result<Integer> unreadCount(@RequestParam("userId") Integer userId) {
        return Result.success(messageService.getUnreadDirectMessageCount(userId));
    }
}
