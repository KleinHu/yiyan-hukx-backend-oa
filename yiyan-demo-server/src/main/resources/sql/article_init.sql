-- 易研 OA - 门户文章管理全量初始化脚本 (MySQL 版)

DROP TABLE IF EXISTS `article`;
DROP TABLE IF EXISTS `article_column`;
DROP TABLE IF EXISTS `article_comment`;
DROP TABLE IF EXISTS `article_like_record`;
DROP TABLE IF EXISTS `article_favorite`;
DROP TABLE IF EXISTS `article_view_record`;

-- 1. 专栏表
CREATE TABLE `article_column` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父专栏ID',
  `name` varchar(50) NOT NULL COMMENT '专栏名称',
  `description` varchar(255) DEFAULT NULL COMMENT '专栏简介',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '专栏封面图',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序号',
  `status` tinyint(2) DEFAULT '1' COMMENT '状态 (1-启用 0-停用)',
  `is_home_show` tinyint(2) DEFAULT '0' COMMENT '是否首页展示(1-展示 0-不展示)',
  `home_sort` int(11) DEFAULT '0' COMMENT '首页排序号',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_ip` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `is_deleted` tinyint(2) DEFAULT '0' COMMENT '是否删除 (0-未删除，1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章专栏表';

-- 2. 文章内容主表
CREATE TABLE `article` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `column_id` bigint(20) NOT NULL COMMENT '所属专栏ID',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图',
  `content` longtext COMMENT '文章正文',
  `author_name` varchar(50) DEFAULT NULL COMMENT '作者名称',
  `author_code` varchar(50) DEFAULT NULL COMMENT '作者工号',
  `publish_time" datetime DEFAULT NULL COMMENT '发布时间',
  `status` tinyint(2) DEFAULT '0' COMMENT '状态 (0-草稿，1-已发布，2-已下线)',
  `audit_opinion` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `view_count` int(11) DEFAULT '0' COMMENT '浏览量',
  `is_top` tinyint(2) DEFAULT '0' COMMENT '是否置顶 (1-置顶，0-普通)',
  `top_expire_time` datetime DEFAULT NULL COMMENT '置顶截止时间',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_ip` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `is_deleted` tinyint(2) DEFAULT '0' COMMENT '是否删除 (0-未删除，1-已删除)',
  `release_time` datetime DEFAULT NULL COMMENT '计划发布时间',
  `offline_time` datetime DEFAULT NULL COMMENT '计划下线时间',
  `like_count` int(11) DEFAULT '0' COMMENT '点赞数',
  `is_release_timed` tinyint(1) DEFAULT '0' COMMENT '是否定时发布',
  `is_offline_timed` tinyint(1) DEFAULT '0' COMMENT '是否定时下线',
  `attachments` text COMMENT '附件信息',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签',
  `favorite_count` int(11) DEFAULT '0' COMMENT '收藏数',
  `is_must_read` tinyint(1) DEFAULT '0' COMMENT '是否为必读项(1-是, 0-否)',
  PRIMARY KEY (`id`),
  KEY `idx_column_id` (`column_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章内容表';

-- 3. 文章评论表
CREATE TABLE `article_comment` (
  `id` bigint(20) NOT NULL,
  `article_id` bigint(20) NOT NULL,
  `content` text NOT NULL,
  `user_name` varchar(50),
  `user_code` varchar(50),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `delete_status` tinyint(2) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_art_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章评论表';

-- 4. 文章点赞表
CREATE TABLE `article_like_record` (
  `id` bigint(20) NOT NULL,
  `article_id` bigint(20) NOT NULL,
  `user_code` varchar(50) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_art_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞表';

-- 5. 文章收藏表
CREATE TABLE `article_favorite` (
  `id` bigint(20) NOT NULL,
  `article_id` bigint(20) NOT NULL,
  `user_code" varchar(50) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_art_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';

-- 6. 文章浏览记录表
CREATE TABLE `article_view_record` (
  `id` bigint(20) NOT NULL,
  `article_id` bigint(20) NOT NULL,
  `user_id` bigint(20),
  `user_name` varchar(50),
  `user_code" varchar(50),
  `view_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_art_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章浏览记录表';

-- 7. 文章已阅回执表
CREATE TABLE `oa_article_receipt` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `article_id` bigint(20) NOT NULL COMMENT '文章ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户名称',
  `dept_id` varchar(64) DEFAULT NULL COMMENT '部门ID',
  `dept_name` varchar(255) DEFAULT NULL COMMENT '部门名称',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '确认时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_ip` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `is_deleted` tinyint(2) DEFAULT 0 COMMENT '是否逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_art_id` (`article_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章已阅回执表';
