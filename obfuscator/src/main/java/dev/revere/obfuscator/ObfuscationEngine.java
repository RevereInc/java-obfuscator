package dev.revere.obfuscator;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.TransformerManager;
import dev.revere.obfuscator.transformer.transformers.AsciiArtTransformer;
import dev.revere.obfuscator.transformer.transformers.FieldTransformer;
import dev.revere.obfuscator.transformer.transformers.StringTransformer;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class ObfuscationEngine {
    private static final Logger LOGGER = Logger.getLogger(ObfuscationEngine.class.getName());

    private TransformerManager transformerManager;

    public void obfuscate(Path inputPath, Path outputPath, Configuration config) throws ObfuscationException, IOException {
        JarProcessor jarProcessor = new JarProcessor(transformerManager, config);
        this.transformerManager = new TransformerManager(jarProcessor);
        this.transformerManager.addTransformer(new AsciiArtTransformer());
        this.transformerManager.addTransformer(new StringTransformer());
        this.transformerManager.addTransformer(new FieldTransformer());

        for (String transformer : config.getEnabledTransformers()) {
            if (transformerManager.getTransformer(transformer) == null) {
                LOGGER.warning("Transformer not found: " + transformer);
            }
        }

        jarProcessor.setTransformerManager(transformerManager);
        jarProcessor.process(inputPath, outputPath);
    }
}