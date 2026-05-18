package com.zsr.fitaiagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Tag(name = "用户记录管理", description = "用户伤病和训练记录管理接口")
@Slf4j
public class UserRecordController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ========== 伤病记录 ==========

    @GetMapping("/injuries")
    @Operation(summary = "获取用户伤病列表")
    public Map<String, Object> getInjuries(@RequestParam Long userId) {
        List<Map<String, Object>> injuries = jdbcTemplate.queryForList(
                "SELECT * FROM user_injury WHERE user_id = ? ORDER BY created_at DESC", userId
        );
        return Map.of("success", true, "data", injuries);
    }

    @PostMapping("/injuries")
    @Operation(summary = "新增伤病记录")
    public Map<String, Object> addInjury(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String injuryType = (String) request.get("injuryType");
        String injuryLocation = (String) request.get("injuryLocation");
        String severity = (String) request.get("severity");
        String description = (String) request.get("description");
        String recoveryStatus = (String) request.get("recoveryStatus");
        String injuryDate = (String) request.get("injuryDate");

        if (injuryType == null || injuryLocation == null) {
            return Map.of("success", false, "message", "伤病类型和部位不能为空");
        }

        jdbcTemplate.update(
                "INSERT INTO user_injury (user_id, injury_type, injury_location, severity, description, recovery_status, injury_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId, injuryType, injuryLocation,
                severity != null ? severity : "MILD",
                description,
                recoveryStatus != null ? recoveryStatus : "RECOVERING",
                injuryDate
        );
        log.info("新增伤病记录: userId={}, type={}", userId, injuryType);
        return Map.of("success", true, "message", "添加成功");
    }

    @PutMapping("/injuries/{id}")
    @Operation(summary = "更新伤病记录")
    public Map<String, Object> updateInjury(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        jdbcTemplate.update(
                "UPDATE user_injury SET injury_type=?, injury_location=?, severity=?, description=?, recovery_status=?, injury_date=? WHERE id=?",
                request.get("injuryType"), request.get("injuryLocation"),
                request.get("severity"), request.get("description"),
                request.get("recoveryStatus"), request.get("injuryDate"), id
        );
        return Map.of("success", true, "message", "更新成功");
    }

    @DeleteMapping("/injuries/{id}")
    @Operation(summary = "删除伤病记录")
    public Map<String, Object> deleteInjury(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM user_injury WHERE id = ?", id);
        return Map.of("success", true, "message", "删除成功");
    }

    // ========== 训练记录 ==========

    @GetMapping("/training-records")
    @Operation(summary = "获取用户训练记录列表")
    public Map<String, Object> getTrainingRecords(@RequestParam Long userId) {
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT * FROM training_record WHERE user_id = ? ORDER BY training_date DESC, created_at DESC", userId
        );
        return Map.of("success", true, "data", records);
    }

    @PostMapping("/training-records")
    @Operation(summary = "新增训练记录")
    public Map<String, Object> addTrainingRecord(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String trainingDate = (String) request.get("trainingDate");
        String trainingType = (String) request.get("trainingType");
        String exerciseName = (String) request.get("exerciseName");

        if (trainingDate == null || trainingType == null || exerciseName == null) {
            return Map.of("success", false, "message", "训练日期、类型和运动名称不能为空");
        }

        Integer sets = request.get("sets") != null ? Integer.valueOf(request.get("sets").toString()) : null;
        Integer reps = request.get("reps") != null ? Integer.valueOf(request.get("reps").toString()) : null;
        Object weightObj = request.get("weight");
        java.math.BigDecimal weight = weightObj != null ? new java.math.BigDecimal(weightObj.toString()) : null;
        Integer duration = request.get("duration") != null ? Integer.valueOf(request.get("duration").toString()) : null;
        Integer caloriesBurned = request.get("caloriesBurned") != null ? Integer.valueOf(request.get("caloriesBurned").toString()) : null;
        String completionStatus = (String) request.get("completionStatus");
        String notes = (String) request.get("notes");

        jdbcTemplate.update(
                "INSERT INTO training_record (user_id, training_date, training_type, exercise_name, sets, reps, weight, duration, calories_burned, completion_status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId, trainingDate, trainingType, exerciseName, sets, reps, weight, duration, caloriesBurned,
                completionStatus != null ? completionStatus : "COMPLETED", notes
        );
        log.info("新增训练记录: userId={}, exercise={}", userId, exerciseName);
        return Map.of("success", true, "message", "添加成功");
    }

    @PutMapping("/training-records/{id}")
    @Operation(summary = "更新训练记录")
    public Map<String, Object> updateTrainingRecord(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Object weightObj = request.get("weight");
        java.math.BigDecimal weight = weightObj != null ? new java.math.BigDecimal(weightObj.toString()) : null;
        jdbcTemplate.update(
                "UPDATE training_record SET training_date=?, training_type=?, exercise_name=?, sets=?, reps=?, weight=?, duration=?, calories_burned=?, completion_status=?, notes=? WHERE id=?",
                request.get("trainingDate"), request.get("trainingType"), request.get("exerciseName"),
                request.get("sets") != null ? Integer.valueOf(request.get("sets").toString()) : null,
                request.get("reps") != null ? Integer.valueOf(request.get("reps").toString()) : null,
                weight,
                request.get("duration") != null ? Integer.valueOf(request.get("duration").toString()) : null,
                request.get("caloriesBurned") != null ? Integer.valueOf(request.get("caloriesBurned").toString()) : null,
                request.get("completionStatus"), request.get("notes"), id
        );
        return Map.of("success", true, "message", "更新成功");
    }

    @DeleteMapping("/training-records/{id}")
    @Operation(summary = "删除训练记录")
    public Map<String, Object> deleteTrainingRecord(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM training_record WHERE id = ?", id);
        return Map.of("success", true, "message", "删除成功");
    }
}