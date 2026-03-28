package com.zsr.fitaiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.function.Consumer;

/**
 * 闲聊 Agent
 * 处理用户的问候、闲聊等非健身相关的对话
 */
@Slf4j
public class ChatAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个友好、热情的健身助手。你的任务是：

            1. 与用户进行友好的对话交流
            2. 回应用户的问候、感谢等社交性对话
            3. 在适当的时候引导用户使用健身相关功能

            **对话风格**：
            - 友好、热情、积极
            - 简洁明了，不要过于冗长
            - 适当使用表情符号增加亲和力
            - 在闲聊中自然地提及健身话题

            **重要规则**：
            - 保持对话简短，1-3 句话即可
            - 如果用户只是简单问候（如"你好"），简短回应后可以询问是否需要健身帮助
            - 如果用户表示感谢，礼貌回应并鼓励继续使用
            - 不要主动结束对话，让用户决定下一步
            - **回复完成后，调用 doTerminate 工具结束任务**
            """;

    public ChatAgent(ToolCallback[] availableTools, ChatClient chatClient) {
        super(availableTools);
        this.setName("闲聊Agent");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setChatClient(chatClient);
        this.setMaxSteps(3);
    }

    /**
     * 处理闲聊对话
     *
     * @param userInput 用户输入
     * @return 回复内容
     */
    public String chat(String userInput) {
        String userPrompt = String.format(
                "用户说：%s\n\n请给出友好的回复。",
                userInput
        );
        return this.run(userPrompt);
    }

    public String streamChat(String userInput, Consumer<String> tokenConsumer) {
        String userPrompt = String.format(
                "用户说：%s\n\n请给出友好的回复。",
                userInput
        );
        return this.runWithStreamingFinalAnswer(userPrompt, tokenConsumer);
    }
}
