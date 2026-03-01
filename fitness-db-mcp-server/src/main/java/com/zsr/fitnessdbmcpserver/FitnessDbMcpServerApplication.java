package com.zsr.fitnessdbmcpserver;

import com.zsr.fitnessdbmcpserver.tools.FitnessDbTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FitnessDbMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitnessDbMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider fitnessDbTools(FitnessDbTool fitnessDbTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(fitnessDbTool)
                .build();
    }

}
