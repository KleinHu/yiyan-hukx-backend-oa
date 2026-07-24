-- 外部系统跳转配置 - 初始化脚本 (达梦数据库 DM 版本)

CREATE TABLE "oa_external_system" (
  "id" BIGINT NOT NULL,
  "name" VARCHAR(100) NOT NULL,
  "url" VARCHAR(255) NOT NULL,
  "icon" VARCHAR(255),
  "sort_order" INT DEFAULT 0,
  "creator" VARCHAR(64),
  "create_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64),
  "update_time" DATETIME DEFAULT CURRENT_TIMESTAMP,
  "update_ip" VARCHAR(255),
  "is_deleted" TINYINT DEFAULT 0,
  PRIMARY KEY ("id")
);
