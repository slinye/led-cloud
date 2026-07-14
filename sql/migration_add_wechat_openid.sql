-- =============================================
-- 微信小程序支持 - 添加 wechat_openid 字段
-- =============================================
USE led_management;

ALTER TABLE t_user ADD COLUMN wechat_openid VARCHAR(100) NULL UNIQUE AFTER status;
