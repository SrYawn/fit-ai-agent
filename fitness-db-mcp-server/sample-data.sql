-- 示例数据插入脚本

USE fitness_db;

-- 插入示例用户
INSERT INTO user_profile (username, age, gender, height, weight, fitness_goal, fitness_level) VALUES
('zhangsan', 28, 'MALE', 175.00, 70.50, '增肌', 'INTERMEDIATE'),
('lisi', 25, 'FEMALE', 165.00, 55.00, '减脂', 'BEGINNER'),
('wangwu', 32, 'MALE', 180.00, 85.00, '提高耐力', 'ADVANCED');

-- 插入示例伤病信息
INSERT INTO user_injury (user_id, injury_type, injury_location, severity, description, recovery_status, injury_date) VALUES
(1, '肌肉拉伤', '右肩', 'MILD', '训练时右肩三角肌轻微拉伤', 'RECOVERING', '2026-02-15'),
(2, '膝盖疼痛', '左膝', 'MODERATE', '跑步后左膝关节疼痛', 'RECOVERING', '2026-02-20'),
(3, '腰部劳损', '下背部', 'MILD', '硬拉时腰部不适', 'RECOVERED', '2026-01-10');

-- 插入示例训练记录
INSERT INTO training_record (user_id, training_date, training_type, exercise_name, sets, reps, weight, duration, calories_burned, completion_status, notes) VALUES
(1, '2026-02-28', '力量训练', '卧推', 4, 10, 60.00, 45, 250, 'COMPLETED', '感觉良好,重量适中'),
(1, '2026-02-27', '力量训练', '深蹲', 4, 8, 80.00, 50, 300, 'COMPLETED', '最后一组有点吃力'),
(2, '2026-02-28', '有氧运动', '跑步', NULL, NULL, NULL, 30, 200, 'COMPLETED', '配速稳定'),
(2, '2026-02-26', '力量训练', '哑铃推举', 3, 12, 8.00, 40, 180, 'COMPLETED', ''),
(3, '2026-02-28', '力量训练', '硬拉', 5, 5, 120.00, 60, 400, 'COMPLETED', '新PR!'),
(3, '2026-02-27', '有氧运动', '游泳', NULL, NULL, NULL, 45, 350, 'COMPLETED', '自由泳为主');
