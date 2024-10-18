package dev.revere.obfuscator.logging;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class LoggerConfig {
    private static boolean configured = false;

    public static synchronized void configureLogger() {
        if (configured) {
            return;
        }
        Logger.setLevel(LogLevel.FINEST);

        configured = true;
    }
}