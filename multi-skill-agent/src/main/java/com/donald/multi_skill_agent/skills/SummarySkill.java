package com.donald.multi_skill_agent.skills;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class SummarySkill implements Skill {
    @Override
    public String getName() { return "summary-skill"; }

    @Override
    public String getInstructions() {
        return """
                 ## Summarization
                When the user asks for a summary or report, synthesize data from the other skills:
                
                Expense summaries:
                - Group by category, show each total, then a grand total
                - Highlight the top spending category
                - Use a table format: | Category | Amount |
                
                Task summaries:
                - Show counts: X pending, Y in progress, Z done
                - List any PENDING or IN_PROGRESS tasks by name
                - Flag overdue intent if the user has mentioned deadlines
                
                General:
                - Always end a summary with one actionable insight or suggestion
                - Keep summaries under 200 words unless the user asks for detail
                 """;
    }

}
