package com.zsr.fitaiagent.chatmemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的 Session 级对话记忆
 * 仅保留 UserMessage 和 AssistantMessage（最终文本回复），不存中间工具调用消息
 * 使用滑动窗口控制上下文长度，内置 TTL 自动清除过期 session
 */
public class InMemorySessionChatMemory implements ChatMemory {

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final int maxMessages;
    private final long ttlMillis;

    /**
     * @param maxMessages 滑动窗口大小（保留最近 N 条消息）
     * @param ttlMinutes  session 过期时间（分钟）
     */
    public InMemorySessionChatMemory(int maxMessages, long ttlMinutes) {
        this.maxMessages = maxMessages;
        this.ttlMillis = ttlMinutes * 60 * 1000;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        SessionEntry entry = sessions.computeIfAbsent(conversationId, k -> new SessionEntry());
        entry.touchAccessTime();
        // 只保留 UserMessage 和 AssistantMessage
        for (Message message : messages) {
            if (message instanceof UserMessage || message instanceof AssistantMessage) {
                entry.messages.add(message);
            }
        }
        // 滑动窗口裁剪
        while (entry.messages.size() > maxMessages) {
            entry.messages.remove(0);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        cleanExpiredSessions();
        SessionEntry entry = sessions.get(conversationId);
        if (entry == null) {
            return new ArrayList<>();
        }
        entry.touchAccessTime();
        return new ArrayList<>(entry.messages);
    }

    @Override
    public void clear(String conversationId) {
        sessions.remove(conversationId);
    }

    /**
     * 将历史消息格式化为文本块，用于注入 agent 的 userPrompt
     */
    public String formatHistoryAsText(String conversationId) {
        List<Message> history = get(conversationId);
        if (history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【对话历史】\n");
        for (Message msg : history) {
            if (msg instanceof UserMessage) {
                sb.append("用户: ").append(msg.getText()).append("\n");
            } else if (msg instanceof AssistantMessage) {
                sb.append("助手: ").append(msg.getText()).append("\n");
            }
        }
        return sb.toString();
    }

    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry ->
                (now - entry.getValue().lastAccessTime) > ttlMillis
        );
    }

    private static class SessionEntry {
        final List<Message> messages = new ArrayList<>();
        volatile long lastAccessTime;

        SessionEntry() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        void touchAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
