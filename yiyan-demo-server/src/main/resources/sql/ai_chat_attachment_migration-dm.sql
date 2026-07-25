-- 个人 AI 对话附件持久化（既有达梦 DM 数据库迁移）
ALTER TABLE "oa_ai_chat_message" ADD "attachment_name" VARCHAR(512);
ALTER TABLE "oa_ai_chat_message" ADD "attachment_path" VARCHAR(1024);
ALTER TABLE "oa_ai_chat_message" ADD "attachment_size" BIGINT;
ALTER TABLE "oa_ai_chat_message" ADD "attachment_content_type" VARCHAR(256);
