package dev.revere.obfuscator.transformer;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public abstract class AbstractTransformer implements Transformer {
    private final String name;

    protected AbstractTransformer(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, byte[]> transformAllClasses(Map<String, byte[]> classes, Configuration config, TransformationContext context) throws ObfuscationException {
        Map<String, byte[]> transformedClasses = new HashMap<>(classes);
        Map<String, byte[]> newClasses = new HashMap<>();

        for (Map.Entry<String, byte[]> entry : new HashMap<>(classes).entrySet()) {
            String className = entry.getKey().replace('/', '.').replace(".class", "");
            byte[] classBytes = entry.getValue();
            if (TransformerFilter.shouldTransform(className, getName(), config)) {
                byte[] transformedBytes = doTransform(classBytes, className, config, context, transformedClasses, newClasses);
                if (transformedBytes != null) {
                    transformedClasses.put(entry.getKey(), transformedBytes);
                }
            }
        }

        transformedClasses.putAll(newClasses);
        return transformedClasses;
    }

    public void collectInformation(Map<String, byte[]> classes, Configuration config, TransformationContext context) throws ObfuscationException {}
    protected abstract byte[] doTransform(byte[] classBytes, String className, Configuration config, TransformationContext context, Map<String, byte[]> allClasses, Map<String, byte[]> newClasses) throws ObfuscationException;
}