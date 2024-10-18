package dev.revere.obfuscator.transformer.transformers;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.transformer.AbstractTransformer;
import dev.revere.obfuscator.transformer.TransformationContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class FieldTransformer extends AbstractTransformer {
    private final Random random = new Random();

    public FieldTransformer() {
        super("FieldTransformer");
    }

    @Override
    public void collectInformation(Map<String, byte[]> classes, Configuration config, TransformationContext context) throws ObfuscationException {
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            String className = entry.getKey().replace('/', '.').replace(".class", "");
            ClassReader cr = new ClassReader(entry.getValue());
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            for (FieldNode fieldNode : classNode.fields) {
                String newName = generateSecureFieldName(fieldNode.name);
                context.addFieldMapping(className, fieldNode.name, newName);
            }
        }
    }

    @Override
    protected byte[] doTransform(byte[] classBytes, String className, Configuration config, TransformationContext context, Map<String, byte[]> allClasses, Map<String, byte[]> newClasses) throws ObfuscationException {
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            transformFields(classNode, context, className);

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(cw);
            return cw.toByteArray();
        } catch (Exception e) {
            throw new ObfuscationException("Failed to transform class " + className);
        }
    }

    private void transformFields(ClassNode classNode, TransformationContext context, String className) {
        for (FieldNode fieldNode : classNode.fields) {
            String newName = context.getNewFieldName(className, fieldNode.name);
            if (newName != null) {
                fieldNode.name = newName;
            }
        }

        for (MethodNode methodNode : classNode.methods) {
            transformFieldAccesses(methodNode, context, className);
        }
    }

    private void transformFieldAccesses(MethodNode methodNode, TransformationContext context, String className) {
        for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
            if (insnNode instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                String ownerClassName = fieldInsnNode.owner.replace('/', '.');
                String newName = context.getNewFieldName(ownerClassName, fieldInsnNode.name);
                if (newName != null) {
                    fieldInsnNode.name = newName;
                }
            }
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

            newName.setCharAt(0, (char) ('a' + random.nextInt(26)));

            return newName.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}