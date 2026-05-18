package com.zsr.fitaiagent.config;

import com.zsr.fitaiagent.chatmemory.JdbcSessionChatMemory;
import com.zsr.fitaiagent.service.ChatSessionService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Session 级对话记忆配置
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory sessionChatMemory(JdbcTemplate jdbcTemplate, ChatSessionService chatSessionService) {
        // 从 DB 读取最近 20 条消息作为上下文窗口
        return new JdbcSessionChatMemory(jdbcTemplate, chatSessionService, 20);
    }
}
