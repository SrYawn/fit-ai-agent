USE fitness_db;

-- 插入示例用户数据
INSERT INTO user_profile (username, age, gender, height, weight, fitness_goal, fitness_level) VALUES
('zhangsan', 28, 'MALE', 175.00, 70.50, '增肌', 'INTERMEDIATE'),
('lisi', 25, 'FEMALE', 165.00, 55.00, '减脂', 'BEGINNER'),
('wangwu', 32, 'MALE', 180.00, 85.00, '提高体能', 'ADVANCED');

-- 插入示例伤病数据
INSERT INTO user_injury (user_id, injury_type, injury_location, severity, description, recovery_status, injury_date) VALUES
(1, '肌肉拉伤', '右肩', 'MILD', '卧推时右肩轻微拉伤', 'RECOVERING', '2026-02-15'),
(2, '膝盖疼痛', '左膝', 'MODERATE', '跑步后左膝疼痛', 'RECOVERING', '2026-02-20');

-- 插入示例训练记录
INSERT INTO training_record (user_id, training_date, training_type, exercise_name, sets, reps, weight, duration, calories_burned, completion_status, notes) VALUES
(1, '2026-02-28', '力量训练', '卧推', 4, 10, 60.00, 30, 200, 'COMPLETED', '感觉良好'),
(1, '2026-02-28', '力量训练', '深蹲', 4, 12, 80.00, 25, 250, 'COMPLETED', ''),
(2, '2026-02-28', '有氧训练', '跑步', NULL, NULL, NULL, 30, 300, 'COMPLETED', '配速6分钟/公里'),
(2, '2026-02-27', '力量训练', '哑铃推举', 3, 12, 10.00, 20, 150, 'PARTIAL', '第三组未完成'),
(3, '2026-02-28', '力量训练', '硬拉', 5, 5, 120.00, 35, 280, 'COMPLETED', '新PR');
