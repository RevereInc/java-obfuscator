package dev.revere.obfuscator.transformer;

import dev.revere.obfuscator.classloader.ClassLoaderProvider;
import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.transformer.context.TransformerContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

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

    public abstract Map<String, ClassNode> transform(Map<String, ClassNode> classNodes, Configuration config, TransformerContext context) throws ObfuscationException;

    protected boolean shouldTransform(String className, Configuration config) {
        return TransformerFilter.shouldTransform(className, getName(), config);
    }

    public ClassWriter getClassWriter(ClassLoaderProvider classLoaderProvider) {
        return new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected ClassLoader getClassLoader() {
                return classLoaderProvider.getClassLoader();
            }
        };
    }
}