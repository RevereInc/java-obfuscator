package dev.revere.obfuscator.transformer.transformers;

import dev.revere.obfuscator.config.Configuration;
import dev.revere.obfuscator.exception.ObfuscationException;
import dev.revere.obfuscator.transformer.AbstractTransformer;
import dev.revere.obfuscator.transformer.context.TransformerContext;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Remi
 * @project revere-java-obfuscator
 * @date 10/18/2024
 */
public class StringTransformer extends AbstractTransformer {
    private static final String DECRYPT_METHOD_NAME = "IIIIIiiiIIIIiIIIiIIIiiiiiiiIiIIIIIIIIIIIIIIIIiiiIIIIiIIIiIIIiiiiiiiIiIIIIIIIIIIIIIIIIiiiIIIIiIIIiIIIiiiiiiiIiIIIIIIIIIIIIIIIIiiiIIIIiIIIiIIIiiiiiiiIiIIIIIIIIIII";
    private static final String DECRYPT_METHOD_DESC = "(Ljava/lang/String;I)Ljava/lang/String;";
    private static final String DECRYPT_HELPER_CLASS = "org/bson/IIIIIiiiIIIIiIIIiIIIiiiiiiiIiIIIIIIIIIII";
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
    public Map<String, ClassNode> transform(Map<String, ClassNode> classNodes, Configuration config, TransformerContext context) throws ObfuscationException {
        Map<String, ClassNode> transformedNodes = new HashMap<>(classNodes);
        if (!decryptorAdded) {
            addStringDecryptorClass(transformedNodes);
            decryptorAdded = true;
        }

        for (Map.Entry<String, ClassNode> entry : transformedNodes.entrySet()) {
            String className = entry.getKey().replace('/', '.').replace(".class", "");
            ClassNode classNode = entry.getValue();

            if (shouldTransform(className, config)) {
                for (MethodNode methodNode : classNode.methods) {
                    transformMethod(methodNode);
                }
            }
        }

        return transformedNodes;
    }

    private void addStringDecryptorClass(Map<String, ClassNode> classNodes) {
        ClassNode decryptorNode = new ClassNode();
        decryptorNode.version = Opcodes.V1_8;
        decryptorNode.access = Opcodes.ACC_PUBLIC;
        decryptorNode.name = DECRYPT_HELPER_CLASS;
        decryptorNode.superName = "java/lang/Object";

        // Add constructor
        MethodVisitor mv = decryptorNode.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Add MAGIC field
        FieldVisitor fv = decryptorNode.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "MAGIC", "[J", null, null);
        fv.visitEnd();

