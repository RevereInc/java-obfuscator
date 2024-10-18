package dev.revere.obfuscator.transformer;

import dev.revere.obfuscator.JarProcessor;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
@Getter
public class TransformationContext {
    private final Map<String, Map<String, String>> fieldMappings;
    private final Map<String, Map<String, String>> methodMappings;
    private final JarProcessor jarProcessor;

    public TransformationContext(JarProcessor jarProcessor) {
        this.fieldMappings = new HashMap<>();
        this.methodMappings = new HashMap<>();
        this.jarProcessor = jarProcessor;
    }

    public void addFieldMapping(String className, String oldName, String newName) {
        fieldMappings.computeIfAbsent(className, k -> new HashMap<>()).put(oldName, newName);
    }

    public String getNewFieldName(String className, String oldName) {
        Map<String, String> classMappings = fieldMappings.get(className);
        if (classMappings == null) {
            return oldName;
        }
        return classMappings.getOrDefault(oldName, oldName);
    }

    public void addMethodMapping(String className, String oldName, String newName) {
        methodMappings.computeIfAbsent(className, k -> new HashMap<>()).put(oldName, newName);
    }

    public String getNewMethodName(String className, String oldName) {
        Map<String, String> classMappings = methodMappings.get(className);
        if (classMappings == null) {
            return oldName;
        }
        return classMappings.getOrDefault(oldName, oldName);
    }

    public Map<String, String> getFieldMappings(String className) {
        return Collections.unmodifiableMap(fieldMappings.getOrDefault(className, new HashMap<>()));
    }

    public Map<String, String> getMethodMappings(String className) {
        return Collections.unmodifiableMap(methodMappings.getOrDefault(className, new HashMap<>()));
    }
}