package no.vebb.f1.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import no.vebb.f1.util.TimeUtil;

@Component
public class DatabaseBackup {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseBackup.class);

	@Scheduled(fixedDelay = TimeUtil.DAY, initialDelay = TimeUtil.MINUTE)
	@PreDestroy
	public void backupDatabase() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String backupFilePath = String.format("./backup/f1-backup-%s.db", time);
        String originalDbPath = "./f1.db";

        File originalFile = new File(originalDbPath);
        File backupDir = new File("./backup");
        try {
            if (!backupDir.exists()) {
                if (backupDir.mkdirs()) {
			        logger.info("Could not make backup directory");
                    throw new RuntimeException("Could not make backup directory");
                }
            }
            File backupFile = new File(backupFilePath);
			logger.info("Starting backup of database");
            Files.copy(originalFile.toPath(), backupFile.toPath());
			logger.info("Successful backup of database to '{}'", backupFilePath);
        } catch (IOException e) {
			logger.info("Failed backup of database");
        }
    }
}
