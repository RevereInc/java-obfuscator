package dev.revere.obfuscator.transformer;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.logging.Logger;

import java.util.List;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class TransformerFilter {
    private static final Logger LOGGER = Logger.getLogger(TransformerFilter.class.getName());

    public static boolean shouldTransform(String className, String transformerName, Configuration config) {
        boolean isGloballyExcluded = matchesAny(className, config.getGlobalExclusions());
        boolean isGloballyIncluded = matchesAny(className, config.getGlobalInclusions());
        boolean isTransformerExcluded = matchesAny(className, config.getExclusions(transformerName));
        boolean isTransformerIncluded = matchesAny(className, config.getInclusions(transformerName));

        if (isGloballyExcluded) {
            if (isTransformerIncluded) {
                LOGGER.debug("Including class " + className + " because it is included by transformer " + transformerName);
                return true;
            }
            return false;
        }

        if (isGloballyIncluded) {
            if (isTransformerExcluded) {
                LOGGER.debug("Skipping class " + className + " because it is excluded by transformer " + transformerName);
                return false;
            }

            return true;
        }

        if (isTransformerIncluded) {
            LOGGER.debug("Including class " + className + " because it is included by transformer " + transformerName);

            if (isTransformerExcluded) {
                LOGGER.debug("Skipping class " + className + " because it is excluded by transformer " + transformerName);
                return false;
            }
            return true;
        }

        // If the class is neither globally included nor excluded, check transformer-specific exclusions
        if (isTransformerExcluded) {
            LOGGER.debug("Skipping class " + className + " because it is excluded by transformer " + transformerName);
            return false;
        }

        return true;
    }
    private static boolean matchesAny(String className, List<String> patterns) {
        for (String pattern : patterns) {
            if (matchesPattern(className, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPattern(String className, String pattern) {
        if (pattern.endsWith("/**")) {
            String basePattern = pattern.substring(0, pattern.length() - 3);
            return className.startsWith(basePattern.replace('/', '.'));
        } else if (pattern.startsWith("^")) {
            String specificClass = pattern.substring(1);
            return className.equals(specificClass.replace('/', '.'));
        } else {
            return className.equals(pattern.replace('/', '.'));
        }
    }
}