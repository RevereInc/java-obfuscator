package dev.revere.obfuscator.transformer.context;

import dev.revere.obfuscator.hierarchy.Hierarchy;
import dev.revere.obfuscator.logging.Logger;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/19/2024
 */
public class TransformerContext {
    private static final Logger LOGGER = Logger.getLogger(TransformerContext.class.getName());

    @Getter
    private final Map<String, Set<String>> protectedFields;
    private final Map<String, Set<String>> protectedMethods;

    @Getter
    private final Hierarchy hierarchy;

    public TransformerContext(Hierarchy hierarchy) {
        this.protectedFields = new HashMap<>();
        this.protectedMethods = new HashMap<>();
        this.hierarchy = hierarchy;
    }

    public void addProtectedField(String className, String fieldName) {
        protectedFields.computeIfAbsent(className, k -> new HashSet<>()).add(fieldName);
    }

    public void addProtectedMethod(String className, String methodName) {
        protectedMethods.computeIfAbsent(className, k -> new HashSet<>()).add(methodName);
    }

    public boolean isFieldProtected(String className, String fieldName) {
        Set<String> fields = protectedFields.get(className);
        return fields != null && fields.contains(fieldName);
    }

    public boolean isMethodProtected(String className, String methodName) {
        Set<String> methods = protectedMethods.get(className);
        return methods != null && methods.contains(methodName);
    }
}