        // Add static initializer for MAGIC
        mv = decryptorNode.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
        mv.visitLabel(l0);
        mv.visitIntInsn(Opcodes.BIPUSH, 3);
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
        mv.visitLabel(l1);
        Label l3 = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, l3);
        mv.visitLabel(l2);
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitLabel(l3);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(6, 1);
        mv.visitEnd();

        // Add decryption method
        MethodVisitor decryptor = decryptorNode.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                DECRYPT_METHOD_NAME,
                DECRYPT_METHOD_DESC,
                null,
                null
        );
        decryptor.visitCode();

        decryptor.visitInsn(Opcodes.ACONST_NULL);
        decryptor.visitVarInsn(Opcodes.ASTORE, 2); // byte[] byArray
        decryptor.visitInsn(Opcodes.ACONST_NULL);
        decryptor.visitVarInsn(Opcodes.ASTORE, 3); // byte[] byArray2
        decryptor.visitInsn(Opcodes.ACONST_NULL);
        decryptor.visitVarInsn(Opcodes.ASTORE, 4); // long[] lArray
        decryptor.visitInsn(Opcodes.ICONST_0);
        decryptor.visitVarInsn(Opcodes.ISTORE, 5); // int i
        decryptor.visitInsn(Opcodes.ICONST_0);
        decryptor.visitVarInsn(Opcodes.ISTORE, 6); // int n2
        decryptor.visitLdcInsn(0L);
        decryptor.visitVarInsn(Opcodes.LSTORE, 7); // long l
        decryptor.visitInsn(Opcodes.ICONST_0);
        decryptor.visitVarInsn(Opcodes.ISTORE, 9); // int n3
        decryptor.visitInsn(Opcodes.ICONST_0);
        decryptor.visitVarInsn(Opcodes.ISTORE, 10); // int n4
        decryptor.visitInsn(Opcodes.ICONST_0);
        decryptor.visitVarInsn(Opcodes.ISTORE, 11); // int junk1
        decryptor.visitInsn(Opcodes.ICONST_0);
        decryptor.visitVarInsn(Opcodes.ISTORE, 12); // int junk2

        Label junkLabel1 = new Label();
        Label junkLabel2 = new Label();
        decryptor.visitLdcInsn("IIIIIiiiIIIIiIIIiIII");
        decryptor.visitVarInsn(Opcodes.ASTORE, 3);
        decryptor.visitVarInsn(Opcodes.ALOAD, 3);
        decryptor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        decryptor.visitJumpInsn(Opcodes.IFLE, junkLabel1);
        decryptor.visitLdcInsn("iIIiiiiIIIIIiiIIIIII");
        decryptor.visitVarInsn(Opcodes.ASTORE, 3);
        decryptor.visitJumpInsn(Opcodes.GOTO, junkLabel2);
        decryptor.visitLabel(junkLabel1);
        decryptor.visitLdcInsn("iIiIIIiiiiiiiiiIIIIIIII");
        decryptor.visitVarInsn(Opcodes.ASTORE, 3);
        decryptor.visitLabel(junkLabel2);

        Label defaultLabel = new Label();
        Label[] caseLabels = new Label[3];
        for (int i = 0; i < caseLabels.length; i++) {
            caseLabels[i] = new Label();
        }

        decryptor.visitVarInsn(Opcodes.ALOAD, 0);
        decryptor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        decryptor.visitInsn(Opcodes.ICONST_3);
        decryptor.visitInsn(Opcodes.IREM);
        decryptor.visitTableSwitchInsn(0, 2, defaultLabel, caseLabels);

        // Case 1
        decryptor.visitLabel(caseLabels[0]);
        decryptor.visitLdcInsn("U2FsdGVkX1");
        decryptor.visitVarInsn(Opcodes.ASTORE, 3);
        decryptor.visitJumpInsn(Opcodes.GOTO, defaultLabel);

        // Case 2
        decryptor.visitLabel(caseLabels[1]);
        decryptor.visitLdcInsn("9JaWxsaW5n");
        decryptor.visitVarInsn(Opcodes.ASTORE, 3);
        decryptor.visitJumpInsn(Opcodes.GOTO, defaultLabel);

        // Case 3
        decryptor.visitLabel(caseLabels[2]);
        decryptor.visitLdcInsn("X0NyeXB0bw==");
        decryptor.visitVarInsn(Opcodes.ASTORE, 3);
        decryptor.visitJumpInsn(Opcodes.GOTO, defaultLabel);

        decryptor.visitLabel(defaultLabel);

        decryptor.visitVarInsn(Opcodes.ALOAD, 3);
        decryptor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        decryptor.visitVarInsn(Opcodes.ILOAD, 1);
        decryptor.visitInsn(Opcodes.IADD);
        decryptor.visitIntInsn(Opcodes.BIPUSH, 16);
        decryptor.visitInsn(Opcodes.IREM);
        decryptor.visitVarInsn(Opcodes.ISTORE, 4);

        // byte[] byArray = Base64.getDecoder().decode(string);
        decryptor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;", false);
        decryptor.visitVarInsn(Opcodes.ALOAD, 0);
        decryptor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B", false);
        decryptor.visitVarInsn(Opcodes.ASTORE, 2);

        // byte[] byArray2 = new byte[byArray.length];
        decryptor.visitVarInsn(Opcodes.ALOAD, 2);
        decryptor.visitInsn(Opcodes.ARRAYLENGTH);
        decryptor.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        decryptor.visitVarInsn(Opcodes.ASTORE, 3);

        // long[] lArray = MAGIC;
        decryptor.visitFieldInsn(Opcodes.GETSTATIC, DECRYPT_HELPER_CLASS, "MAGIC", "[J");
        decryptor.visitVarInsn(Opcodes.ASTORE, 4);

        // Loop through byArray and decrypt
        Label loopStart = new Label();
        Label loopEnd = new Label();
        decryptor.visitInsn(Opcodes.ICONST_0);
        decryptor.visitVarInsn(Opcodes.ISTORE, 5);
        decryptor.visitLabel(loopStart);
        decryptor.visitVarInsn(Opcodes.ILOAD, 5);
        decryptor.visitVarInsn(Opcodes.ALOAD, 2);
        decryptor.visitInsn(Opcodes.ARRAYLENGTH);
        decryptor.visitJumpInsn(Opcodes.IF_ICMPGE, loopEnd);

        // Add junk calculations
        decryptor.visitVarInsn(Opcodes.ILOAD, 5);
        decryptor.visitIntInsn(Opcodes.BIPUSH, 7);
        decryptor.visitInsn(Opcodes.IREM);
        decryptor.visitVarInsn(Opcodes.ISTORE, 11);

        decryptor.visitVarInsn(Opcodes.ILOAD, 11);
        Label[] junkLabels = new Label[7];
        for (int i = 0; i < junkLabels.length; i++) {
            junkLabels[i] = new Label();
        }

        Label junkDefault = new Label();
        decryptor.visitTableSwitchInsn(0, 6, junkDefault, junkLabels);

        for (int i = 0; i < junkLabels.length; i++) {
            decryptor.visitLabel(junkLabels[i]);
            decryptor.visitIntInsn(Opcodes.BIPUSH, 10 + i);
            decryptor.visitVarInsn(Opcodes.ISTORE, 12);
            decryptor.visitJumpInsn(Opcodes.GOTO, junkDefault);
        }
        decryptor.visitLabel(junkDefault);

        Label junkLabel3 = new Label();
        Label junkLabel4 = new Label();
        decryptor.visitVarInsn(Opcodes.ILOAD, 11);
        decryptor.visitVarInsn(Opcodes.ILOAD, 12);
        decryptor.visitInsn(Opcodes.IXOR);
        decryptor.visitIntInsn(Opcodes.BIPUSH, 2);
        decryptor.visitInsn(Opcodes.IREM);
        decryptor.visitJumpInsn(Opcodes.IFEQ, junkLabel3);
        decryptor.visitVarInsn(Opcodes.LLOAD, 7);
        decryptor.visitLdcInsn(0xA5A5A5A5A5A5A5A5L);
        decryptor.visitInsn(Opcodes.LXOR);
        decryptor.visitVarInsn(Opcodes.LSTORE, 7);
        decryptor.visitJumpInsn(Opcodes.GOTO, junkLabel4);
        decryptor.visitLabel(junkLabel3);
        decryptor.visitVarInsn(Opcodes.ILOAD, 12);
        decryptor.visitInsn(Opcodes.I2F);
        decryptor.visitLdcInsn(0.1f);
        decryptor.visitInsn(Opcodes.FADD);
        decryptor.visitInsn(Opcodes.F2I);
        decryptor.visitVarInsn(Opcodes.ISTORE, 12);
        decryptor.visitLabel(junkLabel4);

        // int n2 = i % lArray.length;
        decryptor.visitVarInsn(Opcodes.ILOAD, 5);
        decryptor.visitVarInsn(Opcodes.ALOAD, 4);
        decryptor.visitInsn(Opcodes.ARRAYLENGTH);
        decryptor.visitInsn(Opcodes.IREM);
        decryptor.visitVarInsn(Opcodes.ISTORE, 6);

        // long l = lArray[n2];
        decryptor.visitVarInsn(Opcodes.ALOAD, 4);
        decryptor.visitVarInsn(Opcodes.ILOAD, 6);
        decryptor.visitInsn(Opcodes.LALOAD);
        decryptor.visitVarInsn(Opcodes.LSTORE, 7);

        // int n3 = (i % 4) * 8;
        decryptor.visitVarInsn(Opcodes.ILOAD, 5);
        decryptor.visitIntInsn(Opcodes.BIPUSH, 4);
        decryptor.visitInsn(Opcodes.IREM);
        decryptor.visitIntInsn(Opcodes.BIPUSH, 8);
        decryptor.visitInsn(Opcodes.IMUL);
        decryptor.visitVarInsn(Opcodes.ISTORE, 9);

        // int n4 = (l >>> n3) & 0xFF;
        decryptor.visitVarInsn(Opcodes.LLOAD, 7);
        decryptor.visitVarInsn(Opcodes.ILOAD, 9);
        decryptor.visitInsn(Opcodes.LUSHR);
        decryptor.visitLdcInsn(0xFFL);
        decryptor.visitInsn(Opcodes.LAND);
        decryptor.visitInsn(Opcodes.L2I);
        decryptor.visitVarInsn(Opcodes.ISTORE, 10);

        // byArray2[i] = (byte)(byArray[i] ^ key ^ n4 ^ (i & 0xFF));
        decryptor.visitVarInsn(Opcodes.ALOAD, 3);
        decryptor.visitVarInsn(Opcodes.ILOAD, 5);
        decryptor.visitVarInsn(Opcodes.ALOAD, 2);
        decryptor.visitVarInsn(Opcodes.ILOAD, 5);
        decryptor.visitInsn(Opcodes.BALOAD);
        decryptor.visitVarInsn(Opcodes.ILOAD, 1);  // key
        decryptor.visitInsn(Opcodes.IXOR);
        decryptor.visitVarInsn(Opcodes.ILOAD, 10);  // magicByte
        decryptor.visitInsn(Opcodes.IXOR);
        decryptor.visitVarInsn(Opcodes.ILOAD, 5);
        decryptor.visitLdcInsn(0xFF);
        decryptor.visitInsn(Opcodes.IAND);
        decryptor.visitInsn(Opcodes.IXOR);
        decryptor.visitInsn(Opcodes.I2B);
        decryptor.visitInsn(Opcodes.BASTORE);

        // Increment loop variable
        decryptor.visitIincInsn(5, 1);
        decryptor.visitJumpInsn(Opcodes.GOTO, loopStart);
        decryptor.visitLabel(loopEnd);

        // Return new String(byArray2, StandardCharsets.UTF_8)
        decryptor.visitTypeInsn(Opcodes.NEW, "java/lang/String");
        decryptor.visitInsn(Opcodes.DUP);
        decryptor.visitVarInsn(Opcodes.ALOAD, 3);
        decryptor.visitFieldInsn(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
        decryptor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        decryptor.visitInsn(Opcodes.ARETURN);

        decryptor.visitMaxs(6, 11);
        decryptor.visitEnd();
        decryptorNode.visitEnd();

        classNodes.put(DECRYPT_HELPER_CLASS + ".class", decryptorNode);
    }

    private void transformMethod(MethodNode methodNode) {
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
                    }
                }
            }
        }
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