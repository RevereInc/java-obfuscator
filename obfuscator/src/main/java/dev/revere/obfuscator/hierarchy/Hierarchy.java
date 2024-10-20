package dev.revere.obfuscator.hierarchy;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.TransformerFilter;
import lombok.Getter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/19/2024
 */
@Getter
public class Hierarchy {
    private static final Logger LOGGER = Logger.getLogger(Hierarchy.class.getName());

    private final Map<String, ClassNode> classes = new HashMap<>();
    private final Map<String, Set<String>> subclasses = new HashMap<>();
    private final Map<String, String> superclasses = new HashMap<>();
    private final Map<String, Set<MethodNode>> methodsInClass = new HashMap<>();
    private final Map<String, Set<String>> methodsInHierarchy = new HashMap<>();

    public void addClass(ClassNode classNode) {
        String className = classNode.name.replace('/', '.');
        classes.put(className, classNode);

        if (classNode.superName != null) {
            String superClassName = classNode.superName.replace('/', '.');
            superclasses.put(className, superClassName);
            subclasses.computeIfAbsent(superClassName, k -> new HashSet<>()).add(className);
        }

        Set<MethodNode> classMethods = new HashSet<>(classNode.methods);
        methodsInClass.put(className, classMethods);

        for (MethodNode methodNode : classMethods) {
            String methodKey = className + "." + methodNode.name + methodNode.desc;
            methodsInHierarchy.computeIfAbsent(methodKey, k -> new HashSet<>()).add(className);
        }
    }

    public void buildHierarchy() {
        for (Map.Entry<String, Set<String>> entry : subclasses.entrySet()) {
            String superClassName = entry.getKey();
            for (String subClassName : entry.getValue()) {
                superclasses.put(subClassName, superClassName);
            }
        }
    }

    public boolean isSubclassOf(String c1, String c2) {
        if (c1.equals(c2)) {
            return true;
        }
        String current = c1;
        while (current != null) {
            current = superclasses.get(current);
            if (current != null && current.equals(c2)) {
                return true;
            }
        }
        return false;
    }

    public ClassNode getClass(String className) {
        return classes.get(className);
    }

    public Set<String> getSubclasses(String className) {
        return subclasses.getOrDefault(className, Collections.emptySet());
    }

    public String getSuperclass(String className) {
        return superclasses.get(className);
    }

    public Set<MethodNode> getMethodsInClass(String className) {
        return methodsInClass.getOrDefault(className, Collections.emptySet());
    }

    public Set<String> getClassesWithMethod(String className, String methodName, String methodDesc) {
        String methodKey = className + "." + methodName + methodDesc;
        return methodsInHierarchy.getOrDefault(methodKey, Collections.emptySet());
    }

    public List<String> getHierarchy(String className) {
        List<String> hierarchy = new ArrayList<>();
        String current = className;
        while (current != null) {
            hierarchy.add(current);
            current = getSuperclass(current);
            if (current != null && !classes.containsKey(current)) {
                break;
            }
        }
        return hierarchy;
    }
}