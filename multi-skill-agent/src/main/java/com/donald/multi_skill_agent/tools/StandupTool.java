package com.donald.multi_skill_agent.tools;

import com.donald.multi_skill_agent.model.Standup;
import com.donald.multi_skill_agent.repository.StandupRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StandupTool {

    private final StandupRepository standupRepository;

    public StandupTool(StandupRepository standupRepository) {
        this.standupRepository = standupRepository;
    }

    @Tool(description = "Save a generated standup report to the database for the current session.")
    public String saveStandup(
            @ToolParam(description = "Current session ID") String sessionId,
            @ToolParam(description = "The formatted standup content to save") String content) {
        Standup standup = new Standup();
        standup.setSessionId(sessionId);
        standup.setContent(content);
        standupRepository.save(standup);
        return "Standup saved successfully.";
    }

    @Tool(description = "Retrieve the last 5 standup reports for the current session.")
    public String getStandupHistory(
            @ToolParam(description = "Current session ID") String sessionId) {
        List<Standup> history = standupRepository.findTop5BySessionIdOrderByCreatedAtDesc(sessionId);
        if (history.isEmpty()) {
            return "No standup history found for this session.";
        }
        return history.stream()
                .map(s -> "--- " + s.getCreatedAt() + " ---\n" + s.getContent())
                .collect(Collectors.joining("\n\n"));
    }
}
