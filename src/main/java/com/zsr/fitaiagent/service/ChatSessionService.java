package com.zsr.fitaiagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChatSessionService {

    private static final int MAX_TITLE_LENGTH = 255;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void ensureSession(Long userId, String sessionId, String firstUserMessage) {
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(
                "SELECT user_id FROM chat_session WHERE session_id = ?",
                sessionId
        );

        if (sessions.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO chat_session (session_id, user_id, title) VALUES (?, ?, ?)",
                    sessionId,
                    userId,
                    buildTitle(firstUserMessage)
            );
            log.info("创建聊天会话: sessionId={}, userId={}", sessionId, userId);
            return;
        }

        Number ownerId = (Number) sessions.get(0).get("user_id");
        if (ownerId == null || ownerId.longValue() != userId) {
            throw new IllegalArgumentException("会话不存在或无权限访问");
        }
    }

    public List<Map<String, Object>> listSessions(Long userId) {
        return jdbcTemplate.queryForList(
                """
                SELECT session_id AS sessionId,
                       title,
                       created_at AS createdAt,
                       updated_at AS updatedAt,
                       last_message_at AS lastMessageAt
                FROM chat_session
                WHERE user_id = ?
                ORDER BY last_message_at DESC, created_at DESC
                """,
                userId
        );
    }

    public List<Map<String, Object>> listMessages(Long userId, String sessionId) {
        if (!ownsSession(userId, sessionId)) {
            throw new IllegalArgumentException("会话不存在或无权限访问");
        }

        return jdbcTemplate.queryForList(
                """
                SELECT id,
                       LOWER(role) AS role,
                       content,
                       created_at AS createdAt
                FROM chat_message
                WHERE user_id = ? AND session_id = ?
                ORDER BY created_at ASC, id ASC
                """,
                userId,
                sessionId
        );
    }

    public boolean ownsSession(Long userId, String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM chat_session WHERE user_id = ? AND session_id = ?",
                Integer.class,
                userId,
                sessionId
        );
        return count != null && count > 0;
    }

    private String buildTitle(String firstUserMessage) {
        String normalized = firstUserMessage == null ? "新会话" : firstUserMessage.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return "新会话";
        }
        return normalized.length() <= MAX_TITLE_LENGTH ? normalized : normalized.substring(0, MAX_TITLE_LENGTH);
    }
}
