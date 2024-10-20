package dev.revere.obfuscator.classloader;

import java.net.URLClassLoader;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/19/2024
 */
public interface ClassLoaderProvider {
    URLClassLoader getClassLoader();
}