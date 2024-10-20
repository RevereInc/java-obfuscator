package dev.revere.obfuscator.transformer.transformers;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.AbstractTransformer;
import dev.revere.obfuscator.transformer.context.TransformerContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class FieldTransformer extends AbstractTransformer {
    private static final Logger LOGGER = Logger.getLogger(FieldTransformer.class.getName());
    private final Random random = new Random();

    public FieldTransformer() {
        super("FieldTransformer");
    }

    @Override
    public Map<String, ClassNode> transform(Map<String, ClassNode> classNodes, Configuration config, TransformerContext context) throws ObfuscationException {
        try {
            Map<String, ClassNode> transformedNodes = new HashMap<>(classNodes);
            Map<String, Map<String, String>> fieldMappings = new HashMap<>();

            // First pass: Rename fields and collect mappings
            for (Map.Entry<String, ClassNode> entry : transformedNodes.entrySet()) {
                String className = entry.getKey().replace('/', '.').replace(".class", "");
                ClassNode classNode = entry.getValue();

                if (shouldTransform(className, config)) {
                    Map<String, String> classMappings = new HashMap<>();
                    for (FieldNode fieldNode : classNode.fields) {
                        if (context.isFieldProtected(className, fieldNode.name)) {
                            continue;
                        }

                        if ((fieldNode.access & Opcodes.ACC_SYNTHETIC) != 0 || (fieldNode.access & Opcodes.ACC_ENUM) != 0) {
                            continue;
                        }

                        String newName = generateSecureFieldName(fieldNode.name);
                        classMappings.put(fieldNode.name + fieldNode.desc, newName);
                        fieldNode.name = newName;
                    }
                    if (!classMappings.isEmpty()) {
                        fieldMappings.put(className, classMappings);
                    }
                }
            }

            // Second pass: Update field references in all classes
            for (ClassNode classNode : transformedNodes.values()) {
                for (MethodNode methodNode : classNode.methods) {
                    for (AbstractInsnNode insnNode : methodNode.instructions) {
                        if (insnNode instanceof FieldInsnNode) {
                            FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                            String ownerClassName = fieldInsnNode.owner.replace('/', '.');
                            Map<String, String> classMappings = fieldMappings.get(ownerClassName);
                            if (classMappings != null) {
                                String key = fieldInsnNode.name + fieldInsnNode.desc;
                                String newName = classMappings.get(key);
                                if (newName != null) {
                                    fieldInsnNode.name = newName;
                                }
                            }
                        }
                    }
                }
            }

            return transformedNodes;
        } catch (Exception e) {
            throw new ObfuscationException("Failed to transform fields: " + e.getMessage());
        }
    }

    private String generateSecureFieldName(String originalName) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest((originalName + System.nanoTime() + random.nextInt()).getBytes());

            StringBuilder newName = new StringBuilder("_");
            for (byte b : hashBytes) {
                newName.append(String.format("%02x", b));
            }

            newName.setCharAt(0, ( char) ('a' + random.nextInt(26)));

            return newName.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}