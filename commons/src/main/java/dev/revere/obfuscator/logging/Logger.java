package dev.revere.obfuscator.logging;

import dev.revere.obfuscator.Environment;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */

public class Logger {
    private static final String RESET = "\u001B[0m";
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static LogLevel currentLevel = LogLevel.SUCCESS;
    private static boolean useColor = true;

    private final String name;

    public Logger(String name) {
        this.name = name;
    }

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public static void setLevel(LogLevel level) {
        currentLevel = level;
    }

    public static void setUseColor(boolean useColor) {
        Logger.useColor = useColor;
    }

    private void log(LogLevel level, String message) {
        if (level.ordinal() <= currentLevel.ordinal()) {
            String formattedMessage = formatMessage(level, message);
            System.out.println(formattedMessage);
        }
    }

    private String formatMessage(LogLevel level, String message) {
        LocalTime time = LocalTime.now();
        String formattedTime = time.format(timeFormatter);
        String colorStart = useColor ? level.getColor() : "";
        String colorEnd = useColor ? RESET : "";

        return String.format("%s%s | %s %s | %s: %s%s",
                colorStart, formattedTime, level.getSymbol(), level.name(), name, message, colorEnd);
    }

    public void severe(String message) {
        log(LogLevel.SEVERE, message);
    }

    public void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    public void success(String message) {
        log(LogLevel.SUCCESS, message);
    }

    public void config(String message) {
        log(LogLevel.CONFIG, message);
    }

    public void process(String message) {
        log(LogLevel.INFO, message);
    }

    public void debug(String message) {
        if (Environment.DEBUG) {
            log(LogLevel.DEBUG, message);
        }
    }

    public void finest(String message) {
        log(LogLevel.FINEST, message);
    }
}