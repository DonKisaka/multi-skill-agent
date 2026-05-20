package com.donald.multi_skill_agent.skills;

import org.springframework.ai.tool.ToolCallback;

public interface Skill {
    String getName();
    String getInstructions();
    ToolCallback[] getTools();
}
