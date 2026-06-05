# Multi-Skill Agent

A conversational AI agent built with Spring Boot and Spring AI that manages personal expenses and tasks through natural language. The agent streams responses in real time and maintains conversation memory across turns.

---

## Problem Statement

Most AI chat assistants are stateless and general-purpose — they cannot take meaningful actions on your behalf or remember context between turns. The goal of this project was to build a **personal assistant agent** that:

- Understands natural language requests like _"Add a $45 food expense for today"_ or _"Mark task 3 as done"_
- Executes those requests by calling real functions against a real database
- Asks for clarification instead of guessing when information is missing
- Plans before acting on multi-step requests
- Maintains conversation memory so follow-up questions make sense
- Keeps different users' data completely isolated from each other

---

## Architecture

### High-Level Flow

```
POST /api/v1/agent/chat
{ "sessionId": "abc-123", "message": "Add a $30 food expense for today" }
        │
        ▼
  AgentController  (thin HTTP adapter)
        │
        ▼
  AssistantAgent   (Spring AI ChatClient)
        │
        ├── System prompt (all skill instructions merged)
        ├── Conversation memory (last 20 messages, scoped to sessionId)
        └── Tool registry (all @Tool methods from all skills)
        │
        ▼
  Anthropic Claude (claude-haiku-4-5)
        │
        ▼  (decides which tool to call)
  ExpenseTool / TaskTool / AskUserTool / TodoWriteTool
        │
        ▼
  PostgreSQL (expenses) / ConcurrentHashMap (tasks, plans)
        │
        ▼
  Flux<String> → Server-Sent Events → client
```

---

## Architectural Decisions

### 1. Skill Abstraction

Every capability is modelled as a `Skill` — an interface with three methods:

```java
public interface Skill {
    String getName();
    String getInstructions();           // contributed to the system prompt
    default ToolCallback[] getTools();  // callable functions exposed to the LLM
}
```

This separates **what the AI is told** (instructions) from **what the AI can do** (tools). Adding a new capability means writing a new class that implements `Skill` — no changes to core agent logic are required.

Skills are automatically discovered by Spring's dependency injection and composed in `ChatClientConfig`:
- All instructions are merged into a single system prompt
- All tools are flattened into a single tool registry

### 2. Session Isolation via `sessionId`

Every request carries a `sessionId` string. This single value drives two isolation mechanisms:

| Mechanism | How `sessionId` is used |
|---|---|
| Conversation memory | `ChatMemory.CONVERSATION_ID` — each session has its own 20-message window |
| Task and plan storage | Map key in `ConcurrentHashMap` — tasks and plans are namespaced per session |
| System prompt | `{sessionId}` template variable — the LLM is told the current session ID so it can pass it to tools correctly |

### 3. Instruction-Only Skills

Not all skills need tools. `SummarySkill` contributes only instructions to the system prompt — it teaches the AI how to format and present summaries using data already available from the other skills. This keeps formatting logic out of code and makes it easy to adjust without redeployment.

### 4. Streaming Responses with WebFlux

The endpoint returns `Flux<String>` over `text/event-stream` (Server-Sent Events). This means the client receives tokens as they are generated rather than waiting for the full response. Spring WebFlux with Reactor Netty handles the non-blocking I/O, making this efficient under concurrent load.

### 5. Heterogeneous Storage

Different data has different persistence requirements:

| Data | Storage | Reasoning |
|---|---|---|
| Expenses | PostgreSQL via JPA | Needs to survive restarts; financial data must be durable |
| Tasks | In-memory `ConcurrentHashMap` | Scoped to a session; no persistence needed |
| Execution plans | In-memory `ConcurrentHashMap` | Transient planning state; discarded after completion |
| Conversation history | In-memory `MessageWindowChatMemory` | Short-lived context; 20-message sliding window is sufficient |

### 6. AskUser Guardrail

The `AskUserTool` + `AskUserSkill` combination provides a named mechanism for the agent to pause and request missing information rather than hallucinating values. The tool description explicitly instructs the LLM: _"Do NOT guess or assume missing values — ask instead."_ Having both a prompt-level instruction and a callable tool makes this behaviour significantly more reliable than either alone.

