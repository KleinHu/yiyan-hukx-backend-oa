-- 外部系统跳转配置 - 初始化脚本 (MySQL 版)
DROP TABLE IF EXISTS `oa_external_system`;

CREATE TABLE `oa_external_system` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '系统名称',
  `url` varchar(255) NOT NULL COMMENT '跳转URL',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标标识',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序号',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_ip` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `is_deleted` tinyint(2) DEFAULT '0' COMMENT '是否删除 (0-未删除，1-已删除)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部系统跳转配置表';
