SET NAMES utf8mb4;

DELETE FROM system_config;
INSERT INTO system_config (config_key, config_value, config_desc) VALUES
('system_name', '摩托车维修预约系统', '系统名称'),
('shop_display_name', '张师傅维修店', '用户端店铺显示名称'),
('default_avatar', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', '默认头像URL'),
('default_business_hours', '08:00-20:00', '默认营业时间'),
('default_service_duration', '60', '默认服务时长(分钟)'),
('review_tags', '服务态度好,技术专业,速度快,价格合理,沟通顺畅,环境整洁,耐心细致,准时交付', '评价标签'),
('rate_texts', '很差,较差,一般,满意,非常满意', '评分文案'),
('rest_rules', '①每人每周休2天 ②不能连续休息 ③每项维修技能至少2人在岗 ④不考虑司机技能（人人都是司机）⑤优先安排休息少的技师', '排休规则说明');

DELETE FROM dict_data;
INSERT INTO dict_data (dict_type, dict_label, dict_value, dict_sort, css_class, status) VALUES
('appointment_status', '待确认', '0', 1, 'info', 1),
('appointment_status', '已确认', '1', 2, 'warning', 1),
('appointment_status', '维修中', '2', 3, 'primary', 1),
('appointment_status', '已完成', '3', 4, 'success', 1),
('appointment_status', '已取消', '4', 5, 'danger', 1),
('appointment_status', '爽约', '5', 6, 'warning', 1),
('pickup_status', '待接车', '0', 1, 'info', 1),
('pickup_status', '已接车', '1', 2, 'primary', 1),
('pickup_status', '车辆到店', '2', 3, 'success', 1),
('pickup_status', '未接到', '3', 4, 'danger', 1),
('user_role', '管理员', '1', 1, 'danger', 1),
('user_role', '维修店', '2', 2, 'warning', 1),
('user_role', '用户', '3', 3, 'info', 1),
('user_role', '技师', '4', 4, 'primary', 1),
('user_status', '正常', '1', 1, 'success', 1),
('user_status', '禁用', '0', 2, 'danger', 1),
('audit_status', '待审核', '0', 1, 'warning', 1),
('audit_status', '已通过', '1', 2, 'success', 1),
('audit_status', '已拒绝', '2', 3, 'danger', 1),
('message_type', '系统', '0', 1, 'info', 1),
('message_type', '预约', '1', 2, 'warning', 1),
('message_type', '聊天', '3', 3, 'primary', 1),
('message_type', '表扬', '4', 4, 'success', 1),
('pay_status', '未支付', '0', 1, 'info', 1),
('pay_status', '已支付', '1', 2, 'success', 1);

DELETE FROM employee WHERE id IN (1, 2);
DELETE FROM service_template WHERE id >= 361;
