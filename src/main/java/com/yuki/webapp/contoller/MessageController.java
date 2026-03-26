package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.Message;
import com.yuki.webapp.pojo.MessageDTO;
import com.yuki.webapp.pojo.Result;
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

    // 查询参与者的所有信息
    @GetMapping("/memberMessage")
    public Result<List<MessageDTO>> memberMessage(
            @RequestParam("userId") Integer userId,
            @RequestParam("isRead") Boolean isRead
    ){
        return messageService.getMemberMessage(userId,isRead);
    }

    // 查询某一用户创建的竞赛的所有未录取的成员
    @GetMapping("/unadmittedMembers")
    public Result<List<Map<String, Object>>> unadmittedMembers(@RequestParam("userId") Integer userId){
        List<Map<String, Object>> data = messageService.getUnadmittedMembers(userId);
        return Result.success(data);
    }

    // 创建信息
    @PostMapping("/createMessage")
    public Result createMessage(@RequestBody Message message){
        messageService.createMessage(message);
        return Result.success();
    }

    // 将信息标记为已读
    @PutMapping("/read")
    public Result read(@RequestParam("messageId") Integer messageId){
        messageService.read(messageId);
        return Result.success();
    }


}
