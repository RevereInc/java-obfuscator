package dev.revere.obfuscator.transformer.transformers;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.logging.Logger;
import dev.revere.obfuscator.transformer.AbstractTransformer;
import dev.revere.obfuscator.transformer.TransformationContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.security.MessageDigest;
import java.util.Map;
import java.util.Random;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class MethodTransformer extends AbstractTransformer {
    private static final Logger LOGGER = Logger.getLogger(MethodTransformer.class.getName());
    private final Random random;

    public MethodTransformer() {
        super("MethodTransformer");
        this.random = new Random();
    }

    @Override
    public void collectInformation(Map<String, byte[]> classes, Configuration config, TransformationContext context) throws ObfuscationException {
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            String className = entry.getKey().replace('/', '.').replace(".class", "");
            ClassReader cr = new ClassReader(entry.getValue());
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            for (MethodNode methodNode : classNode.methods) {
                String newName = generateSecureMethodName(methodNode.name);
                context.addMethodMapping(className, methodNode.name, newName);
            }
        }
    }

    @Override
    protected byte[] doTransform(byte[] classBytes, String className, Configuration config, TransformationContext context, Map<String, byte[]> allClasses, Map<String, byte[]> newClasses) throws ObfuscationException {
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            transformMethods(classNode, context, className);

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(cw);
            return cw.toByteArray();
        } catch (Exception e) {
            throw new ObfuscationException("Failed to transform class " + className);
        }
    }

    private void transformMethods(ClassNode classNode, TransformationContext context, String className) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) {
                continue;
            }

            String newName = context.getNewMethodName(className, methodNode.name);
            if (newName != null) {
                methodNode.name = newName;
            }
        }

        for (MethodNode methodNode : classNode.methods) {
            transformMethodCalls(methodNode, context, className);
        }
    }

    private void transformMethodCalls(MethodNode methodNode, TransformationContext content, String className) {
        for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                String newName = content.getNewMethodName(methodInsnNode.owner.replace('/', '.'), methodInsnNode.name);
                if (newName != null) {
                    methodInsnNode.name = newName;
                }
            }
        }
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
