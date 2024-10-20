package dev.revere.obfuscator;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.TransformerManager;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class ObfuscationEngine {
    private static final Logger LOGGER = Logger.getLogger(ObfuscationEngine.class.getName());

    private final JarProcessor jarProcessor;

    public ObfuscationEngine(Configuration config) {
        TransformerManager transformerManager = new TransformerManager(config);
        this.jarProcessor = new JarProcessor(config, transformerManager);
    }

    public void obfuscate(Path inputPath, Path outputPath) throws ObfuscationException, IOException {
        jarProcessor.process(inputPath, outputPath);
    }
}