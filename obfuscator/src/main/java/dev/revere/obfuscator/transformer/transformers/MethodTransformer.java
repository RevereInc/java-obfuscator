package dev.revere.obfuscator.transformer.transformers;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.hierarchy.Hierarchy;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.AbstractTransformer;
import dev.revere.obfuscator.transformer.context.TransformerContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import java.security.MessageDigest;
import java.util.*;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class MethodTransformer extends AbstractTransformer {
    private static final Logger LOGGER = Logger.getLogger(MethodTransformer.class.getName());
    private final Random random;
    private Map<String, String> globalMethodMappings;

    public MethodTransformer() {
        super("MethodTransformer");
        this.random = new Random();
    }

    @Override
    public Map<String, ClassNode> transform(Map<String, ClassNode> classNodes, Configuration config, TransformerContext context) throws ObfuscationException {
        try {
            Map<String, ClassNode> transformedNodes = new HashMap<>(classNodes);
            globalMethodMappings = new HashMap<>();
            Hierarchy hierarchy = context.getHierarchy();

            for (Map.Entry<String, ClassNode> entry : transformedNodes.entrySet()) {
                String className = entry.getKey().replace('/', '.').replace(".class", "");
                ClassNode classNode = entry.getValue();
                renameMethodsInClass(classNode, className, config, hierarchy);
            }

            debugHierarchy(hierarchy, "me.emmy.alley.api.menu.Menu");
            debugHierarchy(hierarchy, "me.emmy.alley.api.menu.impl.PartyEventMenu");

            for (Map.Entry<String, ClassNode> entry : transformedNodes.entrySet()) {
                String className = entry.getKey().replace('/', '.').replace(".class", "");
                ClassNode classNode = entry.getValue();
                handleOverriddenMethods(classNode, className, hierarchy);
            }

            // Second pass: Update method references
            for (ClassNode classNode : transformedNodes.values()) {
                String className = classNode.name.replace('/', '.');
                for (MethodNode methodNode : classNode.methods) {
                    updateMethodReferences(methodNode, className, hierarchy);
                }
            }

            return transformedNodes;
        } catch (Exception e) {
            throw new ObfuscationException("Failed to transform methods: " + e.getMessage());
        }
    }

    private void renameMethodsInClass(ClassNode classNode, String className, Configuration config, Hierarchy hierarchy) {
        boolean isTargetClass = className.endsWith("PartyEventMenu") || className.endsWith("Menu");

        if (isTargetClass) {
            LOGGER.debug("Processing target class: " + className);
            LOGGER.debug("Superclass: " + hierarchy.getSuperclass(className));
            LOGGER.debug("Subclasses: " + hierarchy.getSubclasses(className));
        }

        // Rename methods in the current class
        for (MethodNode methodNode : classNode.methods) {
            if (!isExcludedMethod(methodNode, config)) {
                String methodSignature = className + "." + methodNode.name + methodNode.desc;
                if (!globalMethodMappings.containsKey(methodSignature)) {
                    String newName = generateSecureMethodName(methodNode.name);
                    globalMethodMappings.put(methodSignature, newName);
                    String oldName = methodNode.name;
                    methodNode.name = newName;
                    if (isTargetClass) {
                        LOGGER.debug("Renamed method in " + className + ": " + oldName + " -> " + newName);
                    }
                } else if (isTargetClass) {
                    LOGGER.debug("Method already renamed: " + methodSignature + " -> " + globalMethodMappings.get(methodSignature));
                }
            } else if (isTargetClass) {
                LOGGER.debug("Excluded method: " + className + "." + methodNode.name);
            }
        }
    }

    private void propagateRenamesToSubclasses(ClassNode classNode, String className, Hierarchy hierarchy) {
        Set<String> subclasses = hierarchy.getSubclasses(className);
        boolean isTargetClass = className.endsWith("PartyEventMenu") || className.endsWith("Menu");

        if (isTargetClass) {
            LOGGER.debug("Propagating renames to subclasses of " + className + ": " + subclasses);
        }

        for (String subclass : subclasses) {
            ClassNode subclassNode = hierarchy.getClass(subclass);
            if (subclassNode != null) {
                boolean isTargetSubclass = subclass.endsWith("PartyEventMenu") || subclass.endsWith("Menu");
                if (isTargetSubclass) {
                    LOGGER.debug("Processing target subclass: " + subclass);
                }
                for (MethodNode subMethodNode : subclassNode.methods) {
                    String superMethodSignature = findSuperMethodSignature(className, subMethodNode.name, subMethodNode.desc, hierarchy);
                    if (superMethodSignature != null) {
                        String newName = globalMethodMappings.get(superMethodSignature);
                        if (newName != null && !subMethodNode.name.equals(newName)) {
                            String oldName = subMethodNode.name;
                            subMethodNode.name = newName;
                            if (isTargetSubclass) {
                                LOGGER.debug("Renamed method in subclass " + subclass + ": " + oldName + " -> " + newName);
                            }
                            // Update the mapping for the subclass method
                            String subMethodSignature = subclass + "." + oldName + subMethodNode.desc;
                            globalMethodMappings.put(subMethodSignature, newName);
                        }
                    }
                }
            }
        }
    }

    private String findSuperMethodSignature(String className, String methodName, String methodDesc, Hierarchy hierarchy) {
        List<String> classHierarchy = hierarchy.getHierarchy(className);
        for (String superClass : classHierarchy) {
            if (!superClass.equals(className)) {
                String methodSignature = superClass + "." + methodName + methodDesc;
                if (globalMethodMappings.containsKey(methodSignature)) {
                    return methodSignature;
                }
            }
        }
        return null;
    }

    private void updateMethodReferences(MethodNode methodNode, String currentClassName, Hierarchy hierarchy) {
        boolean isTargetClass = currentClassName.endsWith("PartyEventMenu") || currentClassName.endsWith("Menu");

        for (AbstractInsnNode insnNode : methodNode.instructions) {
            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                String ownerClassName = methodInsnNode.owner.replace('/', '.');
                String newName = findNewMethodNameInHierarchy(ownerClassName, methodInsnNode.name, methodInsnNode.desc, hierarchy);
                if (newName != null) {
                    String oldName = methodInsnNode.name;
                    methodInsnNode.name = newName;
                    if (isTargetClass) {
                    LOGGER.debug("Updated method reference in " + currentClassName + "." + methodNode.name +
                            ": " + methodInsnNode.owner + "." + oldName + " -> " + newName);
                    }
                }
            }
        }
    }

    private String findNewMethodNameInHierarchy(String ownerClassName, String methodName, String methodDesc, Hierarchy hierarchy) {
        List<String> classHierarchy = hierarchy.getHierarchy(ownerClassName);
        for (String className : classHierarchy) {
            String methodSignature = className + "." + methodName + methodDesc;
            String newName = globalMethodMappings.get(methodSignature);
            if (newName != null) {
                return newName;
            }
        }
        return null;
    }

    private void debugHierarchy(Hierarchy hierarchy, String className) {
        LOGGER.debug("Hierarchy for " + className);
        LOGGER.debug("Full Hierarchy " + hierarchy.getHierarchy(className).toString());
        LOGGER.debug("Superclass " + hierarchy.getSuperclass(className));
        LOGGER.debug("Subclasses " + hierarchy.getSubclasses(className).toString());
    }

    private void handleOverriddenMethods(ClassNode classNode, String className, Hierarchy hierarchy) {
        String superClassName = hierarchy.getSuperclass(className);
        if (superClassName != null) {
            ClassNode superClassNode = hierarchy.getClass(superClassName);
            if (superClassNode != null) {
                for (MethodNode methodNode : classNode.methods) {
                    MethodNode superMethod = findMatchingMethod(superClassNode, methodNode);
                    if (superMethod != null) {
                        String superMethodSignature = superClassName + "." + superMethod.name + superMethod.desc;
                        String newName = globalMethodMappings.get(superMethodSignature);
                        if (newName != null) {
                            methodNode.name = newName;
                            preserveOverrideAnnotation(methodNode);
                        }
                    }
                }
            }
        }
    }

    private MethodNode findMatchingMethod(ClassNode classNode, MethodNode methodToMatch) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(methodToMatch.name) && method.desc.equals(methodToMatch.desc)) {
                return method;
            }
        }
        return null;
    }

    private void preserveOverrideAnnotation(MethodNode methodNode) {
        if (methodNode.visibleAnnotations == null) {
            methodNode.visibleAnnotations = new ArrayList<>();
        }

        boolean hasOverrideAnnotation = methodNode.visibleAnnotations.stream()
                .anyMatch(an -> an.desc.equals("Ljava/lang/Override;"));

        if (!hasOverrideAnnotation) {
            methodNode.visibleAnnotations.add(new AnnotationNode("Ljava/lang/Override;"));
        }
    }

    private boolean isExcludedMethod(MethodNode methodNode, Configuration config) {
        Set<String> exclusions = new HashSet<>();

        Object customSetting = config.getCustomSetting("MethodTransformer", "method-exclusions");
        if (customSetting instanceof String) {
            for (String exclusion : ((String) customSetting).split(",")) {
                exclusions.add(exclusion.trim());
            }
        }

        return exclusions.contains(methodNode.name);
    }

    private String generateSecureMethodName(String originalName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest((originalName + System.currentTimeMillis()).getBytes());

            StringBuilder newName = new StringBuilder();
            for (byte b : hashBytes) {
                newName.append(String.format("%02x", b));
            }

            newName.setCharAt(0, (char) ('a' + random.nextInt(26)));

            return newName.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secure method name");
        }
    }
}
