# 决策日志

## 2026-04-11: Session 记忆方案选型
- 选择内存 ConcurrentHashMap 而非 FileBasedChatMemory，因为需求明确不跨会话持久化
- 历史以文本注入 userPrompt 而非修改 BaseAgent，最小侵入

## 2026-04-11: 激励知识库方案
- 复用现有 ES 索引 fitness-knowledge，通过 category=motivation 过滤，避免管理多个 VectorStore Bean
