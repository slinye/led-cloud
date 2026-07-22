-- =============================================
-- 设备ID迁移脚本：将旧版 SCREEN-xxx 更新为 2开头8位数字
-- 执行前请先备份 led_management 数据库
-- =============================================
USE led_management;

-- 方式一：按屏幕 ID 精确映射（推荐，避免重复）
UPDATE t_screen SET mqtt_client_id = '20000001' WHERE id = 1 AND mqtt_client_id LIKE 'SCREEN-%';
UPDATE t_screen SET mqtt_client_id = '20000002' WHERE id = 2 AND mqtt_client_id LIKE 'SCREEN-%';
UPDATE t_screen SET mqtt_client_id = '20000003' WHERE id = 3 AND mqtt_client_id LIKE 'SCREEN-%';
UPDATE t_screen SET mqtt_client_id = '20000004' WHERE id = 4 AND mqtt_client_id LIKE 'SCREEN-%';
UPDATE t_screen SET mqtt_client_id = '20000005' WHERE id = 5 AND mqtt_client_id LIKE 'SCREEN-%';
UPDATE t_screen SET mqtt_client_id = '20000006' WHERE id = 6 AND mqtt_client_id LIKE 'SCREEN-%';

-- 方式二：批量转换所有 SCREEN-xxx 格式的记录
-- 适用场景：原 ID 为 SCREEN-001 ~ SCREEN-999，且没有重复
-- UPDATE t_screen
-- SET mqtt_client_id = CONCAT('20000', LPAD(SUBSTRING(mqtt_client_id, 8), 3, '0'))
-- WHERE mqtt_client_id LIKE 'SCREEN-%';

-- 验证结果
SELECT id, name, mqtt_client_id FROM t_screen ORDER BY id;
