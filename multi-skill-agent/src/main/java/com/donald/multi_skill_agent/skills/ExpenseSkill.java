package com.donald.multi_skill_agent.skills;

import com.donald.multi_skill_agent.tools.ExpenseTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class ExpenseSkill implements Skill{

   private final ExpenseTool expenseTool;

   public ExpenseSkill(ExpenseTool expenseTool) {
       this.expenseTool = expenseTool;
   }

    @Override
    public String getName() { return "expense-skill"; }

    @Override
    public String getInstructions() {
        return """
                  You are an expense management assistant.
                  You can add, view, filter, and delete expenses.
                  When adding an expense, always confirm the details with the user before saving.
                  When amounts or dates are unclear, ask the user to clarify.
                  Always respond with a summary after performing any action.
                  """;
    }


    @Override
    public ToolCallback[] getTools() {
        return ToolCallbacks.from(expenseTool);
    }
}
