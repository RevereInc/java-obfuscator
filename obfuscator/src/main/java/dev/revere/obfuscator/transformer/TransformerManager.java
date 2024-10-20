package dev.revere.obfuscator.transformer;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.transformers.AsciiArtTransformer;
import dev.revere.obfuscator.transformer.transformers.FieldTransformer;
import dev.revere.obfuscator.transformer.transformers.MethodTransformer;
import dev.revere.obfuscator.transformer.transformers.StringTransformer;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
@Getter
public class TransformerManager {
    private static final Logger LOGGER = Logger.getLogger(TransformerManager.class.getName());

    private final List<AbstractTransformer> transformers;
    private final Configuration config;

    public TransformerManager(Configuration config) {
        this.transformers = new ArrayList<>();
        this.config = config;
        initializeTransformers();
    }

    private void initializeTransformers() {
        addTransformer(new AsciiArtTransformer());
        addTransformer(new MethodTransformer());
        addTransformer(new StringTransformer());
        addTransformer(new FieldTransformer());

        for (String transformer : config.getEnabledTransformers()) {
            if (getTransformer(transformer) == null) {
                LOGGER.warning("Transformer not found: " + transformer);
            }
        }
    }

    public void addTransformer(AbstractTransformer transformer) {
        transformers.add(transformer);
    }

    public AbstractTransformer getTransformer(String name) {
        return transformers.stream()
                .filter(transformer -> transformer.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}