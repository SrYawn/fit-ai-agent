package com.zsr.fitaiagent.config;

import com.zsr.fitaiagent.chatmemory.InMemorySessionChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Session 级对话记忆配置
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory sessionChatMemory() {
        // 滑动窗口 20 条消息，30 分钟 TTL
        return new InMemorySessionChatMemory(20, 30);
    }
}
