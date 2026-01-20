package dev.osunolimits.modules;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheInvalidator {
    private static final Logger log = LoggerFactory.getLogger("CacheInvalidator");

    public static void clearUserCache() {
        clearCacheDirectory(".cache/users");
    }

    public static void clearLeaderboardCache() {
        clearCacheDirectory(".cache/leaderboard");
    }

    public static void clearAllApiCaches() {
        clearUserCache();
        clearLeaderboardCache();
    }

    private static void clearCacheDirectory(String directory) {
        Path cachePath = Paths.get(directory);
        if (!Files.exists(cachePath)) {
            return;
        }

        try {
            Files.walk(cachePath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            log.debug("Cleared cache directory: {}", directory);
        } catch (IOException e) {
            log.error("Failed to clear cache directory: {}", directory, e);
        }
    }
}
