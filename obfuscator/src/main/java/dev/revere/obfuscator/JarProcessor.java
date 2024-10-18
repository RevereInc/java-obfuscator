package dev.revere.obfuscator;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.TransformerManager;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class JarProcessor {
    private static final Logger LOGGER = Logger.getLogger(JarProcessor.class.getName());

    @Setter private TransformerManager transformerManager;
    @Getter private URLClassLoader classLoader;
    private final Configuration config;

    public JarProcessor(TransformerManager transformerManager, Configuration config) {
        this.transformerManager = transformerManager;
        this.config = config;
    }

    public void process(Path inputPath, Path outputPath) throws ObfuscationException, IOException {
        createClassLoader(inputPath);

        try {
            collectInformation(inputPath);
            applyTransformationsAndWrite(inputPath, outputPath);
        } finally {
            if (classLoader != null) {
                classLoader.close();
            }
        }
    }

    private Map<String, byte[]> collectInformation(Path inputPath) throws IOException, ObfuscationException {
        Map<String, byte[]> classes = new HashMap<>();
        try (JarFile jarFile = new JarFile(inputPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        byte[] classBytes = readAllBytes(is);
                        String className = entry.getName();
                        classes.put(className, classBytes);
                    }
                }
            }
        }
        transformerManager.collectInformation(classes, config);
        return classes;
    }

    private void applyTransformationsAndWrite(Path inputPath, Path outputPath) throws IOException, ObfuscationException {
        Map<String, byte[]> classes = readAllClasses(inputPath);
        Set<String> resourceEntries = readResourceEntries(inputPath);

        Map<String, byte[]> transformedClasses = transformerManager.applyTransformations(classes, config);

        writeTransformedJar(outputPath, transformedClasses, resourceEntries, inputPath);
    }

    private Set<String> readResourceEntries(Path inputPath) throws IOException {
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

    private void writeTransformedJar(Path outputPath, Map<String, byte[]> classes, Set<String> resourceEntries, Path inputPath) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(outputPath))) {
            for (Map.Entry<String, byte[]> classEntry : classes.entrySet()) {
                jos.putNextEntry(new JarEntry(classEntry.getKey()));
                jos.write(classEntry.getValue());
                jos.closeEntry();
            }

            // Write resource entries
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


    private Map<String, byte[]> readAllClasses(Path inputPath) throws IOException {
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

    private void createClassLoader(Path inputPath) throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        urls.add(inputPath.toUri().toURL());

        for (String libraryPath : config.getLibraryPaths()) {
            urls.add(new File(libraryPath).toURI().toURL());
        }

        URL[] urlArray = urls.toArray(new URL[0]);
        classLoader = new URLClassLoader(urlArray, getClass().getClassLoader());
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