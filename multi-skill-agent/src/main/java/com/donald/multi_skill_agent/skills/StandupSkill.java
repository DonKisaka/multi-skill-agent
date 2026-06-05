package com.donald.multi_skill_agent.skills;

import com.donald.multi_skill_agent.tools.StandupTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class StandupSkill implements Skill {

    private final StandupTool standupTool;

    public StandupSkill(StandupTool standupTool) {
        this.standupTool = standupTool;
    }

    @Override
    public String getName() {
        return "standup-skill";
    }

    @Override
    public String getInstructions() {
        return """
                ## Standup Generation
                When the user asks to "generate standup", "create standup", "write my standup", \
                "standup update", or similar phrases:
                1. Call listTasks() to retrieve all tasks for the current session.
                2. Format the standup report exactly as:
                   **Yesterday:** [tasks with status DONE]
                   **Today:** [tasks with status IN_PROGRESS or PENDING]
                   **Blockers:** [any explicitly mentioned blockers, otherwise "None"]
                3. After formatting, call saveStandup(sessionId, content) with the exact session ID \
                and the full formatted standup text.
                4. Present the formatted standup to the user.

                Always pass the current session ID when calling saveStandup.
                """;
    }

    @Override
    public ToolCallback[] getTools() {
        return ToolCallbacks.from(standupTool);
    }
}
