package dev.revere.obfuscator.util;

import java.util.regex.Pattern;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class PatternMatcher {
    public static boolean matches(String pattern, String input) {
        if (pattern.equals("*")) {
            return true;
        }

        String regexPattern = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return Pattern.matches(regexPattern, input);
    }
}