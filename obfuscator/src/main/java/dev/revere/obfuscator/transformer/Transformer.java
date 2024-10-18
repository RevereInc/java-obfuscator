package dev.revere.obfuscator.transformer;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;

import java.util.Map;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public interface Transformer {
    String getName();
    Map<String, byte[]> transformAllClasses(Map<String, byte[]> classes, Configuration config, TransformationContext context) throws ObfuscationException;
}