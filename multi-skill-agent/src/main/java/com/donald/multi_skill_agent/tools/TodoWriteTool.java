package com.donald.multi_skill_agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TodoWriteTool {
    private final Map<String, List<String>> sessionPlans = new ConcurrentHashMap<>();

    @Tool(description = """
              Write a step-by-step plan before executing a multi-step task.
              Always call this FIRST when the user asks for something that requires
              multiple actions (e.g. add expenses + summarize, create tasks + report).
              The plan guides your execution — follow it step by step.
              """)
    public String writePlan(
            @ToolParam(description = "Current session ID") String sessionId,
            @ToolParam(description = "Ordered list of steps to execute") List<String> steps) {
        sessionPlans.put(sessionId, steps);
        StringBuilder plan = new StringBuilder("Plan created:\n");
        for (int i = 0; i < steps.size(); i++) {
            plan.append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }
        return plan.toString();
    }

    @Tool(description = "Get the current execution plan for this session.")
    public String getPlan(
            @ToolParam(description = "Current session ID") String sessionId) {
        List<String> steps = sessionPlans.get(sessionId);
        if (steps == null || steps.isEmpty()) return "No plan found for this session.";
        StringBuilder plan = new StringBuilder("Current plan:\n");
        for (int i = 0; i < steps.size(); i++) {
            plan.append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }
        return plan.toString();
    }

    @Tool(description = "Clear the plan for this session once all steps are done.")
    public String clearPlan(
            @ToolParam(description = "Current session ID") String sessionId) {
        sessionPlans.remove(sessionId);
        return "Plan cleared.";
    }

}
