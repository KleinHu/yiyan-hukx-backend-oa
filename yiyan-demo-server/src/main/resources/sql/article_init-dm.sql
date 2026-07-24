-- 易研 OA - 门户文章管理全量初始化脚本 (达梦数据库 DM 版本)

-- 1. 专栏表
CREATE TABLE "article_column" (
  "id" BIGINT NOT NULL,
  "parent_id" BIGINT DEFAULT 0,
  "name" VARCHAR(50) NOT NULL,
  "description" VARCHAR(255),
  "cover_url" VARCHAR(500),
  "sort_order" INT DEFAULT 0,
  "status" TINYINT DEFAULT 1,
  "is_home_show" TINYINT DEFAULT 0,
  "home_sort" INT DEFAULT 0,
  "creator" VARCHAR(64),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64),
  "update_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "update_ip" VARCHAR(255),
  "is_deleted" TINYINT DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE INDEX "idx_art_col_parent" ON "article_column"("parent_id");

-- 2. 文章内容主表
CREATE TABLE "article" (
  "id" BIGINT NOT NULL,
  "column_id" BIGINT NOT NULL,
  "title" VARCHAR(200) NOT NULL,
  "summary" VARCHAR(500),
  "cover_url" VARCHAR(500),
  "content" TEXT,
  "author_name" VARCHAR(50),
  "author_code" VARCHAR(50),
  "publish_time" DATETIME,
  "status" TINYINT DEFAULT 0,
  "audit_opinion" VARCHAR(500),
  "view_count" INT DEFAULT 0,
  "is_top" TINYINT DEFAULT 0,
  "top_expire_time" DATETIME,
  "creator" VARCHAR(64),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64),
  "update_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "update_ip" VARCHAR(255),
  "is_deleted" TINYINT DEFAULT 0,
  "release_time" DATETIME,
  "offline_time" DATETIME,
  "like_count" INT DEFAULT 0,
  "is_release_timed" TINYINT DEFAULT 0,
  "is_offline_timed" TINYINT DEFAULT 0,
  "attachments" TEXT,
  "tags" VARCHAR(500),
  "favorite_count" INT DEFAULT 0,
  "is_must_read" TINYINT DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE INDEX "idx_art_col_id" ON "article"("column_id");

-- 3. 互动表组
CREATE TABLE "article_comment" (
  "id" BIGINT NOT NULL,
  "article_id" BIGINT NOT NULL,
  "content" TEXT NOT NULL,
  "user_name" VARCHAR(50),
  "user_code" VARCHAR(50),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "delete_status" TINYINT DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE TABLE "article_like_record" (
  "id" BIGINT NOT NULL,
  "article_id" BIGINT NOT NULL,
  "user_code" VARCHAR(50) NOT NULL,
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "article_favorite" (
  "id" BIGINT NOT NULL,
  "article_id" BIGINT NOT NULL,
  "user_code" VARCHAR(50) NOT NULL,
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "article_view_record" (
  "id" BIGINT NOT NULL,
  "article_id" BIGINT NOT NULL,
  "user_id" BIGINT,
  "user_name" VARCHAR(50),
  "user_code" VARCHAR(50),
  "view_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);

-- 7. 文章已阅回执表
CREATE TABLE "oa_article_receipt" (
  "id" BIGINT NOT NULL,
  "article_id" BIGINT NOT NULL,
  "user_id" VARCHAR(64) NOT NULL,
  "user_name" VARCHAR(50),
  "dept_id" VARCHAR(64),
  "dept_name" VARCHAR(255),
  "creator" VARCHAR(64),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64),
  "update_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "update_ip" VARCHAR(255),
  "is_deleted" TINYINT DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE INDEX "idx_art_receipt_aid" ON "oa_article_receipt"("article_id");
CREATE INDEX "idx_art_receipt_uid" ON "oa_article_receipt"("user_id");

-- 添加注释
COMMENT ON TABLE "article" IS '文章内容主表';
COMMENT ON COLUMN "article"."is_must_read" IS '是否为必读项';
COMMENT ON COLUMN "article"."attachments" IS '附件内容';
