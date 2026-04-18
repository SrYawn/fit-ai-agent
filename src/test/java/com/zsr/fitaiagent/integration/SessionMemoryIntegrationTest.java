package com.zsr.fitaiagent.integration;

import com.zsr.fitaiagent.graph.FitnessWorkflowGraph;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多轮会话记忆功能集成测试
 * 验证 sessionId 机制和对话历史上下文注入
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class SessionMemoryIntegrationTest {

    @Autowired
    private FitnessWorkflowGraph fitnessWorkflowGraph;

    @Test
    @DisplayName("测试1: 首次请求自动生成 sessionId")
    void testSessionIdGeneration() {
        String userInput = "你好";
        Long userId = 1L;
        String sessionId = null; // 不传 sessionId

        var response = fitnessWorkflowGraph.executeWorkflow(userInput, userId, sessionId);

        assertNotNull(response, "响应不应为空");
        assertNotNull(response.sessionId(), "sessionId 应该被自动生成");
        assertEquals("completed", response.status(), "状态应为 completed");
        assertNotNull(response.result(), "结果不应为空");

        log.info("✓ 测试1通过 - sessionId: {}", response.sessionId());
        log.info("AI 回复: {}", response.result());
    }

    @Test
    @DisplayName("测试2: 多轮对话 - 意图承接（计划生成场景）")
    void testMultiTurnConversation_PlanGeneration() throws InterruptedException {
        Long userId = 1L;
        String sessionId = "test-session-plan-" + System.currentTimeMillis();

        // 第一轮：用户提出减肥计划需求
        log.info("\n=== 第一轮对话 ===");
        String input1 = "帮我制定一个减肥计划";
        var response1 = fitnessWorkflowGraph.executeWorkflow(input1, userId, sessionId);

        assertEquals("completed", response1.status());
        assertEquals("plan_generation", response1.intent(), "第一轮应识别为 plan_generation");
        assertNotNull(response1.result());
        assertTrue(response1.result().contains("减肥") || response1.result().contains("训练") || response1.result().contains("计划"),
                "第一轮回复应包含计划相关内容");

        log.info("第一轮意图: {}", response1.intent());
        log.info("第一轮回复: {}", response1.result().substring(0, Math.min(200, response1.result().length())) + "...");

        // 等待一下，模拟真实对话间隔
        Thread.sleep(1000);

        // 第二轮：用户追加需求（承接性请求）
        log.info("\n=== 第二轮对话 ===");
        String input2 = "加上饮食建议";
        var response2 = fitnessWorkflowGraph.executeWorkflow(input2, userId, sessionId);

        assertEquals("completed", response2.status());
        assertEquals("plan_generation", response2.intent(), "第二轮应识别为 plan_generation（承接上下文）");
        assertNotNull(response2.result());
        assertTrue(response2.result().contains("饮食") || response2.result().contains("营养") || response2.result().contains("蛋白"),
                "第二轮回复应包含饮食相关内容");

        log.info("第二轮意图: {}", response2.intent());
        log.info("第二轮回复: {}", response2.result().substring(0, Math.min(200, response2.result().length())) + "...");

        log.info("\n✓ 测试2通过 - 意图承接成功，第二轮正确识别为 plan_generation");
    }

    @Test
    @DisplayName("测试3: 多轮对话 - 陪伴激励场景的情绪追踪")
    void testMultiTurnConversation_EmotionTracking() throws InterruptedException {
        Long userId = 1L;
        String sessionId = "test-session-emotion-" + System.currentTimeMillis();

        // 第一轮：用户表达疲惫
        log.info("\n=== 第一轮对话 ===");
        String input1 = "最近训练好累，不想练了";
        var response1 = fitnessWorkflowGraph.executeWorkflow(input1, userId, sessionId);

        assertEquals("completed", response1.status());
        assertEquals("chat", response1.intent(), "应识别为 chat（陪伴激励）");
        assertNotNull(response1.result());

        log.info("第一轮意图: {}", response1.intent());
        log.info("第一轮回复: {}", response1.result());

        Thread.sleep(1000);

        // 第二轮：用户继续表达（应该能引用上一轮的情绪）
        log.info("\n=== 第二轮对话 ===");
        String input2 = "你说得对，我再试试";
        var response2 = fitnessWorkflowGraph.executeWorkflow(input2, userId, sessionId);

        assertEquals("completed", response2.status());
        assertEquals("chat", response2.intent());
        assertNotNull(response2.result());

        log.info("第二轮意图: {}", response2.intent());
        log.info("第二轮回复: {}", response2.result());

        log.info("\n✓ 测试3通过 - 陪伴激励场景的多轮对话成功");
    }

    @Test
    @DisplayName("测试4: 不同 sessionId 的对话隔离")
    void testSessionIsolation() {
        Long userId = 1L;
        String sessionId1 = "test-session-1-" + System.currentTimeMillis();
        String sessionId2 = "test-session-2-" + System.currentTimeMillis();

        // Session 1: 讨论减肥
        String input1 = "我想减肥";
        var response1 = fitnessWorkflowGraph.executeWorkflow(input1, userId, sessionId1);
        assertEquals("plan_generation", response1.intent());

        // Session 2: 讨论增肌（不同 session，不应受 session1 影响）
        String input2 = "我想增肌";
        var response2 = fitnessWorkflowGraph.executeWorkflow(input2, userId, sessionId2);
        assertEquals("plan_generation", response2.intent());

        // Session 1 继续: 说"加上饮食"应该承接减肥上下文
        String input3 = "加上饮食建议";
        var response3 = fitnessWorkflowGraph.executeWorkflow(input3, userId, sessionId1);
        assertEquals("plan_generation", response3.intent());
        // 回复应该与减肥相关，而非增肌
        assertTrue(response3.result().contains("减") || response3.result().contains("脂") || response3.result().contains("热量缺口"),
                "Session 1 的第二轮应该承接减肥上下文");

        log.info("✓ 测试4通过 - 不同 session 的对话正确隔离");
    }

    @Test
    @DisplayName("测试5: sessionId 在响应中正确返回")
    void testSessionIdInResponse() {
        String userInput = "你好";
        Long userId = 1L;
        String providedSessionId = "test-provided-session-" + System.currentTimeMillis();

        var response = fitnessWorkflowGraph.executeWorkflow(userInput, userId, providedSessionId);

        assertEquals(providedSessionId, response.sessionId(), "返回的 sessionId 应与传入的一致");
        log.info("✓ 测试5通过 - sessionId 正确返回");
    }
}
