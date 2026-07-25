-- 已部署 oa_ai_user_config 的达梦实例：执行此脚本增加 AI 请求 Token 限额配置
ALTER TABLE "oa_ai_user_config" ADD "max_input_tokens" INT;
ALTER TABLE "oa_ai_user_config" ADD "context_window_tokens" INT;
