package com.donald.multi_skill_agent.skills;

import com.donald.multi_skill_agent.tools.TodoWriteTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class PlanningSkill implements Skill {
    private final TodoWriteTool todoWriteTool;

    public PlanningSkill(TodoWriteTool todoWriteTool) {
        this.todoWriteTool = todoWriteTool;
    }

    @Override
    public String getName() { return "planning-skill"; }

    @Override
    public String getInstructions() {
        return """
                  ## Planning
                  For any request requiring more than one action, always write a plan first
                  using the writePlan tool before executing any steps.
                  Follow the plan in order — do not skip steps.
                  Once all steps are complete, clear the plan.
                  """;
    }

    @Override
    public ToolCallback[] getTools() {
        return ToolCallbacks.from(todoWriteTool);
    }
}
