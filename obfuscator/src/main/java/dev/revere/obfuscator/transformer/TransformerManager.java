package dev.revere.obfuscator.transformer;

import dev.revere.obfuscator.JarProcessor;
import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.logging.Logger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
@Getter
public class TransformerManager {
    private static final Logger LOGGER = Logger.getLogger(TransformerManager.class.getName());

    private final List<AbstractTransformer> transformers;
    private final TransformationContext context;

    public TransformerManager(JarProcessor jarProcessor) {
        this.transformers = new ArrayList<>();
        this.context = new TransformationContext(jarProcessor);
    }

    public void addTransformer(AbstractTransformer transformer) {
        transformers.add(transformer);
    }

    public AbstractTransformer getTransformer(String name) {
        for (AbstractTransformer transformer : transformers) {
            if (transformer.getName().equals(name)) {
                return transformer;
            }
        }

        return null;
    }

    public void collectInformation(Map<String, byte[]> classes, Configuration config) throws ObfuscationException {
        for (AbstractTransformer transformer : transformers) {
            if (config.isTransformerEnabled(transformer.getName())) {
                transformer.collectInformation(classes, config, context);
            }
        }
    }

    public Map<String, byte[]> applyTransformations(Map<String, byte[]> classes, Configuration config) throws ObfuscationException {
        Map<String, byte[]> transformedClasses = new HashMap<>(classes);

        for (Transformer transformer : transformers) {
            if (config.isTransformerEnabled(transformer.getName())) {
                try {
                    LOGGER.debug("Applying transformer " + transformer.getName());
                    transformedClasses = transformer.transformAllClasses(transformedClasses, config, context);
                } catch (ObfuscationException e) {
                    LOGGER.severe("Failed to apply transformer " + transformer.getName());
                    throw e;
                }
            }
        }

        return transformedClasses;
    }
}