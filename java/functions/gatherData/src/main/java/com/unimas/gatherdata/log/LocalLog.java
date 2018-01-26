package com.unimas.gatherdata.log;

import com.unimas.gatherdata.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.logging.*;

/**
 * jdk自带的logger
 * 原本直接使用jdk,然而需要输出kafka,则使用log4j
 */
@Deprecated
public class LocalLog {

    private static Logger globalLog;

    static {
        globalLog = Logger.getLogger("gather_data");
        globalLog.setLevel(Level.INFO);
        addConsoleHandler(globalLog);
        addFileHandler(globalLog);
        globalLog.setUseParentHandlers(false);
    }

    public static Logger getLogger() {
        return globalLog;
    }

    private static void addConsoleHandler(Logger logger) {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        logger.addHandler(consoleHandler);
    }

    private static void addFileHandler(Logger logger) {
        try {
            File logFile = Paths.get(Main.log_dir.toString(), "gather.log").toFile();
            if (logFile.exists()) {
                DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                        .appendPattern("uuuu-MM-dd HH")
                        .toFormatter();
                Files.move(logFile.toPath(),
                        Paths.get(logFile.getAbsolutePath(), ".", dateTimeFormatter.format(OffsetDateTime.now())),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath());
            fileHandler.setEncoding("utf-8");
            fileHandler.setFormatter(new Formatter() {
                DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                        .appendPattern("uuuu-MM-dd HH:mm:ss.SSSX")
                        .toFormatter();

                @Override
                public String format(LogRecord record) {
                    StringBuilder log = new StringBuilder(dateTimeFormatter.format(OffsetDateTime.now()))
                            .append(" - ").append(record.getLevel().getName()).append(": ");
                    if (record.getThrown() != null) log.append(record.getThrown().toString());
                    else log.append(record.getSourceClassName()).append("-")
                            .append(record.getSourceMethodName())
                            .append(record.getMessage());
                    log.append("\n");
                    return log.toString();
                }
            });
            logger.addHandler(fileHandler);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }
}
