-- MySQL dump 10.13  Distrib 5.7.20, for Linux (x86_64)
--
-- Host: localhost    Database: dsm
-- ------------------------------------------------------
-- Server version	5.7.20

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `proxycolauth`
--

DROP TABLE IF EXISTS `proxycolauth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proxycolauth` (
  `dbkey` varchar(32) NOT NULL,
  `username` varchar(45) NOT NULL,
  `tablename` varchar(45) NOT NULL,
  `colname` varchar(45) NOT NULL,
  `priv` text,
  PRIMARY KEY (`dbkey`,`username`,`tablename`,`colname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `proxycolauth`
--

LOCK TABLES `proxycolauth` WRITE;
/*!40000 ALTER TABLE `proxycolauth` DISABLE KEYS */;
/*!40000 ALTER TABLE `proxycolauth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `proxydb`
--

DROP TABLE IF EXISTS `proxydb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proxydb` (
  `id` varchar(32) NOT NULL COMMENT '提供给sdk的实例名',
  `type` varchar(10) NOT NULL COMMENT '数据库类型',
  `host` varchar(15) NOT NULL COMMENT '数据库地址',
  `port` int(11) NOT NULL COMMENT '数据库端口',
  `driverclass` varchar(45) NOT NULL COMMENT '驱动类',
  `driverpath` varchar(255) NOT NULL COMMENT '驱动jar路径',
  `dbname` varchar(45) DEFAULT NULL COMMENT '数据库实例',
  `dbuser` varchar(45) NOT NULL COMMENT '连接用户',
  `userpwd` varchar(45) NOT NULL COMMENT '连接用户密码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `proxydb`
--

LOCK TABLES `proxydb` WRITE;
/*!40000 ALTER TABLE `proxydb` DISABLE KEYS */;
/*!40000 ALTER TABLE `proxydb` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `proxydbauth`
--

DROP TABLE IF EXISTS `proxydbauth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proxydbauth` (
  `dbkey` varchar(32) NOT NULL,
  `username` varchar(45) NOT NULL,
  `priv` text,
  PRIMARY KEY (`dbkey`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `proxydbauth`
--

LOCK TABLES `proxydbauth` WRITE;
/*!40000 ALTER TABLE `proxydbauth` DISABLE KEYS */;
/*!40000 ALTER TABLE `proxydbauth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `proxyfilter`
--

DROP TABLE IF EXISTS `proxyfilter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proxyfilter` (
  `dbkey` varchar(32) NOT NULL,
  `username` varchar(45) NOT NULL,
  `tablename` varchar(45) NOT NULL,
  `colname` varchar(45) NOT NULL,
  `filterval` varchar(45) NOT NULL,
  PRIMARY KEY (`dbkey`,`username`,`tablename`,`colname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `proxyfilter`
--

LOCK TABLES `proxyfilter` WRITE;
/*!40000 ALTER TABLE `proxyfilter` DISABLE KEYS */;
/*!40000 ALTER TABLE `proxyfilter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `proxytableauth`
--

DROP TABLE IF EXISTS `proxytableauth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proxytableauth` (
  `dbkey` varchar(32) NOT NULL,
  `username` varchar(45) NOT NULL,
  `tablename` varchar(45) NOT NULL,
  `priv` text,
  `colpriv` text,
  PRIMARY KEY (`dbkey`,`username`,`tablename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `proxytableauth`
--

LOCK TABLES `proxytableauth` WRITE;
/*!40000 ALTER TABLE `proxytableauth` DISABLE KEYS */;
/*!40000 ALTER TABLE `proxytableauth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `proxyuser`
--

DROP TABLE IF EXISTS `proxyuser`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proxyuser` (
  `name` varchar(45) NOT NULL COMMENT '用户名',
  `pwd` varchar(45) NOT NULL COMMENT '用户密码',
  `priv` text COMMENT '用户权限',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `proxyuser`
--

LOCK TABLES `proxyuser` WRITE;
/*!40000 ALTER TABLE `proxyuser` DISABLE KEYS */;
/*!40000 ALTER TABLE `proxyuser` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-06-12 11:21:57
