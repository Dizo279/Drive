-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: mydrive_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `files`
--

DROP TABLE IF EXISTS `files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size_bytes` bigint NOT NULL,
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACTIVE','DELETED') COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `files_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `files`
--

LOCK TABLES `files` WRITE;
/*!40000 ALTER TABLE `files` DISABLE KEYS */;
INSERT INTO `files` VALUES (1,2,'index.html','2/a20a6ddd-dfbd-489c-9157-6ec5065b56f6.html',22145,'text/html','ACTIVE','2026-03-21 07:15:35','2026-03-21 13:14:05'),(2,1,'test.txt','2/7b56b938-8db2-4411-9f13-54790b6eeac9.txt',14,'text/plain','ACTIVE','2026-03-21 07:34:00','2026-03-21 13:14:05'),(3,2,'abcxyz.txt','2/81dcb23c-a038-4d48-ad7d-11e3db243c76.txt',864,'text/plain','ACTIVE','2026-03-21 11:48:45','2026-03-21 13:14:05'),(12,2,'nguyen_truong_duy_0005468_mht1.ipynb','2/c949b13b-2a42-40a1-a381-b23b3a6a30f0.ipynb',799609,'application/octet-stream','ACTIVE','2026-03-22 09:46:10','2026-03-22 09:46:10'),(13,1,'wallpaperflare.com_wallpaper.jpg','1/23d1b15f-16b3-4281-b59b-1942512e184b.jpg',81978,'image/jpeg','ACTIVE','2026-03-22 09:47:35','2026-03-22 09:47:35'),(15,2,'wallpaperflare.com_wallpaper.jpg','2/808051bf-83c0-4d6f-8d73-f3846b2d46fe.jpg',81978,'image/jpeg','ACTIVE','2026-03-22 14:04:53','2026-03-22 14:04:53');
/*!40000 ALTER TABLE `files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shared_files`
--

DROP TABLE IF EXISTS `shared_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shared_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_id` bigint NOT NULL,
  `owner_id` bigint NOT NULL,
  `shared_with_id` bigint DEFAULT NULL,
  `permission` enum('VIEW','DOWNLOAD') COLLATE utf8mb4_unicode_ci DEFAULT 'VIEW',
  `public_token` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `public_token` (`public_token`),
  KEY `file_id` (`file_id`),
  KEY `owner_id` (`owner_id`),
  KEY `shared_with_id` (`shared_with_id`),
  CONSTRAINT `shared_files_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `files` (`id`) ON DELETE CASCADE,
  CONSTRAINT `shared_files_ibfk_2` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `shared_files_ibfk_3` FOREIGN KEY (`shared_with_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shared_files`
--

LOCK TABLES `shared_files` WRITE;
/*!40000 ALTER TABLE `shared_files` DISABLE KEYS */;
INSERT INTO `shared_files` VALUES (5,3,2,NULL,'VIEW','275c8ea4de5d4aaebb77c8d5b13338fa','2026-03-28 13:15:37','2026-03-21 13:15:37'),(6,3,2,NULL,'VIEW','70123db6ca0749b08ead514cfa67fefc','2026-03-28 13:29:27','2026-03-21 13:29:27'),(7,1,2,NULL,'VIEW','76ff90b2bb0146cb84af8c7ab34fcd73','2026-03-28 13:43:08','2026-03-21 13:43:08'),(8,2,1,2,'DOWNLOAD',NULL,NULL,'2026-03-22 13:49:29'),(9,2,1,NULL,'DOWNLOAD','1a11a872f74e471aa14b4ee9021f59b2','2026-03-29 14:42:19','2026-03-22 14:42:19'),(10,13,1,NULL,'DOWNLOAD','d94753ab23444abfb32ff5bf6a2ee821','2026-03-30 04:03:30','2026-03-23 04:03:30'),(11,3,2,1,'DOWNLOAD',NULL,NULL,'2026-03-23 05:00:58'),(12,1,2,1,'DOWNLOAD',NULL,NULL,'2026-03-23 05:01:25');
/*!40000 ALTER TABLE `shared_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_quota`
--

DROP TABLE IF EXISTS `user_quota`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_quota` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `quota_bytes` bigint DEFAULT '5368709120',
  `used_bytes` bigint DEFAULT '0',
  `plan` enum('FREE','PRO','ADMIN') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `user_quota_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_quota`
--

LOCK TABLES `user_quota` WRITE;
/*!40000 ALTER TABLE `user_quota` DISABLE KEYS */;
INSERT INTO `user_quota` VALUES (1,1,107374182400,81992,'ADMIN'),(2,2,10737418240,904596,'PRO'),(3,3,10737418240,0,'PRO'),(4,4,5368709120,0,'FREE');
/*!40000 ALTER TABLE `user_quota` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('USER','ADMIN') COLLATE utf8mb4_unicode_ci DEFAULT 'USER',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','admin@mydrive.com','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi','ADMIN','2026-03-21 05:53:35'),(2,'alice','alice@gmail.com','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi','USER','2026-03-21 05:53:35'),(3,'bob','bob@gmail.com','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi','USER','2026-03-21 05:53:35'),(4,'charlie','charlie@gmail.com','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi','USER','2026-03-21 05:53:35');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-07 22:52:31
