package com.donald.multi_skill_agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class AskUserTool {
    @Tool(description = """
              Ask the user a clarifying question before proceeding.
              Use this when the user's request is ambiguous, incomplete, or missing required details
              such as amount, category, date, or task description.
              Do NOT guess or assume missing values — ask instead.
              """)
    public String askUser(
            @ToolParam(description = "The clarifying question to ask the user") String question) {
        return "QUESTION: " + question;
    }
}
