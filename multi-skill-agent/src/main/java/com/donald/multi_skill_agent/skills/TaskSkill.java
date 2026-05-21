package com.donald.multi_skill_agent.skills;

import com.donald.multi_skill_agent.tools.TaskTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class TaskSkill implements Skill{

    private final TaskTool taskTool;

    public TaskSkill(TaskTool taskTool) {
        this.taskTool = taskTool;
    }


    @Override
    public String getName() { return "task-skill"; }

    @Override
    public String getInstructions() {
        return """
                 ## Task Management
                You can add, list, update, and remove tasks using their numeric IDs.
                Each task has an ID, description, and a status: PENDING, IN_PROGRESS, or DONE.
                
                IMPORTANT: Every task tool requires the current session ID as its first argument.
                The session ID is provided to you at the start of each conversation — always pass it exactly.
                
                Rules:
                - When adding a task, confirm it was saved and show its assigned ID.
                - When listing tasks, group them by status: PENDING → IN_PROGRESS → DONE.
                - To complete a task, call updateTaskStatus(DONE) — never remove it unless explicitly asked.
                - Tasks are isolated per session and lost when the session ends
                 """;
    }

    @Override
    public ToolCallback[] getTools() {
        return ToolCallbacks.from(taskTool);
    }
}
