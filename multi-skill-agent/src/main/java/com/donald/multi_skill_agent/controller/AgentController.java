package com.donald.multi_skill_agent.controller;

import com.donald.multi_skill_agent.agent.AssistantAgent;
import com.donald.multi_skill_agent.model.Standup;
import com.donald.multi_skill_agent.repository.StandupRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final AssistantAgent assistantAgent;
    private final StandupRepository standupRepository;

    public AgentController(AssistantAgent assistantAgent, StandupRepository standupRepository) {
        this.assistantAgent = assistantAgent;
        this.standupRepository = standupRepository;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return assistantAgent.chat(request.sessionId(), request.message());
    }

    @GetMapping("/standup/history")
    public List<Standup> getStandupHistory(@RequestParam String sessionId) {
        return standupRepository.findTop5BySessionIdOrderByCreatedAtDesc(sessionId);
    }

    public record ChatRequest(String sessionId, String message) {}
}
