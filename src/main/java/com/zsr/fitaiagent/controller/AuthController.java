package com.zsr.fitaiagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "用户认证", description = "用户认证相关接口")
@Slf4j
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Map.of("success", false, "message", "用户名和密码不能为空");
        }

        var existing = jdbcTemplate.queryForList(
                "SELECT id FROM user_profile WHERE username = ?", username.trim()
        );
        if (!existing.isEmpty()) {
            return Map.of("success", false, "message", "用户名已存在");
        }

        String passwordHash = passwordEncoder.encode(password);
        Integer age = request.get("age") != null ? Integer.parseInt(request.get("age")) : null;
        String gender = request.get("gender");
        String height = request.get("height");
        String weight = request.get("weight");
        String fitnessGoal = request.get("fitnessGoal");
        String fitnessLevel = request.get("fitnessLevel");

        jdbcTemplate.update(
                "INSERT INTO user_profile (username, password_hash, role, age, gender, height, weight, fitness_goal, fitness_level) VALUES (?, ?, 'USER', ?, ?, ?, ?, ?, ?)",
                username.trim(), passwordHash, age, gender,
                height != null ? new java.math.BigDecimal(height) : null,
                weight != null ? new java.math.BigDecimal(weight) : null,
                fitnessGoal, fitnessLevel != null ? fitnessLevel : "BEGINNER"
        );

        log.info("新用户注册成功: {}", username.trim());
        return Map.of("success", true, "message", "注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码登录")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return Map.of("success", false, "message", "用户名和密码不能为空");
        }

        var users = jdbcTemplate.queryForList(
                "SELECT id, username, password_hash, role, fitness_goal, fitness_level FROM user_profile WHERE username = ?",
                username
        );

        if (users.isEmpty()) {
            return Map.of("success", false, "message", "用户名或密码错误");
        }

        var user = users.get(0);
        String storedHash = (String) user.get("password_hash");

        if (storedHash == null || !passwordEncoder.matches(password, storedHash)) {
            return Map.of("success", false, "message", "用户名或密码错误");
        }

        log.info("用户登录成功: {}", username);
        return Map.of(
                "success", true,
                "user", Map.of(
                        "id", user.get("id"),
                        "username", user.get("username"),
                        "role", user.get("role") != null ? user.get("role") : "USER",
                        "fitnessGoal", user.get("fitness_goal") != null ? user.get("fitness_goal") : "",
                        "fitnessLevel", user.get("fitness_level") != null ? user.get("fitness_level") : ""
                )
        );
    }
}
