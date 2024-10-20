package dev.revere.obfuscator.jar;

import dev.revere.obfuscator.logging.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/19/2024
 */
public class JarHandler implements JarReader, JarWriter {
    private static final Logger LOGGER = Logger.getLogger(JarHandler.class.getName());

    @Override
    public Map<String, byte[]> readClasses(Path inputPath) throws IOException {
        Map<String, byte[]> classes = new HashMap<>();
        try (JarFile jarFile = new JarFile(inputPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        classes.put(entry.getName(), readAllBytes(is));
                    }
                }
            }
        }
        return classes;
    }

    @Override
    public Set<String> readResourceEntries(Path inputPath) throws IOException {
        Set<String> resourceEntries = new HashSet<>();
        try (JarFile jarFile = new JarFile(inputPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && !entry.getName().endsWith(".class")) {
                    resourceEntries.add(entry.getName());
                }
            }
        }
        return resourceEntries;
    }

    @Override
    public void writeJar(Path outputPath, Map<String, byte[]> classes, Set<String> resourceEntries, Path inputPath) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(outputPath))) {
            for (Map.Entry<String, byte[]> classEntry : classes.entrySet()) {
                jos.putNextEntry(new JarEntry(classEntry.getKey()));
                jos.write(classEntry.getValue());
                jos.closeEntry();
            }

            try (JarFile inputJar = new JarFile(inputPath.toFile())) {
                for (String resourceEntry : resourceEntries) {
                    JarEntry entry = inputJar.getJarEntry(resourceEntry);
                    jos.putNextEntry(new JarEntry(resourceEntry));
                    try (InputStream is = inputJar.getInputStream(entry)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            jos.write(buffer, 0, bytesRead);
                        }
                    }
                    jos.closeEntry();
                }
            }
        }
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }
        return bos.toByteArray();
    }
}