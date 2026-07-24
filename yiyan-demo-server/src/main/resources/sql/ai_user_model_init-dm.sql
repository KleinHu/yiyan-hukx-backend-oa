-- 个人 AI 网关模型缓存（达梦 DM 版本）
CREATE TABLE "oa_ai_user_model" (
  "id" BIGINT NOT NULL,
  "user_id" BIGINT NOT NULL,
  "model_id" VARCHAR(256) NOT NULL,
  "model_name" VARCHAR(512) NOT NULL,
  "enabled" TINYINT DEFAULT 1 NOT NULL,
  "last_seen_time" DATETIME NOT NULL,
  "creator" VARCHAR(64),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64),
  "update_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "update_ip" VARCHAR(255),
  "is_deleted" TINYINT DEFAULT 0 NOT NULL,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ai_user_model_user_model_deleted"
  ON "oa_ai_user_model" ("user_id", "model_id", "is_deleted");

COMMENT ON TABLE "oa_ai_user_model" IS '个人 AI 网关模型缓存';
COMMENT ON COLUMN "oa_ai_user_model"."enabled" IS '是否仍可供用户选择（0-否，1-是）';
