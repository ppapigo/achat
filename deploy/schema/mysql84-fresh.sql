-- FRESH DATABASE ONLY. Never apply to an existing database.
-- Exported from isolated MySQL 8.4 schema; contains no account/game rows.

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
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `augment_awards` (
  `points` int NOT NULL,
  `id` varchar(100) NOT NULL,
  `game_id` varchar(255) DEFAULT NULL,
  `round_id` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX5eud3w3xkh367072cqbum71bh` (`game_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `augment_progress` (
  `awarded_points` int NOT NULL,
  `draws` int NOT NULL,
  `messages_seen` int NOT NULL,
  `reward_count` int NOT NULL,
  `violations` int NOT NULL,
  `wins` int NOT NULL,
  `last_draw_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(100) NOT NULL,
  `forbidden_word` varchar(255) DEFAULT NULL,
  `room_id` varchar(255) DEFAULT NULL,
  `round_id` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `type` enum('CAPS_LOCK','CHAOS_ROULETTE','DOPPELGANGER','ECHO_BURST','FATE_DICE','FORBIDDEN_WORD','GHOST_MESSAGE','HARMONY_AURA','ICE_SEAL','INSIGHT_DROP','JACKPOT_MESSENGER','LIKE_OVERDRIVE','LINK_BRIDGE','MAIN_CHARACTER','QUEST_GIVER','REVERSE_TEXT','SEALED_MESSAGE','SHADOW_SPEAKER','SPOTLIGHT','THREAD_WEAVER','TIME_BOMB','TIME_CAPSULE','TIME_SHIFTER','TURBO_CASTER','VISUAL_SPELL','WHISPER','ZERO_GRAVITY') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `augment_reward_content` (
  `content_hashes` varchar(64) DEFAULT NULL,
  `progress_id` varchar(100) NOT NULL,
  KEY `FK7kl09deohv7b19ng4g5b66ck9` (`progress_id`),
  CONSTRAINT `FK7kl09deohv7b19ng4g5b66ck9` FOREIGN KEY (`progress_id`) REFERENCES `augment_progress` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_sessions` (
  `revoked` bit(1) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `password_fingerprint` varchar(64) NOT NULL,
  `refresh_hash` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_auth_session_user` (`user_id`),
  KEY `idx_auth_session_expiry` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_comments` (
  `hidden` bit(1) NOT NULL,
  `board_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `author_id` varchar(36) DEFAULT NULL,
  `author_nickname` varchar(255) DEFAULT NULL,
  `content` text,
  `hidden_reason` text,
  PRIMARY KEY (`id`),
  KEY `IDXcl5yunvnumydtf65rn80jex1y` (`board_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `boards` (
  `comment_count` int NOT NULL,
  `hidden` bit(1) NOT NULL,
  `like_count` int NOT NULL,
  `view_count` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `author_id` varchar(36) DEFAULT NULL,
  `author_nickname` varchar(255) DEFAULT NULL,
  `content` text,
  `hidden_reason` text,
  `title` varchar(255) DEFAULT NULL,
  `category` enum('FREE','GUIDE','NOTICE','QUESTION','TRADE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXah4vj4g3di9by1qbmv8tt0tu4` (`hidden`,`category`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `reward_eligible` bit(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `highlighted_at` datetime(6) DEFAULT NULL,
  `next_effect_at` datetime(6) DEFAULT NULL,
  `revision` bigint DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `reply_to_id` varchar(36) DEFAULT NULL,
  `room_id` varchar(36) DEFAULT NULL,
  `sender_id` varchar(36) DEFAULT NULL,
  `client_message_id` varchar(64) DEFAULT NULL,
  `applied_augment` varchar(255) DEFAULT NULL,
  `applied_content` text,
  `augment_effects` text,
  `content` text,
  `game_id` varchar(255) DEFAULT NULL,
  `reply_to_preview` varchar(255) DEFAULT NULL,
  `round_id` varchar(255) DEFAULT NULL,
  `sender_title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_client_request` (`room_id`,`sender_id`,`client_message_id`),
  KEY `IDXkt2g2stv2x2l43j8lv0b1f5b9` (`room_id`,`created_at`),
  KEY `IDX15bkgqt5agk95e59kdm9le2la` (`next_effect_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_quest_participants` (
  `quest_id` varchar(36) NOT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  KEY `FK11ihdf1d3b5bdro3m1wldvxsw` (`quest_id`),
  CONSTRAINT `FK11ihdf1d3b5bdro3m1wldvxsw` FOREIGN KEY (`quest_id`) REFERENCES `chat_quests` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_quests` (
  `cancelled` bit(1) NOT NULL,
  `completed` bit(1) NOT NULL,
  `current_count` int NOT NULL,
  `reward_coins` int NOT NULL,
  `target_count` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `message_id` varchar(36) DEFAULT NULL,
  `creator_id` varchar(255) DEFAULT NULL,
  `room_id` varchar(255) DEFAULT NULL,
  `type` enum('LIKE_QUEST','OPINION_QUEST','REPLY_QUEST') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKadqtbcpb8xibgtbeevujjbu0g` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint DEFAULT NULL,
  `target_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `target_type` enum('BOARD','MINI_HOME','POST') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKq0renxup427se2cw9p7q03t1r` (`target_type`,`target_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_slots` (
  `user_id` varchar(36) NOT NULL,
  `item_id` varchar(255) DEFAULT NULL,
  `equip_slot` enum('BACKGROUND','BORDER','EMOJI_SET','ROOM','TITLE') NOT NULL,
  PRIMARY KEY (`user_id`,`equip_slot`),
  CONSTRAINT `FKmm3rp714x617sxwt1fsjkpy9u` FOREIGN KEY (`user_id`) REFERENCES `user_equipment` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feed_comments` (
  `created_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `author_id` varchar(36) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `post_id` varchar(36) DEFAULT NULL,
  `author_username` varchar(255) DEFAULT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  KEY `IDXkpkvmxw7ahqg9082vhuqivf7k` (`post_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_connections` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint DEFAULT NULL,
  `friend_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhxrwrquu8esc79wp3jrvt9kn2` (`user_id`,`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_requests` (
  `created_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `from_user_id` varchar(36) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `to_user_id` varchar(36) DEFAULT NULL,
  `status` enum('ACCEPTED','PENDING','REJECTED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXaw0oswf91tbjgf4hove3l5yqq` (`to_user_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_histories` (
  `coins_earned` int NOT NULL,
  `exp_earned` int NOT NULL,
  `finish_rank` int DEFAULT NULL,
  `is_match_room` bit(1) NOT NULL,
  `message_count` int NOT NULL,
  `player_count` int NOT NULL,
  `rp_change` int NOT NULL,
  `total_points` int NOT NULL,
  `played_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `room_id` varchar(36) DEFAULT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  `augment_used` varchar(255) DEFAULT NULL,
  `room_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXnkf1x4afymumva8c4fsdbkfii` (`user_id`,`played_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `home_furniture` (
  `x` double DEFAULT NULL,
  `y` double DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  `furniture_id` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id`,`furniture_id`),
  CONSTRAINT `FK3cnqhcei2fmmccpr6tftqoe4s` FOREIGN KEY (`user_id`) REFERENCES `homes` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homes` (
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `visit_count` bigint NOT NULL,
  `representative_augment_id` varchar(36) DEFAULT NULL,
  `representative_job_id` varchar(36) DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  `avatar_bg_color` varchar(255) DEFAULT NULL,
  `avatar_seed` varchar(255) DEFAULT NULL,
  `avatar_style` varchar(255) DEFAULT NULL,
  `background_theme` varchar(255) DEFAULT NULL,
  `status_message` varchar(255) DEFAULT NULL,
  `title_badge` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_likes` (
  `message_id` varchar(36) NOT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  KEY `FKspsj0ybttsa2t5o8lk2yiue7o` (`message_id`),
  CONSTRAINT `FKspsj0ybttsa2t5o8lk2yiue7o` FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mini_home_posts` (
  `like_count` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `author_id` varchar(36) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  KEY `IDXfcag5yh0ku8mqbue9mhgodnt` (`author_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_quotas` (
  `count_date` date DEFAULT NULL,
  `daily_count` int NOT NULL,
  `last_post_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_tags` (
  `tag_index` int NOT NULL,
  `post_id` varchar(36) NOT NULL,
  `tag` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`tag_index`,`post_id`),
  KEY `FKkifam22p4s1nm3bkmp1igcn5w` (`post_id`),
  CONSTRAINT `FKkifam22p4s1nm3bkmp1igcn5w` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `created_at` datetime(6) DEFAULT NULL,
  `like_count` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `author_id` varchar(36) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `content` text,
  `type` enum('ACHIEVEMENT','FREE','GAME_LOG','QUESTION') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXgo212y1mdjgf4rao79gb4bovc` (`author_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `profile_cards` (
  `drawn_date` date DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint DEFAULT NULL,
  `owner_id` varchar(36) DEFAULT NULL,
  `target_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdq4fk81tldid71wq0nkprb1vv` (`owner_id`,`drawn_date`),
  UNIQUE KEY `UKlvfuc0tvf3euxh0wdd5w6ayo5` (`owner_id`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `created_at` datetime(6) DEFAULT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `reporter_id` varchar(36) DEFAULT NULL,
  `room_id` varchar(36) DEFAULT NULL,
  `target_id` varchar(36) DEFAULT NULL,
  `admin_note` text,
  `description` text,
  `reason` enum('ADVERTISEMENT','CHEATING','FALSE_INFO','HARASSMENT','HATE_SPEECH','ILLEGAL_CONTENT','IMPERSONATION','INAPPROPRIATE_CONTENT','INAPPROPRIATE_NAME','OTHER','PRIVACY_VIOLATION','SCAM','SPAM') DEFAULT NULL,
  `status` enum('PENDING','REJECTED','RESOLVED','REVIEWED') DEFAULT NULL,
  `target_type` enum('COMMENT','POST','ROOM','USER') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXfkmoayuudgw25957g7sgsqavs` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_invites` (
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `from_user_id` varchar(36) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `room_id` varchar(36) DEFAULT NULL,
  `to_user_id` varchar(36) DEFAULT NULL,
  `status` enum('ACCEPTED','EXPIRED','PENDING','REJECTED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXjq78pvtd4wgwwneqaa800t4ei` (`to_user_id`,`status`,`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_members` (
  `room_id` varchar(36) NOT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  KEY `FK1bbl9rh6ae8v6mebaoq2ilg9g` (`room_id`),
  CONSTRAINT `FK1bbl9rh6ae8v6mebaoq2ilg9g` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_ranks` (
  `activity_score` int NOT NULL,
  `participation_score` int NOT NULL,
  `penalty_score` int NOT NULL,
  `report_count` int NOT NULL,
  `retention_score` int NOT NULL,
  `return_user_count` int NOT NULL,
  `total_member_count` int NOT NULL,
  `unique_user_count` int NOT NULL,
  `valid_message_count` int NOT NULL,
  `last_activity_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `room_id` varchar(36) NOT NULL,
  PRIMARY KEY (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_sub_rooms` (
  `room_id` varchar(36) NOT NULL,
  `sub_room_id` varchar(36) DEFAULT NULL,
  KEY `FKlg06iv9bmdpgqh20pyak6g7as` (`room_id`),
  CONSTRAINT `FKlg06iv9bmdpgqh20pyak6g7as` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `max_members` int NOT NULL,
  `season_active_members` int NOT NULL,
  `season_id` int NOT NULL,
  `season_message_count` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `owner_id` varchar(36) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `parent_id` varchar(255) DEFAULT NULL,
  `type` enum('MATCH','PARTY','PERMANENT') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX8xjj7xbvjaaqihesnaay8odjr` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sanctions` (
  `is_active` bit(1) NOT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `issued_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `report_id` varchar(36) DEFAULT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  `issued_by` varchar(255) DEFAULT NULL,
  `reason` text,
  `type` enum('MUTE','PERM_BAN','TEMP_BAN','WARNING') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXkflglonkbhpbs2rei487vaex7` (`user_id`,`issued_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seasons` (
  `active` bit(1) NOT NULL,
  `id` int NOT NULL,
  `end_date` datetime(6) DEFAULT NULL,
  `start_date` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_equipment` (
  `version` bigint DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_interactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_interaction_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `target_user_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhgduoh8wkdnbuaiif9tey3sio` (`user_id`,`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_inventory_items` (
  `quantity` int NOT NULL,
  `acquired_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` bigint DEFAULT NULL,
  `item_id` varchar(36) DEFAULT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKffgsmd3yfq5apvb47bol2npi6` (`user_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_job_states` (
  `exp` int NOT NULL,
  `invite_actions` int NOT NULL,
  `knowledge_actions` int NOT NULL,
  `level` int NOT NULL,
  `moderation_actions` int NOT NULL,
  `room_design_actions` int NOT NULL,
  `version` bigint DEFAULT NULL,
  `user_id` varchar(36) NOT NULL,
  `current_job` enum('ARCHITECT','CONNECTOR','FREELANCER','GUARDIAN','MAVEN') DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_placement_ranks` (
  `finish_rank` int DEFAULT NULL,
  `game_index` int NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`game_index`,`user_id`),
  KEY `FKta5aghsdgauiuiv5c9f55vh76` (`user_id`),
  CONSTRAINT `FKta5aghsdgauiuiv5c9f55vh76` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_season_histories` (
  `final_rp` int NOT NULL,
  `reward_coins` int NOT NULL,
  `season_id` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recorded_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  `final_tier` enum('BRONZE','CHALLENGER','DIAMOND','GOLD','PLATINUM','SILVER','UNRANKED') DEFAULT NULL,
  `highest_tier` enum('BRONZE','CHALLENGER','DIAMOND','GOLD','PLATINUM','SILVER','UNRANKED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKotorj2vblh6oq7bn4pdu3xqbs` (`user_id`,`season_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `coins` int NOT NULL,
  `email_verified` bit(1) DEFAULT NULL,
  `exp` int NOT NULL,
  `is_admin` bit(1) NOT NULL,
  `level` int NOT NULL,
  `placement_completed` bit(1) NOT NULL,
  `ranked_games` int NOT NULL,
  `rp` int NOT NULL,
  `season_id` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `username` varchar(100) NOT NULL,
  `username_key` varchar(100) NOT NULL,
  `email` varchar(254) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `season_highest_tier` enum('BRONZE','CHALLENGER','DIAMOND','GOLD','PLATINUM','SILVER','UNRANKED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKf5uaubci8x7asr9x2bh0e9kmk` (`username_key`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
