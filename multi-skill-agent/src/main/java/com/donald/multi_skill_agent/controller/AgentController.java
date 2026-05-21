package com.donald.multi_skill_agent.controller;

import com.donald.multi_skill_agent.agent.AssistantAgent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final AssistantAgent assistantAgent;

    public AgentController(AssistantAgent assistantAgent) {
        this.assistantAgent = assistantAgent;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return assistantAgent.chat(request.sessionId(), request.message());
    }

    public record ChatRequest(String sessionId, String message) {}
}
