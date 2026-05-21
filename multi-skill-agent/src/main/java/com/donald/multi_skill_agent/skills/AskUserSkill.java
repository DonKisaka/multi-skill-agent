package com.donald.multi_skill_agent.skills;

import com.donald.multi_skill_agent.tools.AskUserTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class AskUserSkill implements Skill {
    private final AskUserTool askUserTool;

    public AskUserSkill(AskUserTool askUserTool) {
        this.askUserTool = askUserTool;
    }

    @Override
    public String getName() { return "ask-user-skill"; }

    @Override
    public String getInstructions() {
        return """
                  ## Clarification
                  When a user request is ambiguous or missing details, always ask for clarification
                  before taking any action. Never guess amounts, dates, categories, or task descriptions.
                  Ask one question at a time — do not ask multiple questions at once.
                  """;
    }

    @Override
    public ToolCallback[] getTools() {
        return ToolCallbacks.from(askUserTool);
    }
}
