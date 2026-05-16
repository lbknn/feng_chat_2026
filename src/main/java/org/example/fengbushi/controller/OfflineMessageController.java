package org.example.fengbushi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.entity.mongodb.Message;
import org.example.fengbushi.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 与 {@link org.example.fengbushi.service.CrossServerClient} 使用的路径一致，
 * 供集群内其它节点拉取本机 MongoDB 中的消息体。
 */
@Slf4j
@RestController
@RequestMapping("/api/offline-message")
@RequiredArgsConstructor
public class OfflineMessageController {

    private final MessageService messageService;

    @GetMapping("/fetch/{msgId}")
    public ApiResponse<Message> fetch(@PathVariable String msgId) {
        log.info("跨服务器获取消息 /api/offline-message/fetch: msgId={}", msgId);
        return messageService.prepareCrossServerFetchMessage(msgId);
    }
}
