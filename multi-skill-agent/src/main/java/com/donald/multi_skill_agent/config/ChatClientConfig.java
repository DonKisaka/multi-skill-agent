package com.donald.multi_skill_agent.config;

import com.donald.multi_skill_agent.skills.Skill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientCustomizer;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    ChatClientCustomizer systemPromptCustomizer(List<Skill> skills) {
        String instructions = "You are a helpful personal assistant.\n\n" +
                "The current session ID is: {sessionId}\n\n" +
                skills.stream()
                        .map(Skill::getInstructions)
                        .collect(Collectors.joining("\n"));
        return builder -> builder.defaultSystem(instructions);
    }

    @Bean
    ChatClientCustomizer toolsCustomizer(List<Skill> skills) {
        ToolCallback[] allTools = skills.stream()
                .flatMap(skill -> Arrays.stream(skill.getTools()))
                .toArray(ToolCallback[]::new);

        return builder -> builder.defaultTools(allTools);
    }

    @Bean
    MessageWindowChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    ChatClientCustomizer chatMemoryCustomizer(MessageWindowChatMemory chatMemory) {
        return builder -> builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
        );
    }
}
