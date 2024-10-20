package dev.revere.obfuscator;

import dev.revere.obfuscator.classloader.ClassLoaderProvider;
import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.hierarchy.Hierarchy;
import dev.revere.obfuscator.jar.JarHandler;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.AbstractTransformer;
import dev.revere.obfuscator.transformer.TransformerManager;
import dev.revere.obfuscator.transformer.context.TransformerContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class JarProcessor implements ClassLoaderProvider {
    private static final Logger LOGGER = Logger.getLogger(JarProcessor.class.getName());

    private final JarHandler jarHandler;
    private final TransformerManager transformerManager;
    private final Configuration config;

    private URLClassLoader classLoader;

    public JarProcessor(Configuration config, TransformerManager transformerManager) {
        this.config = config;
        this.transformerManager = transformerManager;
        this.jarHandler = new JarHandler();
    }

    public void process(Path inputPath, Path outputPath) throws ObfuscationException, IOException {
        createClassLoader(inputPath);

        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            Map<String, byte[]> classes = jarHandler.readClasses(inputPath);
            Set<String> resourceEntries = jarHandler.readResourceEntries(inputPath);

            Hierarchy hierarchy = buildHierarchy(classes);

            TransformerContext context = new TransformerContext(hierarchy);

            for (AbstractTransformer transformer : transformerManager.getTransformers()) {
                if (config.isTransformerEnabled(transformer.getName())) {
                    LOGGER.debug("Applying transformer: " + transformer.getName());
                    classes = applyTransformer(transformer, classes, context);
                }
            }

            jarHandler.writeJar(outputPath, classes, resourceEntries, inputPath);
        } finally {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (IOException e) {
                    LOGGER.severe("Error closing ClassLoader: " + e.getMessage());
                }
            }
        }
    }

    private Hierarchy buildHierarchy(Map<String, byte[]> classes) {
        Hierarchy hierarchy = new Hierarchy();
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            ClassReader cr = new ClassReader(entry.getValue());
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);
            hierarchy.addClass(classNode);
        }
        hierarchy.buildHierarchy();
        return hierarchy;
    }

    private Map<String, byte[]> applyTransformer(AbstractTransformer transformer, Map<String, byte[]> classes, TransformerContext context) throws ObfuscationException {
        Map<String, ClassNode> classNodes = new HashMap<>();

        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            ClassReader cr = new ClassReader(entry.getValue());
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);
            classNodes.put(entry.getKey(), classNode);
        }

        classNodes = transformer.transform(classNodes, config, context);

        Map<String, byte[]> transformedClasses = new HashMap<>();
        for (Map.Entry<String, ClassNode> entry : classNodes.entrySet()) {
            ClassWriter cw = transformer.getClassWriter(this);
            entry.getValue().accept(cw);
            transformedClasses.put(entry.getKey(), cw.toByteArray());
        }

        return transformedClasses;
    }

    private void createClassLoader(Path inputPath) throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        urls.add(inputPath.toUri().toURL());

        for (String libraryPath : config.getLibraryPaths()) {
            File libraryFile = new File(libraryPath);
            if (libraryFile.isDirectory()) {
                File[] jarFiles = libraryFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (jarFiles != null) {
                    for (File jarFile : jarFiles) {
                        urls.add(jarFile.toURI().toURL());
                    }
                }
            } else if (libraryFile.isFile() && libraryFile.getName().toLowerCase().endsWith(".jar")) {
                urls.add(libraryFile.toURI().toURL());
            }
        }

        URL[] urlArray = urls.toArray(new URL[0]);
        classLoader = new URLClassLoader(urlArray, getClass().getClassLoader());
    }

    @Override
    public URLClassLoader getClassLoader() {
        return classLoader;
    }
}