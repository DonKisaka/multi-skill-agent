package com.donald.multi_skill_agent.skills;

import org.springframework.ai.tool.ToolCallback;

public interface Skill {
    String getName();
    String getInstructions();

    default ToolCallback[] getTools() {
        return new ToolCallback[0];  // default: instruction-only skill
    }
}
