package com.campus.recruitment.module.message.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.message.service.MessageService;
import com.campus.recruitment.module.message.vo.MessageVO;
import com.campus.recruitment.module.message.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final MessageService messageService;

    @RequireLogin
    @GetMapping("/my")
    public R<PageResult<MessageVO>> getMyMessages(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String messageType) {
        return R.ok(messageService.getMyMessages(pageNum, pageSize, messageType));
    }

    @RequireLogin
    @GetMapping("/unread-count")
    public R<UnreadCountVO> getUnreadCount() {
        return R.ok(messageService.getUnreadCount());
    }

    @RequireLogin
    @PutMapping("/{id}/read")
    public R<Void> markAsRead(@PathVariable("id") Long id) {
        messageService.markAsRead(id);
        return R.ok();
    }

    @RequireLogin
    @DeleteMapping("/{id}")
    public R<Void> deleteMessage(@PathVariable("id") Long id) {
        messageService.deleteMessage(id);
        return R.ok();
    }
}
