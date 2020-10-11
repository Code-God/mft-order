/*
Navicat MySQL Data Transfer

Source Server         : 101.132.165.187
Source Server Version : 50723
Source Host           : 101.132.165.187:3306
Source Database       : m_mall_user

Target Server Type    : MYSQL
Target Server Version : 50723
File Encoding         : 65001

Date: 2018-09-21 14:49:59
*/

USE `m_mall_order`;

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for mall_order_info
-- ----------------------------
DROP TABLE IF EXISTS `mall_order_info`;
CREATE TABLE `mall_order_info` (
  `order_id` varchar(20) NOT NULL COMMENT '订单号',
  `mall_user_id` varchar(20) DEFAULT NULL COMMENT '用户id',
  `payment_amt` decimal(10,2) DEFAULT '0.00' COMMENT '实付金额',
  `credit` decimal(10,0) DEFAULT '0' COMMENT '积分',
  `origin_amt` decimal(10,2) DEFAULT '0.00' COMMENT '原始金额',
  `post_fee_amt` decimal(10,2) DEFAULT '0.00' COMMENT '邮费',
  `discount_amt` decimal(10,2) DEFAULT '0.00' COMMENT '折扣金额',
  `currency` char(1) DEFAULT '0' COMMENT '币种 0人民币，1积分，2美元',
  `addr_id` varchar(5000) DEFAULT NULL COMMENT '地址id  存明文地址',
  `addr_name` varchar(255) DEFAULT NULL,
  `addr_phone` varchar(19) DEFAULT NULL,
  `order_type` char(1) DEFAULT NULL COMMENT '订单类型  0存入云库存 1云库存提货 2直接发货 3云库存换货',
  `order_review` char(1) DEFAULT '0' COMMENT '是否包含敏感产品订单，0不包含。1包含',
  `belongs_code` char(1) DEFAULT NULL COMMENT '所属商城',
  `express_code` varchar(30) DEFAULT NULL COMMENT '快递号',
  `express_company` varchar(255) DEFAULT NULL COMMENT '快递公司',
  `leader_id` varchar(20) DEFAULT NULL COMMENT '上级代理id',
  `proof_path` varchar(5000) DEFAULT NULL COMMENT '支付凭证图片url',
  `buyer_memo` varchar(255) DEFAULT NULL COMMENT '买家备注',
  `cs_memo` varchar(255) DEFAULT NULL COMMENT '后台备注',
  `system_memo` varchar(500) DEFAULT NULL COMMENT '系统备注',
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `verify_end_date` datetime DEFAULT NULL COMMENT '审核截至时间',
  `pay_end_date` datetime DEFAULT NULL COMMENT '支付截止时间',
  `is_del` char(1) DEFAULT '0' COMMENT '0正常，1删除',
  `order_status` char(2) DEFAULT '0' COMMENT '0 =待付款 1=上级审核中 2= 上级审核未通过 3=待发货 4=待收货 5=已完成 6=交易取消 7=交易关闭 8=商务审核中 9=商务审核未通过 10 =退款中 11=已退款',
  `is_can_cancel` char(1) DEFAULT '0' COMMENT '是否可以取消。0可以，1不可以',
  `eclpsono` varchar(35) DEFAULT '' COMMENT '京东出库单号',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `index_orderId` (`order_id`) USING BTREE,
  KEY `index_mall_user_id` (`mall_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for mall_order_item
-- ----------------------------
DROP TABLE IF EXISTS `mall_order_item`;
CREATE TABLE `mall_order_item` (
  `id` varchar(20) NOT NULL COMMENT 'id',
  `order_id` varchar(20) DEFAULT NULL COMMENT '订单号id',
  `item_id` varchar(20) DEFAULT NULL COMMENT '商品id',
  `sku_code` varchar(20) DEFAULT NULL COMMENT 'sku_code',
  `amount` decimal(10,0) DEFAULT NULL COMMENT '数量',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '单价',
  `credit` decimal(10,2) DEFAULT '0.00' COMMENT '积分',
  `currency` char(1) DEFAULT '0' COMMENT '币种，0人民币，1积分，2美元',
  `unit` varchar(20) DEFAULT NULL COMMENT '单位',
  `spec` varchar(20) DEFAULT NULL COMMENT '规格',
  `status` char(1) DEFAULT NULL COMMENT '0待支付，1待审核，2待发货，3待收货，4已完成， 5交易取消',
  `type` char(1) DEFAULT '0' COMMENT '0为转进。1为转出,  相对于云库存换货来说',
  `cart_id` varchar(20) DEFAULT NULL COMMENT '购物车id',
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_del` char(1) DEFAULT '0' COMMENT '0正常，1删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for mall_order_verify
-- ----------------------------
DROP TABLE IF EXISTS `mall_order_verify`;
CREATE TABLE `mall_order_verify` (
  `id` varchar(20) NOT NULL,
  `order_id` varchar(20) DEFAULT NULL COMMENT '订单id',
  `proposer_id` varchar(20) DEFAULT NULL COMMENT '申请人id',
  `accepter_id` varchar(20) DEFAULT NULL COMMENT '审核人id',
  `admin_id` varchar(20) DEFAULT NULL COMMENT '管理员id',
  `verify_type` char(1) DEFAULT '0' COMMENT '审核单类型，0订单审核，1敏感药审核',
  `verify_status` char(1) DEFAULT '0' COMMENT '审核状态，0待审核，1审核通过，2审核不通过，3审核超时',
  `verify_memo` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_del` char(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='审核表';



-- ----------------------------
-- Table structure for mall_regulate_goods
-- ----------------------------
DROP TABLE IF EXISTS `mall_regulate_goods`;
CREATE TABLE `mall_regulate_goods` (
  `id` varchar(20) NOT NULL,
  `regulate_id` varchar(20) DEFAULT NULL COMMENT '调剂单号',
  `item_id` varchar(20) DEFAULT NULL,
  `sku_code` varchar(20) DEFAULT NULL,
  `amount` decimal(10,0) DEFAULT '0' COMMENT '数量',
  `type` char(1) DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `memo` varchar(255) DEFAULT NULL,
  `is_del` char(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for mall_regulate_info
-- ----------------------------
DROP TABLE IF EXISTS `mall_regulate_info`;
CREATE TABLE `mall_regulate_info` (
  `id` varchar(20) NOT NULL,
  `mall_user_id` varchar(20) DEFAULT NULL,
  `admin_id` varchar(20) DEFAULT NULL,
  `regulate_type` char(1) DEFAULT '0' COMMENT '0云库存增加，1云库存减，2换货，3转货',
  `order_id` varchar(20) DEFAULT NULL,
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` char(1) DEFAULT '0',
  `memo` varchar(255) DEFAULT NULL,
  `is_del` char(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for mall_transfer_goods
-- ----------------------------
DROP TABLE IF EXISTS `mall_transfer_goods`;
CREATE TABLE `mall_transfer_goods` (
  `id` varchar(20) NOT NULL,
  `title` varchar(255) DEFAULT NULL COMMENT '标题',
  `mall_user_id` varchar(20) DEFAULT NULL COMMENT '上级id',
  `next_proxy_id` varchar(20) DEFAULT NULL COMMENT '下级id',
  `item_id` varchar(20) DEFAULT NULL COMMENT '商品id',
  `sku_code` varchar(10) DEFAULT NULL COMMENT 'sku',
  `amount` decimal(10,0) DEFAULT NULL COMMENT '数量',
  `relation_id` varchar(20) DEFAULT NULL COMMENT '关联单据',
  `relation_type` char(1) DEFAULT '0' COMMENT '关联单据类型，0订单。1云库存单，2升级单',
  `create_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `type` char(1) DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `memo` varchar(255) DEFAULT NULL,
  `is_del` char(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

