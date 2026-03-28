package com.zsr.fitnessdbmcpserver.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FitnessDbTool {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "Query user profile information by username. Returns user's basic information including age, gender, height, weight, fitness goal and fitness level.")
    public String getUserProfile(@ToolParam(description = "Username to query") String username) {
        try {
            String sql = "SELECT id, username, age, gender, height, weight, fitness_goal, fitness_level, created_at, updated_at " +
                    "FROM user_profile WHERE username = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, username);

            if (results.isEmpty()) {
                return "User not found: " + username;
            }

            return formatResults(results);
        } catch (Exception e) {
            log.error("Error querying user profile", e);
            return "Error querying user profile: " + e.getMessage();
        }
    }

    @Tool(description = "Query user profile information by user ID. Returns user's basic information including username, age, gender, height, weight, fitness goal and fitness level.")
    public String getUserProfileById(@ToolParam(description = "User ID to query") Long userId) {
        try {
            String sql = "SELECT id, username, age, gender, height, weight, fitness_goal, fitness_level, created_at, updated_at " +
                    "FROM user_profile WHERE id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);

            if (results.isEmpty()) {
                return "User not found for user ID: " + userId;
            }

            return formatResults(results);
        } catch (Exception e) {
            log.error("Error querying user profile by user ID", e);
            return "Error querying user profile by user ID: " + e.getMessage();
        }
    }

    @Tool(description = "Query user injury information by user ID. Returns all injury records including injury type, location, severity, recovery status and description.")
    public String getUserInjuries(@ToolParam(description = "User ID to query injuries") Long userId) {
        try {
            String sql = "SELECT id, user_id, injury_type, injury_location, severity, description, recovery_status, injury_date, created_at, updated_at " +
                    "FROM user_injury WHERE user_id = ? ORDER BY injury_date DESC";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);

            if (results.isEmpty()) {
                return "No injury records found for user ID: " + userId;
            }

            return formatResults(results);
        } catch (Exception e) {
            log.error("Error querying user injuries", e);
            return "Error querying user injuries: " + e.getMessage();
        }
    }

    @Tool(description = "Query user training records by user ID. Optionally filter by date range. Returns training history including exercise name, sets, reps, weight, duration and calories burned.")
    public String getUserTrainingRecords(
            @ToolParam(description = "User ID to query training records") Long userId,
            @ToolParam(description = "Start date (YYYY-MM-DD format, optional)", required = false) String startDate,
            @ToolParam(description = "End date (YYYY-MM-DD format, optional)", required = false) String endDate) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT id, user_id, training_date, training_type, exercise_name, sets, reps, weight, duration, " +
                            "calories_burned, completion_status, notes, created_at, updated_at " +
                            "FROM training_record WHERE user_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (startDate != null && !startDate.isEmpty()) {
                sql.append(" AND training_date >= ?");
                params.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append(" AND training_date <= ?");
                params.add(endDate);
            }

            sql.append(" ORDER BY training_date DESC");

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            if (results.isEmpty()) {
                return "No training records found for user ID: " + userId;
            }

            return formatResults(results);
        } catch (Exception e) {
            log.error("Error querying training records", e);
            return "Error querying training records: " + e.getMessage();
        }
    }

    private String formatResults(List<Map<String, Object>> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> row = results.get(i);
            sb.append("Record ").append(i + 1).append(":\n");
            row.forEach((key, value) -> sb.append("  ").append(key).append(": ").append(value).append("\n"));
            sb.append("\n");
        }
        return sb.toString();
    }
}
