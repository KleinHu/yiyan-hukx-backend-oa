-- 已部署 oa_ai_user_config 的达梦实例：先执行本文件，再执行 ai_user_model_init-dm.sql
ALTER TABLE "oa_ai_user_config" ADD "model_cache_updated_at" DATETIME;
