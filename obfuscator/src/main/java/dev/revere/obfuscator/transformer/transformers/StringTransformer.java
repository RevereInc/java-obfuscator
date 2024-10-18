package dev.revere.obfuscator.transformer.transformers;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.transformer.AbstractTransformer;
import dev.revere.obfuscator.transformer.TransformationContext;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class StringTransformer extends AbstractTransformer {
    private static final String DECRYPT_METHOD_NAME = "sseeeeeeeeeeeeeeeeeeeeeexxxxxxxxxxx";
    private static final String DECRYPT_METHOD_DESC = "(Ljava/lang/String;I)Ljava/lang/String;";
    private static final String DECRYPT_HELPER_CLASS = "dev/revere/nigger/ILoveSex";
    private final Random random = new Random();
    private final int[] keys;
    private boolean decryptorAdded = false;

    public StringTransformer() {
        super("StringTransformer");
        this.keys = new int[16];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = random.nextInt();
        }
    }

    @Override
    protected byte[] doTransform(byte[] classBytes, String className, Configuration config, TransformationContext context, Map<String, byte[]> allClasses, Map<String, byte[]> newClasses) throws ObfuscationException {
        if (!decryptorAdded) {
            addStringDecryptorClass(allClasses);
            decryptorAdded = true;
        }

        ClassReader cr = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        boolean modified = false;

        for (MethodNode methodNode : classNode.methods) {
            modified |= transformMethod(methodNode, classNode);
        }

        if (modified) {
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                @Override
                protected ClassLoader getClassLoader() {
                    return context.getJarProcessor().getClassLoader();
                }
            };

            classNode.accept(cw);
            return cw.toByteArray();
        }

        return null;
    }

    private void addStringDecryptorClass(Map<String, byte[]> allClasses) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, DECRYPT_HELPER_CLASS, null, "java/lang/Object", null);

        // Add constructor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Add MAGIC field
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "MAGIC", "[J", null, null);
        fv.visitEnd();

        // Add static initializer for MAGIC
        mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.ICONST_3);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitLdcInsn(0xDEADBEEFL);
        mv.visitInsn(Opcodes.LASTORE);
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitLdcInsn(0xCAFEBABEL);
        mv.visitInsn(Opcodes.LASTORE);
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_2);
        mv.visitLdcInsn(0xFACEFEEDL);
        mv.visitInsn(Opcodes.LASTORE);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, DECRYPT_HELPER_CLASS, "MAGIC", "[J");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 0);
        mv.visitEnd();

        // Add decrypt method
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, DECRYPT_METHOD_NAME, DECRYPT_METHOD_DESC, null, null);
        mv.visitCode();

        // Decode Base64
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        // Create StringBuilder
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        Label loopStart = new Label();
        Label loopEnd = new Label();

        // Loop through bytes
        mv.visitLabel(loopStart);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitInsn(Opcodes.IF_ICMPGE);
        mv.visitJumpInsn(Opcodes.GOTO, loopEnd);

        // Decrypt byte
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitInsn(Opcodes.BALOAD);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitInsn(Opcodes.IREM);
        mv.visitFieldInsn(Opcodes.GETSTATIC, DECRYPT_HELPER_CLASS, "MAGIC", "[J");
        mv.visitInsn(Opcodes.AALOAD);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitInsn(Opcodes.IREM);
        mv.visitInsn(Opcodes.ICONST_4);
        mv.visitInsn(Opcodes.IMUL);
        mv.visitInsn(Opcodes.ISHL);
        mv.visitInsn(Opcodes.L2I);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitVarInsn(Opcodes.ILOAD, 0);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitInsn(Opcodes.IAND);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitInsn(Opcodes.I2B);
        mv.visitVarInsn(Opcodes.ASTORE, 4);

        // Append to StringBuilder
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", false);
        mv.visitInsn(Opcodes.POP);

        // Increment loop counter
        mv.visitIincInsn(1, 1);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);

        // Return decrypted string
        mv.visitLabel(loopEnd);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(5, 5);
        mv.visitEnd();

        cw.visitEnd();
        allClasses.put(DECRYPT_HELPER_CLASS + ".class", cw.toByteArray());
    }

    private boolean transformMethod(MethodNode methodNode, ClassNode classNode) {
        boolean modified = false;
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldcInsn = (LdcInsnNode) insn;
                if (ldcInsn.cst instanceof String) {
                    String originalString = (String) ldcInsn.cst;
                    if (originalString.length() > 3) {
                        int key = random.nextInt();
                        String encryptedString = encrypt(originalString, key);

                        InsnList newInstructions = new InsnList();
                        newInstructions.add(new LdcInsnNode(encryptedString));
                        newInstructions.add(new LdcInsnNode(key));
                        newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DECRYPT_HELPER_CLASS, DECRYPT_METHOD_NAME, DECRYPT_METHOD_DESC, false));

                        methodNode.instructions.insert(ldcInsn, newInstructions);
                        methodNode.instructions.remove(ldcInsn);
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }

    private String encrypt(String input, int key) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = new byte[bytes.length];
        long[] magic = {0xDEADBEEFL, 0xCAFEBABEL, 0xFACEFEEDL};

        for (int i = 0; i < bytes.length; i++) {
            int magicIndex = i % magic.length;
            long magicValue = magic[magicIndex];
            int shift = (i % 4) * 8;
            int magicByte = (int) ((magicValue >>> shift) & 0xFF);
            encrypted[i] = (byte) (bytes[i] ^ key ^ magicByte ^ (i & 0xFF));
        }
        return Base64.getEncoder().encodeToString(encrypted);
    }
}