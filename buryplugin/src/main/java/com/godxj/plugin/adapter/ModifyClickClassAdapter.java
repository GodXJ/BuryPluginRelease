package com.godxj.plugin.adapter;

import com.godxj.plugin.config.AppBaseInfo;
import com.godxj.plugin.config.MethodCell;
import com.godxj.plugin.utils.TransformUtils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.AnalyzerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ModifyClickClassAdapter extends ClassVisitor {

    private static final String TAG = ModifyPageClickMethodAdapter.class.getSimpleName();
    private AppBaseInfo appBaseInfo = AppBaseInfo.getInstance();
    private String packageName;
    private boolean needInject;
    private List<MethodCell> methodNames;

    public ModifyClickClassAdapter(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        boolean needInject = false;
        packageName = name.replaceAll("/", "\\.");
        methodNames = new ArrayList<>();
        if (interfaces != null && interfaces.length > 0
                && !TransformUtils.INJECT_ONCLICK_CLASS_NAME.equals(packageName)
                && !TransformUtils.TAG_ONCLICK_PROXY.equals(name)) {
            for (String interfaceStr : interfaces) {
                //如果接口方法
                MethodCell cell = appBaseInfo.getClickInterfaceMethods().get(interfaceStr);
                if (cell != null) {
                    methodNames.add(cell);
                    needInject = true;
                    continue;
                }
            }
        }
        this.needInject = needInject;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (!needInject) return mv;
        else return new ModifyPageClickMethodAdapter(mv, name, access, name, desc);
    }

    /**
     * 方法插桩(页面停留时间统计)
     */
    private class ModifyPageClickMethodAdapter extends AdviceAdapter {

        private String methodName;
        private int maxStack;
        private int maxLocal;
        private AnalyzerAdapter analyzerAdapter;
        private String methodDesc;

        protected ModifyPageClickMethodAdapter(MethodVisitor mv, String owner, int access, String name, String desc) {
            super(ASM5, mv, access, name, desc);
            this.methodName = name;
            this.methodDesc = desc;
            analyzerAdapter = new AnalyzerAdapter(owner, access, name, desc, mv);
        }

        @Override
        protected void onMethodEnter() {
            super.onMethodEnter();
            //是否需要插桩
            for (MethodCell cell : appBaseInfo.getClickInterfaceMethods().values()) {
                if (cell.getMethodName().equals(methodName) && cell.getMethodDesc().equals(methodDesc)) {
                    System.out.println(TAG + "->" + cell);
                    injectMethod(cell);
                    break;
                }
            }
            maxStack = Math.max(analyzerAdapter.stack.size() + 3, maxStack);
            maxLocal = Math.max(analyzerAdapter.locals.size() + 3, maxLocal);
        }

        /**
         * 实际插桩
         */
        private void injectMethod(MethodCell cell) {
            mv.visitTypeInsn(NEW, TransformUtils.INJECT_ONCLICK_CLASS_NAME);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, TransformUtils.INJECT_ONCLICK_CLASS_NAME, "<init>", "()V", false);
            //参数列表
            for (int i = 0; i < cell.getOpcodes().length; i++) {
                mv.visitVarInsn(cell.getOpcodes()[i], i + 1);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, TransformUtils.INJECT_ONCLICK_CLASS_NAME, methodName, methodDesc, false);
            if (cell.isNeedReturn()){
                mv.visitInsn(POP);
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(Math.max(maxStack, this.maxStack), Math.max(maxLocals, maxLocal));
        }
    }
}
