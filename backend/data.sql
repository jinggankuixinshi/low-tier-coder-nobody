-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: evaluation_db
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.2

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

USE evaluation_db;

--
-- Dumping data for table `evaluation_indicator`
--

LOCK TABLES `evaluation_indicator` WRITE;
/*!40000 ALTER TABLE `evaluation_indicator` DISABLE KEYS */;
INSERT INTO `evaluation_indicator` (`id`, `template_id`, `name`, `description`, `weight`, `max_score`, `sort_order`, `eval_type`, `ai_weight`, `manual_weight`, `dimension`, `create_time`) VALUES (1,1,'文件完整性检查','检查提交文件是否包含任务要求的全部内容（代码、文档、截图等）',0.0000,0.00,1,0,0.60,0.40,'precheck','2026-06-14 21:47:11'),(2,1,'文档齐全性检查','检查需求文档、设计文档、使用说明等材料是否齐全',0.0000,0.00,2,0,0.60,0.40,'precheck','2026-06-14 21:47:11'),(3,1,'功能完成度','核心功能是否按要求完整实现，边界条件处理是否到位',0.3000,100.00,3,2,0.60,0.40,'completion','2026-06-14 21:47:11'),(4,1,'核心功能实现','关键业务逻辑是否正确实现，主流程是否运行正常',0.2500,100.00,4,2,0.60,0.40,'completion','2026-06-14 21:47:11'),(5,1,'需求匹配度','实际实现与需求文档的对应程度，是否遗漏需求点',0.1500,100.00,5,1,0.60,0.40,'completion','2026-06-14 21:47:11'),(6,1,'代码质量与规范','代码可读性、命名规范、注释完整性、编码风格统一性',0.1000,100.00,6,0,0.60,0.40,'tech','2026-06-14 21:47:11'),(7,1,'架构设计合理性','系统架构分层是否清晰，设计模式使用是否得当，模块耦合度',0.1000,100.00,7,0,0.60,0.40,'tech','2026-06-14 21:47:11'),(8,1,'技术选型与应用','所选用技术栈的合理性、版本兼容性及熟练运用程度',0.0500,100.00,8,1,0.60,0.40,'tech','2026-06-14 21:47:11'),(9,1,'创新点与技术亮点','是否具有自主创新设计、性能优化、用户体验提升等亮点',0.0300,100.00,9,1,0.60,0.40,'innovation','2026-06-14 21:47:11'),(10,1,'文档完整性与规范','项目文档撰写完整性、格式规范性、内容可读性',0.0200,100.00,10,0,0.60,0.40,'document','2026-06-14 21:47:11');
/*!40000 ALTER TABLE `evaluation_indicator` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `evaluation_template`
--

LOCK TABLES `evaluation_template` WRITE;
/*!40000 ALTER TABLE `evaluation_template` DISABLE KEYS */;
INSERT INTO `evaluation_template` (`id`, `name`, `description`, `task_id`, `teacher_id`, `eval_method`, `ai_weight`, `manual_weight`, `ai_model`, `weight_completion`, `weight_tech`, `weight_innovation`, `weight_document`, `deleted`, `create_time`, `update_time`) VALUES (1,'通用实训评价模板','适用于各类实训项目的通用评价标准，涵盖完成度、技术质量、创新性和文档规范四个维度',NULL,1,2,0.60,0.40,NULL,0.3000,0.3000,0.2000,0.2000,0,'2026-06-14 21:47:11','2026-06-14 21:47:11');
/*!40000 ALTER TABLE `evaluation_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `email`, `phone`, `student_no`, `deleted`, `create_time`, `update_time`) VALUES (1,'teacher01','$2a$10$cic9WQRS6GOsh8IZmMZL/eE8iKO0TdaJLHrPHlmY6K6uvxy7PL2YG','教师01',1,NULL,NULL,'T2024001',0,'2026-06-14 21:47:11','2026-06-14 21:47:11'),(2,'student01','$2a$10$Gz1DdHg6Bwwy2d0fRRUI4utGEoZ5KYocmfjzN/Rlj4jtvEJ77creG','学生01',0,NULL,NULL,'S2024001',0,'2026-06-14 21:47:11','2026-06-14 21:47:11');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `training_task`
--

LOCK TABLES `training_task` WRITE;
/*!40000 ALTER TABLE `training_task` DISABLE KEYS */;
INSERT INTO `training_task` (`id`, `title`, `subject`, `description`, `expected_output`, `course_name`, `module_name`, `brief`, `business_scenario`, `impl_conditions`, `requirement_docs`, `teacher_id`, `status`, `deadline`, `weight_completion`, `weight_tech`, `weight_innovation`, `weight_document`, `deleted`, `create_time`, `update_time`) VALUES (1,'在线书城系统','Java Web开发实训','设计并实现一个完整的在线书城系统，包含用户注册登录、图书浏览与搜索、购物车、订单管理、后台管理等功能模块。','1. 完整的项目源码（Spring Boot + MyBatis-Plus）\n2. 数据库ER图与SQL脚本\n3. 项目部署文档和使用说明\n4. 功能演示截图或录屏','Java Web开发实训','综合项目实战','本项目要求开发一个功能完备的在线书城系统，涵盖前台用户端和后台管理端，是Java Web开发的综合性实战项目。','某在线教育平台希望构建内部书城，方便师生浏览、选购教材和参考书，需要前后台分离的企业级解决方案。','1. 后端使用Spring Boot 3.x + MyBatis-Plus + MySQL\n2. 前端可使用Vue或模板引擎\n3. 需实现RESTful API接口',NULL,1,1,'2026-07-14 21:47:11',0.35,0.30,0.20,0.15,0,'2026-06-14 21:47:11','2026-06-14 21:47:11'),(2,'电商数据分析系统','Python数据分析实训','采集某电商平台商品数据，进行数据清洗、多维度分析和可视化展示，输出分析报告。','1. Python数据采集脚本\n2. 数据清洗与处理代码（Pandas）\n3. ECharts/Matplotlib可视化图表\n4. 分析报告文档','Python数据分析实训','数据采集与分析','本项目要求完成从数据采集到分析报告的全流程Python数据分析实践，培养数据思维和分析能力。','运营团队需要了解某品类商品的市场价格分布、销量趋势和用户评价特征，以辅助定价和选品决策。','1. 使用Python Requests/Scrapy采集数据\n2. Pandas进行数据清洗和统计分析\n3. ECharts或Matplotlib进行可视化\n4. 输出Markdown/PDF分析报告',NULL,1,1,'2026-07-14 21:47:11',0.35,0.30,0.20,0.15,0,'2026-06-14 21:47:11','2026-06-14 21:47:11'),(3,'学生成绩管理系统数据库设计','数据库原理与应用','完成学生成绩管理系统的数据库设计，包括需求分析、概念结构设计（ER图）、逻辑结构设计和物理实施（SQL脚本）。','1. 需求分析文档\n2. ER图（实体-联系图）\n3. 关系模式及规范化说明\n4. 完整的SQL DDL/DML脚本\n5. 索引与查询优化说明','数据库原理与应用','数据库设计实践','本项目考察数据库设计的完整流程，从需求分析到物理实施，涵盖范式理论、ER建模、SQL编程等核心知识点。','某高校教务处需要一套学生成绩管理系统，需管理学生、课程、教师、选课、成绩等信息，支持成绩录入、查询、统计功能。','1. 至少设计5张核心表\n2. 满足第三范式\n3. 包含主键、外键、索引设计\n4. 编写常用查询的SQL语句\n5. 考虑并发安全（事务/锁）',NULL,1,1,'2026-07-14 21:47:11',0.35,0.30,0.20,0.15,0,'2026-06-14 21:47:11','2026-06-14 21:47:11'),(4,'个人博客前端开发','前端开发实训','使用Vue 3 + Element Plus开发一个响应式个人博客网站，包含首页、文章列表、文章详情、分类标签、关于我等页面。','1. 完整的Vue 3项目源码\n2. 响应式布局适配（PC + 移动端）\n3. 组件化设计说明\n4. API接口对接文档','前端开发实训','Vue框架实战','本项目要求使用现代前端框架开发个人博客，注重组件化开发、状态管理和工程化实践。','开发者需要一个展示技术文章和个人项目的平台，要求界面美观、加载快速、移动端友好。','1. 使用Vue 3 Composition API\n2. Element Plus作为UI组件库\n3. Vue Router实现前端路由\n4. Pinia进行状态管理\n5. Axios对接RESTful API',NULL,1,1,'2026-07-14 21:47:11',0.35,0.30,0.20,0.15,0,'2026-06-14 21:47:11','2026-06-14 21:47:11');
/*!40000 ALTER TABLE `training_task` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-14 21:49:11
