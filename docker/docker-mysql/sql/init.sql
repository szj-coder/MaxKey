create database if not exists  `maxkey` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */ ;

use maxkey ;

source /docker-entrypoint-initdb.d/v3.5.6.ga/maxkey_v3.5.6.GA.sql   ;
source /docker-entrypoint-initdb.d/v3.5.6.ga/maxkey_v3.5.6.GA_data.sql   ;
 