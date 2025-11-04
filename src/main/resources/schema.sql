CREATE TABLE IF NOT EXISTS `locations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `device_id` VARCHAR(100) NOT NULL,
  `lat` DOUBLE NOT NULL,
  `lng` DOUBLE NOT NULL,
  `accuracy` DOUBLE NULL,
  `provider` VARCHAR(100) NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_device_id` (`device_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;