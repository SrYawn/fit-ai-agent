package com.zsr.fitaiagent.chatmemory;

import com.zsr.fitaiagent.service.ChatSessionService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 MySQL 持久化的 Session 级对话记忆。
 * 仅保留 UserMessage 和 AssistantMessage（最终文本回复），不存中间工具调用消息。
 */
public class JdbcSessionChatMemory implements ChatMemory {

    private final JdbcTemplate jdbcTemplate;
    private final ChatSessionService chatSessionService;
    private final int maxMessages;

    public JdbcSessionChatMemory(JdbcTemplate jdbcTemplate, ChatSessionService chatSessionService, int maxMessages) {
        this.jdbcTemplate = jdbcTemplate;
        this.chatSessionService = chatSessionService;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.isBlank() || messages == null || messages.isEmpty()) {
            return;
        }

        Long userId = getSessionUserId(conversationId);
        if (userId == null) {
            return;
        }

        for (Message message : messages) {
            String role = resolveRole(message);
            if (role == null) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO chat_message (session_id, user_id, role, content) VALUES (?, ?, ?, ?)",
                    conversationId,
                    userId,
                    role,
                    message.getText()
            );
        }

        jdbcTemplate.update(
                "UPDATE chat_session SET last_message_at = CURRENT_TIMESTAMP WHERE session_id = ?",
                conversationId
        );
    }

    @Override
    public List<Message> get(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT role, content
                FROM (
                    SELECT role, content, created_at, id
                    FROM chat_message
                    WHERE session_id = ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT ?
                ) recent_messages
                ORDER BY created_at ASC, id ASC
                """,
                conversationId,
                maxMessages
        );

        List<Message> messages = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String role = row.get("role") != null ? row.get("role").toString() : null;
            String content = row.get("content") != null ? row.get("content").toString() : "";
            if ("USER".equalsIgnoreCase(role)) {
                messages.add(new UserMessage(content));
            } else if ("ASSISTANT".equalsIgnoreCase(role)) {
                messages.add(new AssistantMessage(content));
            }
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        jdbcTemplate.update("DELETE FROM chat_message WHERE session_id = ?", conversationId);
        jdbcTemplate.update("DELETE FROM chat_session WHERE session_id = ?", conversationId);
    }

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

    public void ensureSession(Long userId, String sessionId, String firstUserMessage) {
        chatSessionService.ensureSession(userId, sessionId, firstUserMessage);
    }

    private Long getSessionUserId(String conversationId) {
        List<Long> userIds = jdbcTemplate.query(
                "SELECT user_id FROM chat_session WHERE session_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                conversationId
        );
        return userIds.isEmpty() ? null : userIds.getFirst();
    }

    private String resolveRole(Message message) {
        if (message instanceof UserMessage) {
            return "USER";
        }
        if (message instanceof AssistantMessage) {
            return "ASSISTANT";
        }
        return null;
    }
}
