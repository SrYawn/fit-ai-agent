# 设计说明

详见方案文件：`.claude/plans/temporal-doodling-sky.md`

## 核心设计决策
1. Session 记忆用内存 ConcurrentHashMap，不修改 BaseAgent 生命周期，历史以文本注入 userPrompt
2. CompanionMotivationAgent 替换 ChatAgent，分层 Prompt 体系（L1-L4）
3. 激励知识复用现有 ES 索引，通过 category=motivation 过滤
4. EmotionDetectionTool 基于 LLM 二次调用实现情绪分析
