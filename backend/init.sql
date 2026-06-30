-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: evaluation_db
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.2

CREATE DATABASE IF NOT EXISTS evaluation_db DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

USE evaluation_db;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `evaluation_indicator`
--

DROP TABLE IF EXISTS `evaluation_indicator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_indicator` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `name` varchar(200) NOT NULL,
  `description` text,
  `weight` decimal(5,2) DEFAULT '0.00',
  `max_score` decimal(5,2) DEFAULT '100.00',
  `sort_order` int DEFAULT '0',
  `eval_type` tinyint NOT NULL DEFAULT '2',
  `ai_weight` decimal(5,2) DEFAULT '0.60',
  `manual_weight` decimal(5,2) DEFAULT '0.40',
  `dimension` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation_result`
--

DROP TABLE IF EXISTS `evaluation_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `template_id` bigint NOT NULL,
  `indicator_id` bigint NOT NULL,
  `eval_type` tinyint NOT NULL DEFAULT '2',
  `auto_score` decimal(5,2) DEFAULT NULL,
  `auto_comment` text,
  `manual_score` decimal(5,2) DEFAULT NULL,
  `manual_comment` text,
  `dimension` varchar(50) DEFAULT NULL,
  `adjust_score` decimal(5,2) DEFAULT NULL,
  `adjust_reason` varchar(500) DEFAULT NULL,
  `final_score` decimal(5,2) DEFAULT NULL,
  `improvement_suggestion` text,
  `issue_severity` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation_template`
--

DROP TABLE IF EXISTS `evaluation_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` text,
  `task_id` bigint DEFAULT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `eval_method` tinyint NOT NULL DEFAULT '2',
  `ai_weight` decimal(5,2) DEFAULT '0.60',
  `manual_weight` decimal(5,2) DEFAULT '0.40',
  `ai_model` varchar(100) DEFAULT NULL,
  `weight_completion` decimal(5,4) DEFAULT '0.2500',
  `weight_tech` decimal(5,4) DEFAULT '0.2500',
  `weight_innovation` decimal(5,4) DEFAULT '0.2500',
  `weight_document` decimal(5,4) DEFAULT '0.2500',
  `deleted` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `favorite_task`
--

DROP TABLE IF EXISTS `favorite_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_task` (`user_id`,`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_record`
--

DROP TABLE IF EXISTS `report_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint DEFAULT NULL,
  `task_id` bigint DEFAULT NULL,
  `report_type` int DEFAULT '0',
  `title` varchar(500) DEFAULT NULL,
  `pdf_path` varchar(1000) DEFAULT NULL,
  `batch_group_id` varchar(100) DEFAULT NULL,
  `generation_status` int DEFAULT '0',
  `generation_error` text,
  `precheck_pass_count` int DEFAULT NULL,
  `precheck_passed` int DEFAULT NULL,
  `completion_score` decimal(5,2) DEFAULT NULL,
  `tech_quality_score` decimal(5,2) DEFAULT NULL,
  `innovation_score` decimal(5,2) DEFAULT NULL,
  `document_score` decimal(5,2) DEFAULT NULL,
  `weighted_total` decimal(5,2) DEFAULT NULL,
  `grade` varchar(20) DEFAULT NULL,
  `teacher_comment` text,
  `teaching_reflection` text,
  `reviewer_id` bigint DEFAULT NULL,
  `review_date` date DEFAULT NULL,
  `submission_count` int DEFAULT NULL,
  `deleted` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submission`
--

DROP TABLE IF EXISTS `submission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint DEFAULT NULL,
  `student_id` bigint DEFAULT NULL,
  `file_paths` text,
  `file_names` text,
  `file_sizes` text,
  `teacher_comment` text,
  `evaluation_status` tinyint NOT NULL DEFAULT '0',
  `submitted` tinyint NOT NULL DEFAULT '0',
  `approval_status` tinyint DEFAULT NULL,
  `draft_template_id` bigint DEFAULT NULL,
  `ai_score_status` tinyint DEFAULT '0',
  `total_score` decimal(5,2) DEFAULT NULL,
  `submit_time` datetime DEFAULT NULL,
  `deleted` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `real_name` varchar(50) DEFAULT NULL,
  `role` int DEFAULT '0',
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `student_no` varchar(50) DEFAULT NULL,
  `deleted` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `training_task`
--

DROP TABLE IF EXISTS `training_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `training_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `subject` varchar(100) DEFAULT NULL,
  `description` text,
  `expected_output` text,
  `course_name` varchar(100) DEFAULT NULL,
  `module_name` varchar(100) DEFAULT NULL,
  `brief` text,
  `business_scenario` text,
  `impl_conditions` text,
  `requirement_docs` varchar(1000) DEFAULT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `status` int DEFAULT '0',
  `deadline` datetime DEFAULT NULL,
  `weight_completion` decimal(5,2) DEFAULT '0.30',
  `weight_tech` decimal(5,2) DEFAULT '0.30',
  `weight_innovation` decimal(5,2) DEFAULT '0.20',
  `weight_document` decimal(5,2) DEFAULT '0.20',
  `deleted` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `verification_result`
--

DROP TABLE IF EXISTS `verification_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `verification_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint DEFAULT NULL,
  `task_id` bigint DEFAULT NULL,
  `check_item` varchar(200) DEFAULT NULL,
  `check_type` int DEFAULT '0',
  `expected_content` text,
  `actual_content` text,
  `status` int DEFAULT '0',
  `detail` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ============================================================
-- 创建专属数据库用户
-- 密码为占位符，部署时替换为实际密码
-- 使用 DROP + CREATE 替代 IF NOT EXISTS，
-- 避免用户已存在时密码不会更新的问题
-- ============================================================
DROP USER IF EXISTS 'evaluation'@'localhost';
DROP USER IF EXISTS 'evaluation'@'127.0.0.1';
CREATE USER 'evaluation'@'localhost' IDENTIFIED BY 'evaluation_pwd_123456';
CREATE USER 'evaluation'@'127.0.0.1' IDENTIFIED BY 'evaluation_pwd_123456';
GRANT ALL PRIVILEGES ON evaluation_db.* TO 'evaluation'@'localhost';
GRANT ALL PRIVILEGES ON evaluation_db.* TO 'evaluation'@'127.0.0.1';
FLUSH PRIVILEGES;

-- Dump completed on 2026-06-14 21:49:11
