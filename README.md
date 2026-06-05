# Dev Daily Agent

  A conversational AI agent built with Spring Boot and Spring AI that helps developers manage tasks, track expenses, and generate
  daily standup reports through natural language. The agent streams responses in real time, maintains conversation memory across
  turns, and serves a browser-based chat UI.

  ---

  ## Problem Statement

  Developers waste time every morning writing standup updates — opening Jira, Slack, and Git history just to summarize what they did.
   On top of that, task and expense tracking is scattered across tools. This agent solves that in one place:

  - Understands natural language like _"Add task: fix null pointer bug"_ or _"Generate my standup for today"_
  - Executes requests by calling real functions against a real PostgreSQL database
  - Automatically generates a formatted standup (Yesterday / Today / Blockers) from your actual task history
  - Asks for clarification instead of guessing when information is missing
  - Plans before acting on multi-step requests
  - Maintains conversation memory so follow-up questions make sense
  - Keeps different users' data completely isolated via session ID

  ---

  ## Demo

  Open `http://localhost:8080` after starting the app.

  **Try this flow:**
  1. `Add task: implement OAuth login`
  2. `Update task 1 to IN_PROGRESS`
  3. `Add task: fix null pointer in payment service`
  4. `Update task 2 to DONE`
  5. `Add expense: $200 client lunch`
  6. `Generate my standup for today`

  Claude reads your task history, formats the standup, saves it to the database, and the history panel updates automatically.

  ---

  ## Architecture

  ### High-Level Flow

  Browser (http://localhost:8080)
          │  POST /api/v1/agent/chat  (SSE stream)
          │  GET  /api/v1/agent/standup/history
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
          ▼  (decides which tools to call)
    ExpenseTool / TaskTool / StandupTool / AskUserTool / TodoWriteTool
          │
          ▼
    PostgreSQL (expenses, standup_reports) / ConcurrentHashMap (tasks, plans)
          │
          ▼
    Flux → Server-Sent Events → Browser UI

  ---

  ## Architectural Decisions

  ### 1. Skill Abstraction

  Every capability is modelled as a `Skill` — an interface with three methods:

  java
  public interface Skill {
      String getName();
      String getInstructions();           // contributed to the system prompt
      default ToolCallback[] getTools();  // callable functions exposed to the LLM
  }

  This separates what the AI is told (instructions) from what the AI can do (tools). Adding a new capability means writing one new
  class that implements Skill — no changes to core agent logic required.

  Skills are automatically discovered by Spring's dependency injection and composed in ChatClientConfig:
  - All instructions are merged into a single system prompt
  - All tools are flattened into a single tool registry

  2. Session Isolation via sessionId

  Every request carries a sessionId string. This single value drives all isolation:

  ┌──────────────────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │      Mechanism       │                                         How sessionId is used                                         │
  ├──────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Conversation memory  │ ChatMemory.CONVERSATION_ID — each session has its own 20-message window                               │
  ├──────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Task and plan        │ Map key in ConcurrentHashMap — tasks and plans are namespaced per session                             │
  │ storage              │                                                                                                       │
  ├──────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Standup history      │ Column filter in PostgreSQL — standups are scoped to the session                                      │
  ├──────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ System prompt        │ {sessionId} template variable — the LLM is told the current session ID so it passes it to tools       │
  │                      │ correctly                                                                                             │
  └──────────────────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────┘

  3. Standup Generation as a Skill

  The standup generator is implemented entirely as a StandupSkill. Its instructions tell Claude to:
  1. Call listTasks() to read the current session's task state
  2. Format output as Yesterday / Today / Blockers
  3. Call saveStandup() to persist the result

  No standup-specific logic lives in the agent core — it's a pure skill addition.

  4. Instruction-Only Skills

  Not all skills need tools. SummarySkill contributes only instructions to the system prompt — it teaches the AI how to format and
  present summaries using data already available from other skills. This keeps formatting logic out of code.

  5. Streaming Responses with WebFlux

  The endpoint returns Flux<String> over text/event-stream (Server-Sent Events). The browser receives tokens as they are generated
  rather than waiting for the full response. Spring WebFlux with Reactor Netty handles the non-blocking I/O.

  6. Heterogeneous Storage

  ┌──────────────────────┬───────────────────────────────────┬────────────────────────────────────────────┐
  │         Data         │              Storage              │                 Reasoning                  │
  ├──────────────────────┼───────────────────────────────────┼────────────────────────────────────────────┤
  │ Expenses             │ PostgreSQL via JPA                │ Financial data must be durable             │
  ├──────────────────────┼───────────────────────────────────┼────────────────────────────────────────────┤
  │ Standup reports      │ PostgreSQL via JPA                │ Persistent record across sessions          │
  ├──────────────────────┼───────────────────────────────────┼────────────────────────────────────────────┤
  │ Tasks                │ In-memory ConcurrentHashMap       │ Scoped to a session; no persistence needed │
  ├──────────────────────┼───────────────────────────────────┼────────────────────────────────────────────┤
  │ Execution plans      │ In-memory ConcurrentHashMap       │ Transient planning state                   │
  ├──────────────────────┼───────────────────────────────────┼────────────────────────────────────────────┤
  │ Conversation history │ In-memory MessageWindowChatMemory │ 20-message sliding window                  │
  └──────────────────────┴───────────────────────────────────┴────────────────────────────────────────────┘

  7. AskUser Guardrail

  The AskUserTool + AskUserSkill combination provides a named mechanism for the agent to pause and request missing information rather
   than hallucinating values. Having both a prompt-level instruction and a callable tool makes this behaviour significantly more
  reliable than either alone.

  8. Agent Planning with TodoWriteTool

  For multi-step requests, the TodoWriteTool forces the agent to write an explicit execution plan before acting. This addresses a
  common LLM failure mode where the model loses track of steps mid-execution.

  ---
  Tech Stack

  ┌─────────────────┬──────────────────────────────────────┐
  │      Layer      │              Technology              │
  ├─────────────────┼──────────────────────────────────────┤
  │ Language        │ Java 25                              │
  ├─────────────────┼──────────────────────────────────────┤
  │ Framework       │ Spring Boot 4.0.6                    │
  ├─────────────────┼──────────────────────────────────────┤
  │ AI Framework    │ Spring AI 2.0.0-M6                   │
  ├─────────────────┼──────────────────────────────────────┤
  │ LLM             │ Anthropic Claude (claude-haiku-4-5)  │
  ├─────────────────┼──────────────────────────────────────┤
  │ Web Layer       │ Spring WebFlux (Reactor Netty)       │
  ├─────────────────┼──────────────────────────────────────┤
  │ Persistence     │ Spring Data JPA + Hibernate 7        │
  ├─────────────────┼──────────────────────────────────────┤
  │ Database        │ PostgreSQL 17                        │
  ├─────────────────┼──────────────────────────────────────┤
  │ Connection Pool │ HikariCP                             │
  ├─────────────────┼──────────────────────────────────────┤
  │ Build Tool      │ Maven                                │
  ├─────────────────┼──────────────────────────────────────┤
  │ Frontend        │ Plain HTML / CSS / JS (no framework) │
  └─────────────────┴──────────────────────────────────────┘

  ---
  Project Structure

  src/main/java/com/donald/multi_skill_agent/
  ├── agent/
  │   └── AssistantAgent.java          # ChatClient wrapper; entry point for all chat requests
  ├── config/
  │   └── ChatClientConfig.java        # Wires skills, tools, and memory into the ChatClient
  ├── controller/
  │   └── AgentController.java         # POST /api/v1/agent/chat + GET /standup/history
  ├── model/
  │   ├── Expense.java                 # JPA entity: expenses table
  │   ├── Category.java                # Enum: FOOD, TRANSPORT, ENTERTAINMENT, etc.
  │   └── Standup.java                 # JPA entity: standup_reports table
  ├── repository/
  │   ├── ExpenseRepository.java       # Spring Data JPA with custom JPQL queries
  │   └── StandupRepository.java       # findTop5BySessionIdOrderByCreatedAtDesc
  ├── skills/
  │   ├── Skill.java                   # Interface: getName, getInstructions, getTools
  │   ├── ExpenseSkill.java            # Expense management instructions + ExpenseTool
  │   ├── TaskSkill.java               # Task management instructions + TaskTool
  │   ├── StandupSkill.java            # Standup generation instructions + StandupTool
  │   ├── SummarySkill.java            # Summarization instructions only (no tools)
  │   └── AskUserSkill.java            # Clarification instructions + AskUserTool
  └── tools/
      ├── ExpenseTool.java             # 8 @Tool methods backed by PostgreSQL
      ├── TaskTool.java                # 5 @Tool methods backed by in-memory ConcurrentHashMap
      ├── StandupTool.java             # 2 @Tool methods: saveStandup, getStandupHistory
      ├── AskUserTool.java             # 1 @Tool method — asks the user a clarifying question
      └── TodoWriteTool.java           # 3 @Tool methods — agent-side execution planning

  src/main/resources/
  ├── static/
  │   └── index.html                   # Browser chat UI with SSE streaming + standup history panel
  └── application.properties

  ---
  Prerequisites

  - Java 25
  - Docker (for PostgreSQL via docker-compose)
  - An Anthropic API key

  ---
  Setup

  1. Start PostgreSQL

  docker-compose up -d

  2. Set the API key

  # Windows PowerShell
  $env:ANTHROPIC_API_KEY = "sk-ant-..."

  # Mac/Linux
  export ANTHROPIC_API_KEY=sk-ant-...

  3. Run the application

  ./mvnw spring-boot:run

  Open http://localhost:8080. Hibernate auto-creates all tables on first run.

  ---
  API

  Chat

  POST /api/v1/agent/chat
  Content-Type: application/json
  Accept: text/event-stream

  { "sessionId": "your-session-id", "message": "Generate my standup for today" }

  Standup History

  GET /api/v1/agent/standup/history?sessionId=your-session-id

  Returns the last 5 standup reports for the session as JSON.

  Example Requests

  { "sessionId": "s1", "message": "Add task: implement OAuth login" }
  { "sessionId": "s1", "message": "Update task 1 to IN_PROGRESS" }
  { "sessionId": "s1", "message": "Add task: fix null pointer in payment service" }
  { "sessionId": "s1", "message": "Update task 2 to DONE" }
  { "sessionId": "s1", "message": "Add expense: $200 client lunch" }
  { "sessionId": "s1", "message": "Generate my standup for today" }
  { "sessionId": "s1", "message": "Show me all my expenses this month" }

  ---
  Skills Reference

  ┌────────────────┬───────┬─────────────────────────────────────────────────────────────────┐
  │     Skill      │ Tools │                             Purpose                             │
  ├────────────────┼───────┼─────────────────────────────────────────────────────────────────┤
  │ expense-skill  │ 8     │ Add, filter, aggregate and delete expenses in PostgreSQL        │
  ├────────────────┼───────┼─────────────────────────────────────────────────────────────────┤
  │ task-skill     │ 5     │ Create and manage tasks scoped to the current session           │
  ├────────────────┼───────┼─────────────────────────────────────────────────────────────────┤
  │ standup-skill  │ 2     │ Generate and persist daily standup reports from task history    │
  ├────────────────┼───────┼─────────────────────────────────────────────────────────────────┤
  │ summary-skill  │ 0     │ Instructs the agent on how to format cross-skill summaries      │
  ├────────────────┼───────┼─────────────────────────────────────────────────────────────────┤
  │ ask-user-skill │ 1     │ Pauses execution to ask the user for missing information        │
  ├────────────────┼───────┼─────────────────────────────────────────────────────────────────┤
  │ planning-skill │ 3     │ Agent writes and follows an execution plan for multi-step tasks │
  ├────────────────┼───────┼─────────────────────────────────────────────────────────────────┤
  │ ```            │       │                                                                 │
  └────────────────┴───────┴─────────────────────────────────────────────────────────────────┘
