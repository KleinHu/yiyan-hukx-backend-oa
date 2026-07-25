-- 个人 AI 对话历史（达梦 DM 版本）
CREATE TABLE "oa_ai_chat_session" (
  "id" BIGINT NOT NULL,
  "user_id" BIGINT NOT NULL,
  "title" VARCHAR(256) NOT NULL,
  "model" VARCHAR(256) NOT NULL,
  "message_count" INT DEFAULT 0 NOT NULL,
  "last_message_at" DATETIME NOT NULL,
  "creator" VARCHAR(64),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64),
  "update_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "update_ip" VARCHAR(255),
  "is_deleted" TINYINT DEFAULT 0 NOT NULL,
  PRIMARY KEY ("id")
);

CREATE INDEX "idx_ai_chat_session_user_time"
  ON "oa_ai_chat_session" ("user_id", "last_message_at");

CREATE TABLE "oa_ai_chat_message" (
  "id" BIGINT NOT NULL,
  "user_id" BIGINT NOT NULL,
  "session_id" BIGINT NOT NULL,
  "message_order" INT NOT NULL,
  "role" VARCHAR(16) NOT NULL,
  "content" CLOB NOT NULL,
  "attachment_name" VARCHAR(512),
  "attachment_path" VARCHAR(1024),
  "attachment_size" BIGINT,
  "attachment_content_type" VARCHAR(256),
  "creator" VARCHAR(64),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64),
  "update_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "update_ip" VARCHAR(255),
  "is_deleted" TINYINT DEFAULT 0 NOT NULL,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ai_chat_message_session_order"
  ON "oa_ai_chat_message" ("session_id", "message_order");

COMMENT ON TABLE "oa_ai_chat_session" IS '个人 AI 对话会话';
COMMENT ON TABLE "oa_ai_chat_message" IS '个人 AI 对话消息';
