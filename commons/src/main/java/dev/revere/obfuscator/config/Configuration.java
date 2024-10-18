package dev.revere.obfuscator.config;

import dev.revere.obfuscator.logging.Logger;
import lombok.Setter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
@Setter
public class Configuration {
    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    private Map<String, Boolean> enabledTransformers;
    private Map<String, List<String>> inclusions;
    private Map<String, List<String>> exclusions;

    private List<String> globalInclusions;
    private List<String> globalExclusions;
    private List<String> libraryPaths;

    public Configuration() {
        this.enabledTransformers = new HashMap<>();
        this.globalInclusions = new ArrayList<>();
        this.globalExclusions = new ArrayList<>();
        this.libraryPaths = new ArrayList<>();
        this.inclusions = new HashMap<>();
        this.exclusions = new HashMap<>();
    }

    public static Configuration loadFromFile(String filePath) throws IOException {
        LOGGER.process("Loading configuration from: " + filePath);

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            props.load(fis);
        }

        Configuration config = new Configuration();
        String[] transformers = props.getProperty("transformers.enabled", "").split(",");

        for (String transformer : transformers) {
            config.setTransformerEnabled(transformer.trim(), true);
        }

        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("inclusions.")) {
                String transformer = key.substring("inclusions.".length());
                String[] patterns = props.getProperty(key).split(",");

                for (String pattern : patterns) {
                    config.addInclusion(transformer, pattern.trim());
                }
            } else if (key.startsWith("exclusions.")) {
                String transformer = key.substring("exclusions.".length());
                String[] patterns = props.getProperty(key).split(",");

                for (String pattern : patterns) {
                    config.addExclusion(transformer, pattern.trim());
                }
            }
        }

        String globalInclusionsString = props.getProperty("global.inclusions", "");
        String globalExclusionsString = props.getProperty("global.exclusions", "");

        for (String pattern : globalInclusionsString.split(",")) {
            config.addGlobalInclusion(pattern.trim());
        }

        for (String pattern : globalExclusionsString.split(",")) {
            config.addGlobalExclusion(pattern.trim());
        }

        String libsFolderPath = props.getProperty("libs.folder");
        if (libsFolderPath != null && !libsFolderPath.isEmpty()) {
            config.loadLibraries(Paths.get(libsFolderPath));
        }

        LOGGER.success("Configuration loaded successfully.");
        return config;
    }

    private void loadLibraries(Path libsFolder) throws IOException {
        LOGGER.process("Loading libraries from: " + libsFolder);

        if (Files.exists(libsFolder) && Files.isDirectory(libsFolder)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(libsFolder, "*.jar")) {
                for (Path path : stream) {
                    addLibraryPath(path.toString());
                    LOGGER.debug("Added library: " + path);
                }
            }
        } else {
            LOGGER.warning("Libraries folder not found or is not a directory: " + libsFolder);
        }
    }

    public void setTransformerEnabled(String transformerName, boolean enabled) {
        enabledTransformers.put(transformerName, enabled);
    }

    public boolean isTransformerEnabled(String transformerName) {
        return enabledTransformers.getOrDefault(transformerName, false);
    }

    public Set<String> getEnabledTransformers() {
        return Collections.unmodifiableSet(enabledTransformers.keySet());
    }

    public void addInclusion(String transformerName, String pattern) {
        inclusions.computeIfAbsent(transformerName, k -> new ArrayList<>()).add(pattern);
    }

    public void addExclusion(String transformerName, String pattern) {
        exclusions.computeIfAbsent(transformerName, k -> new ArrayList<>()).add(pattern);
    }

    public List<String> getInclusions(String transformerName) {
        return inclusions.getOrDefault(transformerName, new ArrayList<>());
    }

    public List<String> getExclusions(String transformerName) {
        return exclusions.getOrDefault(transformerName, new ArrayList<>());
    }

    public void addGlobalInclusion(String pattern) {
        globalInclusions.add(pattern);
    }

    public void addGlobalExclusion(String pattern) {
        globalExclusions.add(pattern);
    }

    public List<String> getGlobalInclusions() {
        return Collections.unmodifiableList(globalInclusions);
    }

    public List<String> getGlobalExclusions() {
        return Collections.unmodifiableList(globalExclusions);
    }

    public void addLibraryPath(String path) {
        libraryPaths.add(path);
    }

    public List<String> getLibraryPaths() {
        return Collections.unmodifiableList(libraryPaths);
    }
}
