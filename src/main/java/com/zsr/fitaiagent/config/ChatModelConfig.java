package com.zsr.fitaiagent.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ChatModel 配置类，解决多个 ChatModel Bean 冲突问题
 * 将 DashScope ChatModel 设置为 Primary
 */
@Configuration
public class ChatModelConfig {

    @Bean
    @Primary
    public ChatModel primaryChatModel(@Qualifier("dashScopeChatModel") ChatModel dashScopeChatModel) {
        return dashScopeChatModel;
    }
}
