package ru.edu.hse.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ColorfulLogger {
    private final DebugColor color;
    private final Logger logger;

    public ColorfulLogger(DebugColor color, Logger logger) {
        this.color = color;
        this.logger = logger;
    }

    public void log(Level level, String message) {
        logger.log(level, MessageFormat.format("\u001b[38;5;{0}m{1}\u001B[0m", color.getValue(), message));
    }
}

//public class BLYA {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(ColorLogger.class);
//
//    public void logDebug(String logging) {
//        LOGGER.debug("\u001B[34m" + logging + "\u001B[0m");
//    }
//    public void logInfo(String logging) {
//        LOGGER.info("\u001B[32m" + logging + "\u001B[0m");
//    }
//
//    public void logError(String logging) {
//        LOGGER.error("\u001B[31m" + logging + "\u001B[0m");
//    }
//}