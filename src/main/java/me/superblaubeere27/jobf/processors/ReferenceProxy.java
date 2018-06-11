package me.superblaubeere27.jobf.processors;

import me.superblaubeere27.jobf.IClassProcessor;
import me.superblaubeere27.jobf.JObfImpl;
import me.superblaubeere27.jobf.utils.NameUtils;
import me.superblaubeere27.jobf.utils.NodeUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class ReferenceProxy implements IClassProcessor {
    private static Random random = new Random();
    private JObfImpl inst;

    public ReferenceProxy(JObfImpl inst) {
        this.inst = inst;
    }

    @Override
    public void process(ClassNode node, int mode) {
        try {
            HashMap<String, MethodNode> nodes = new HashMap<>();
            List<MethodNode> add = new ArrayList<>();

            for (MethodNode method : node.methods) {
                for (AbstractInsnNode abstractInsnNode : method.instructions.toArray()) {
                    if (abstractInsnNode instanceof MethodInsnNode) {
                        MethodInsnNode insnNode = (MethodInsnNode) abstractInsnNode;

//                        System.out.println(insnNode.getOpcode() + "/" + insnNode.name + insnNode.desc);

                        if (
//                                insnNode.desc.endsWith(")V") &&
                                (insnNode.getOpcode() == Opcodes.INVOKESTATIC || insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) && !(insnNode.desc.contains("J") || insnNode.desc.contains("D") || insnNode.desc.contains("[") || insnNode.desc.contains("Object"))) {
                            MethodNode referenceProxy = getProxyNode(node, insnNode, insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL);

                            method.instructions.insert(insnNode, new MethodInsnNode(Opcodes.INVOKESTATIC, node.name, referenceProxy.name, referenceProxy.desc, false));
                            method.instructions.remove(insnNode);

                            add.add(referenceProxy);
                            System.out.println("PROXY");
                        }
                    }
                }
            }

            for (MethodNode method : add) {
                System.out.println(method);
                node.methods.add(method);
            }

//        for (Map.Entry<String, MethodNode> stringMethodNodeEntry : nodes.entrySet()) {
//            node.methods.add(stringMethodNodeEntry.getValue());
//        }
            inst.setWorkDone();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MethodNode getProxyNode(ClassNode node, MethodInsnNode insnNode, boolean b) {
        String name = NameUtils.generateMethodName(node, insnNode.desc);
        MethodNode mv;

        Type[] argumentTypes = Type.getArgumentTypes(insnNode.desc);
        Type returnType = Type.getReturnType(insnNode.desc);

        int slot1 = argumentTypes.length + (b ? 1 : 0);
        int slot2 = argumentTypes.length + (b ? 1 : 0) + 1;
        int slot3 = argumentTypes.length + (b ? 1 : 0) + 2;
        int slot4 = argumentTypes.length + (b ? 1 : 0) + 3;


        //MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, name, insnNode.desc, null, new String[0]);
        {
            mv = new MethodNode(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, name, b ? "(Ljava/lang/Object;" + insnNode.desc.substring(1) : insnNode.desc, null, null);


            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
            mv.visitLabel(l0);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitVarInsn(Opcodes.ASTORE, slot3);
//            mv.visitLineNumber(14, l0);
            mv.visitLdcInsn(Type.getType("L" + insnNode.owner + ";"));
            mv.visitLdcInsn(insnNode.name);
            //  --package --packagerMainClass tk.ccbluex.CMDControl.CMDControl

            mv.instructions.add(NodeUtils.generateIntPush(argumentTypes.length));
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
            mv.visitVarInsn(Opcodes.ASTORE, slot1);

            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitVarInsn(Opcodes.ALOAD, slot1);
                mv.instructions.add(NodeUtils.generateIntPush(i));
                mv.instructions.add(NodeUtils.getTypeNode(argumentTypes[i]));
                mv.visitInsn(Opcodes.AASTORE);
            }

            mv.visitVarInsn(Opcodes.ALOAD, slot1);

            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
            mv.visitVarInsn(Opcodes.ASTORE, slot1);
            Label l3 = new Label();
            mv.visitLabel(l3);
//            mv.visitLineNumber(15, l3);
            mv.visitVarInsn(Opcodes.ALOAD, slot1);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V", false);
            Label l4 = new Label();
            mv.visitLabel(l4);
//            mv.visitLineNumber(16, l4);
            mv.instructions.add(NodeUtils.generateIntPush(argumentTypes.length));
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitVarInsn(Opcodes.ASTORE, slot2);
            Label l5 = new Label();
            mv.visitLabel(l5);
//            mv.visitLineNumber(17, l5);

            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitVarInsn(Opcodes.ALOAD, slot2);
                mv.instructions.add(NodeUtils.generateIntPush(i));
                mv.visitVarInsn(argumentTypes[i].getOpcode(Opcodes.ILOAD), i + 1);
                mv.instructions.add(NodeUtils.getWrapperMethod(argumentTypes[i]));
                mv.visitInsn(Opcodes.AASTORE);
            }

            Label l6 = new Label();
            mv.visitLabel(l6);
//            mv.visitLineNumber(18, l6);
            mv.visitVarInsn(Opcodes.ALOAD, slot1);

            if (!b) mv.visitInsn(Opcodes.ACONST_NULL);
            else mv.visitVarInsn(Opcodes.ALOAD, 0);

            mv.visitVarInsn(Opcodes.ALOAD, slot2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);

            if (returnType.toString().equals("V")) {
                mv.visitInsn(Opcodes.POP);
            } else {
                mv.visitVarInsn(Opcodes.ASTORE, slot3);
            }

            mv.visitLabel(l1);
//            mv.visitLineNumber(21, l1);
            Label l7 = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, l7);
            mv.visitLabel(l2);
//            mv.visitLineNumber(19, l2);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Exception"});
            mv.visitVarInsn(Opcodes.ASTORE, slot1);
            Label l8 = new Label();
            mv.visitLabel(l8);
//            mv.visitLineNumber(20, l8);
            mv.visitVarInsn(Opcodes.ALOAD, slot1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
            mv.visitLabel(l7);
//            mv.visitLineNumber(22, l7);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

            if (returnType.toString().equals("V")) {
                mv.visitInsn(Opcodes.RETURN);
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, slot3);

                if (!returnType.toString().startsWith("L")) {
                    mv.instructions.add(NodeUtils.getUnWrapMethod(returnType));
                } else {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, returnType.toString().substring(1, returnType.toString().length() - 1));
                }


                mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
            }

            mv.visitMaxs(6, 3);
            mv.visitEnd();

            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitLocalVariable(NameUtils.generateLocalVariableName(), argumentTypes[i].toString(), null, l0, l7, i);
            }
//            mv.visitLocalVariable(NameUtils.generateLocalVariableName(), argumentTypes[i].toString(), null, l0, l7, i);
        }
        return mv;
    }

}