### 7. Agent Planning with TodoWriteTool

For multi-step requests, the `TodoWriteTool` forces the agent to write an explicit execution plan before acting. This addresses a common LLM failure mode where the model loses track of steps mid-execution. The plan is stored per session and can be re-read at any point during execution.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.6 |
| AI Framework | Spring AI 2.0.0-M6 |
| LLM | Anthropic Claude (claude-haiku-4-5) |
| Web Layer | Spring WebFlux (Reactor Netty) |
| Persistence | Spring Data JPA + Hibernate 7 |
| Database | PostgreSQL 17 |
| Connection Pool | HikariCP |
| Build Tool | Maven |

---

## Project Structure

```
src/main/java/com/donald/multi_skill_agent/
├── agent/
│   └── AssistantAgent.java          # ChatClient wrapper; entry point for all chat requests
├── config/
│   └── ChatClientConfig.java        # Wires skills, tools, and memory into the ChatClient
├── controller/
│   └── AgentController.java         # POST /api/v1/agent/chat — thin HTTP adapter
├── model/
│   ├── Expense.java                 # JPA entity
│   └── Category.java                # Enum: FOOD, TRANSPORT, ENTERTAINMENT, etc.
├── repository/
│   └── ExpenseRepository.java       # Spring Data JPA with custom JPQL queries
├── skills/
│   ├── Skill.java                   # Interface: getName, getInstructions, getTools
│   ├── ExpenseSkill.java            # Expense management instructions + ExpenseTool
│   ├── TaskSkill.java               # Task management instructions + TaskTool
│   ├── SummarySkill.java            # Summarization instructions only (no tools)
│   └── AskUserSkill.java            # Clarification instructions + AskUserTool
└── tools/
    ├── ExpenseTool.java             # 8 @Tool methods backed by PostgreSQL
    ├── TaskTool.java                # 5 @Tool methods backed by in-memory ConcurrentHashMap
    ├── AskUserTool.java             # 1 @Tool method — asks the user a clarifying question
    └── TodoWriteTool.java           # 3 @Tool methods — agent-side execution planning
```

---

## Prerequisites

- Java 25
- PostgreSQL running on `localhost:5432`
- An Anthropic API key with credits

---

## Setup

**1. Create the database**

```sql
CREATE USER assistant_user WITH PASSWORD 'assistantuserpassword';
CREATE DATABASE assistant_db OWNER assistant_user;
```

**2. Set the environment variable**

```powershell
# Windows PowerShell
$env:ANTHROPIC_API_KEY = "sk-ant-..."
```

Or set it permanently via System Properties → Environment Variables, then restart IntelliJ.

**3. Run the application**

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Hibernate will create the `expenses` table automatically on first run (`ddl-auto=update`).

---

## API

### Chat

```
POST /api/v1/agent/chat
Content-Type: application/json
Accept: text/event-stream
```

```json
{
  "sessionId": "your-session-id",
  "message": "Add a $45 food expense for lunch today"
}
```

Use the same `sessionId` across requests to maintain conversation memory and task continuity. The response streams back as Server-Sent Events.

### Example Requests

```json
{ "sessionId": "s1", "message": "Add a $45 food expense for lunch on 2026-05-22" }
{ "sessionId": "s1", "message": "Show me all my transport expenses" }
{ "sessionId": "s1", "message": "How much have I spent on food in total?" }
{ "sessionId": "s1", "message": "Add a task: review monthly budget" }
{ "sessionId": "s1", "message": "Mark task 1 as done" }
{ "sessionId": "s1", "message": "Give me a summary of my expenses and tasks" }
```

---

## Skills Reference

| Skill | Tools | Purpose |
|---|---|---|
| `expense-skill` | 8 | Add, filter, aggregate and delete expenses in PostgreSQL |
| `task-skill` | 5 | Create and manage tasks scoped to the current session |
| `summary-skill` | 0 | Instructs the agent on how to format cross-skill summaries |
| `ask-user-skill` | 1 | Pauses execution to ask the user for missing information |
| `todo-write-tool` | 3 | Agent writes and follows an execution plan for multi-step tasks |
