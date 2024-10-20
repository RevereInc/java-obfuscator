package dev.revere.obfuscator.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/19/2024
 */
public interface JarReader {
    Map<String, byte[]> readClasses(Path inputPath) throws IOException;
    Set<String> readResourceEntries(Path inputPath) throws IOException;
}