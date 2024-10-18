package dev.revere.obfuscator.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
@Getter
@AllArgsConstructor
public enum LogLevel {
    SEVERE("\u001B[31m", "[!]"),
    WARNING("\u001B[33m", "[?]"),
    SUCCESS("\u001B[32m", "[+]"),
    CONFIG("\u001B[37m", "[⚙]"),
    INFO("\u001B[34m", "[i]"),
    DEBUG("\u001B[35m", "[*]"),
    FINEST("\u001B[37m", "[*]");

    private final String color;
    private final String symbol;
}