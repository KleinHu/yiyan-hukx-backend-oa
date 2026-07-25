-- 个人 AI 网关配置（达梦 DM 版本）
CREATE TABLE "oa_ai_user_config" (
  "id" BIGINT NOT NULL,
  "user_id" BIGINT NOT NULL,
  "user_code" VARCHAR(64),
  "base_url" VARCHAR(500) NOT NULL,
  "api_key_encrypted" VARCHAR(2048) NOT NULL,
  "default_model" VARCHAR(256),
  "model_cache_updated_at" DATETIME,
  "max_input_tokens" INT,
  "context_window_tokens" INT,
  "creator" VARCHAR(64),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64),
  "update_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "update_ip" VARCHAR(255),
  "is_deleted" TINYINT DEFAULT 0 NOT NULL,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ai_user_config_user_deleted"
  ON "oa_ai_user_config" ("user_id", "is_deleted");

COMMENT ON TABLE "oa_ai_user_config" IS '个人 AI 网关配置';
COMMENT ON COLUMN "oa_ai_user_config"."api_key_encrypted" IS '供应商密钥（内网明文保存，字段名为历史兼容保留）';
