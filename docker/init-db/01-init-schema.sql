-- 创建健身数据库
CREATE DATABASE IF NOT EXISTS fitness_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fitness_db;

-- 1. 用户基本信息表
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    age INT COMMENT '年龄',
    gender ENUM('MALE', 'FEMALE', 'OTHER') COMMENT '性别',
    height DECIMAL(5,2) COMMENT '身高(cm)',
    weight DECIMAL(5,2) COMMENT '体重(kg)',
    fitness_goal VARCHAR(100) COMMENT '健身目标',
    fitness_level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') DEFAULT 'BEGINNER' COMMENT '健身水平',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基本信息表';

-- 2. 用户伤病信息表
CREATE TABLE IF NOT EXISTS user_injury (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '伤病记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    injury_type VARCHAR(100) NOT NULL COMMENT '伤病类型',
    injury_location VARCHAR(100) NOT NULL COMMENT '伤病部位',
    severity ENUM('MILD', 'MODERATE', 'SEVERE') DEFAULT 'MILD' COMMENT '严重程度',
    description TEXT COMMENT '详细描述',
    recovery_status ENUM('RECOVERING', 'RECOVERED', 'CHRONIC') DEFAULT 'RECOVERING' COMMENT '恢复状态',
    injury_date DATE COMMENT '受伤日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_recovery_status (recovery_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户伤病信息表';

-- 3. 用户训练记录表
CREATE TABLE IF NOT EXISTS training_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '训练记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    training_date DATE NOT NULL COMMENT '训练日期',
    training_type VARCHAR(50) NOT NULL COMMENT '训练类型',
    exercise_name VARCHAR(100) NOT NULL COMMENT '运动名称',
    sets INT COMMENT '组数',
    reps INT COMMENT '次数',
    weight DECIMAL(6,2) COMMENT '重量(kg)',
    duration INT COMMENT '持续时间(分钟)',
    calories_burned INT COMMENT '消耗卡路里',
    completion_status ENUM('COMPLETED', 'PARTIAL', 'SKIPPED') DEFAULT 'COMPLETED' COMMENT '完成状态',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_training_date (training_date),
    INDEX idx_completion_status (completion_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户训练记录表';
