package com.donald.multi_skill_agent.repository;

import com.donald.multi_skill_agent.model.Standup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandupRepository extends JpaRepository<Standup, Long> {
    List<Standup> findTop5BySessionIdOrderByCreatedAtDesc(String sessionId);
}
