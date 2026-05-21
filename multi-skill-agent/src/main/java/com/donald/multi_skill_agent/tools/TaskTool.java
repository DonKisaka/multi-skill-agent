package com.donald.multi_skill_agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TaskTool {

    public record Task(long id, String description, String status) {}

    // Outer key: sessionId — inner key: taskId
    private final Map<String, Map<Long, Task>> sessionTasks = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong>    sessionCounters = new ConcurrentHashMap<>();

    private Map<Long, Task> tasksFor(String sessionId) {
        return sessionTasks.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
    }

    private AtomicLong counterFor(String sessionId) {
        return sessionCounters.computeIfAbsent(sessionId, k -> new AtomicLong(1));
    }

    @Tool(description = "Add a new task. Returns the created task with its assigned ID.")
    public Task addTask(
            @ToolParam(description = "Current session ID") String sessionId,
            @ToolParam(description = "Task description")  String description) {
        long id   = counterFor(sessionId).getAndIncrement();
        Task task = new Task(id, description, "PENDING");
        tasksFor(sessionId).put(id, task);
        return task;
    }

    @Tool(description = "List all tasks for this session, sorted by ID.")
    public List<Task> listTasks(
            @ToolParam(description = "Current session ID") String sessionId) {
        return tasksFor(sessionId).values().stream()
                .sorted(Comparator.comparingLong(Task::id))
                .toList();
    }

    @Tool(description = "Update a task's status by its ID.")
    public String updateTaskStatus(
            @ToolParam(description = "Current session ID") String sessionId,
            @ToolParam(description = "Task ID") long id,
            @ToolParam(description = "New status: PENDING, IN_PROGRESS, DONE") String status) {
        Task existing = tasksFor(sessionId).get(id);
        if (existing == null) return "Task " + id + " not found in this session.";
        tasksFor(sessionId).put(id, new Task(id, existing.description(), status));
        return "Task " + id + " updated to " + status + ".";
    }

    @Tool(description = "Remove a task permanently by its ID.")
    public String removeTask(
            @ToolParam(description = "Current session ID") String sessionId,
            @ToolParam(description = "Task ID to remove") long id) {
        Task removed = tasksFor(sessionId).remove(id);
        return removed != null ? "Task " + id + " removed." : "Task " + id + " not found.";
    }

    @Tool(description = "Clear all tasks for this session.")
    public String clearTasks(
            @ToolParam(description = "Current session ID") String sessionId) {
        int count = tasksFor(sessionId).size();
        sessionTasks.remove(sessionId);
        sessionCounters.remove(sessionId);
        return count + " tasks cleared.";
    }
}