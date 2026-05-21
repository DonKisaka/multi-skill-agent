package com.donald.multi_skill_agent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AssistantAgent {

    private final ChatClient chatClient;

    public AssistantAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public Flux<String> chat(String sessionId, String userMessage) {
        return chatClient.prompt()
                .system(s -> s.param("sessionId", sessionId))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(userMessage)
                .stream()
                .content();
    }
}
