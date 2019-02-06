package com.godxj.plugin.adapter;

import com.godxj.plugin.config.AppBaseInfo;
import com.godxj.plugin.utils.TransformUtils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.AnalyzerAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * 插桩
 */
public class ModifyTimeClassAdapter extends ClassVisitor {

    private String className;
    private String superClassName;
    private String packageName;
    private String proxyTimeClassName;
    private String proxyTimeCreate;//页面创建
    private String proxyTimeStart;//onStart
    private String proxyTimeStop;//onStop
    private String proxyTimeDestroy;//onDestroy
    //方法中间的所有方法
    private List<String> allMethodNames = new ArrayList<>();
    private AppBaseInfo appBaseInfo = AppBaseInfo.getInstance();
    private boolean isFragment;
    private boolean isActivity;

    public ModifyTimeClassAdapter(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
        superClassName = superName;
        packageName = className.replaceAll("/", "\\.");
        isFragment = appBaseInfo.getAllFragments().contains(packageName);
        isActivity = appBaseInfo.getAllActivities().contains(packageName);
        //创建一些activity/fragment统计成员变量
        if (isActivity || isFragment) {
            createPrivateParam();
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        allMethodNames.add(name);
        if (isActivity || isFragment) {
            methodVisitor = new ModifyPageTimeMethodAdapter(methodVisitor, className, access, name, desc);
        }
        return methodVisitor;
    }

    /**
     * 创建用于记录时间的成员变量
     */
    private void createPrivateParam() {
        String startIns = packageName.replaceAll("\\.", "_");
        //创建时间处理代理成员变量
        proxyTimeClassName = startIns + "_timeProxy";
        cv.visitField(ACC_PRIVATE, proxyTimeClassName, "L" + TransformUtils.INJECT_TIME_CLASS_NAME + ";", null, null).visitEnd();
        //创建用于记录时间的成员变量
        proxyTimeCreate = startIns + "_createTime";
        cv.visitField(ACC_PRIVATE, proxyTimeCreate, "J", null, null).visitEnd();
        proxyTimeStart = startIns + "_startTime";
        cv.visitField(ACC_PRIVATE, proxyTimeStart, "J", null, null).visitEnd();
        proxyTimeStop = startIns + "_stopTime";
        cv.visitField(ACC_PRIVATE, proxyTimeStop, "J", null, null).visitEnd();
        proxyTimeDestroy = startIns + "_destroyTime";
        cv.visitField(ACC_PRIVATE, proxyTimeDestroy, "J", null, null).visitEnd();
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        //插桩activity类
        if (isActivity) {
            //插桩时间统计
            //判断onCreate是否存在，不存在重写一个
            if (!allMethodNames.contains("onCreate")) addCreateMethod();

            //判断finish是否存在，不存在重写一个 替代onDestroy
            if (!allMethodNames.contains("finish")) addFinishMethod();

            //判断onResume是否存在，不存在重写一个
            if (!allMethodNames.contains("onResume")) addResumeMethod();

            //判断startActivityForResult是否存在，不存在重写一个onStop
            if (!allMethodNames.contains("startActivityForResult"))
                addStartActivityForResultMethod();

        }
        //插桩fragment
        if (isFragment) {
            //判断onCreate是否存在，不存在重写一个
            if (!allMethodNames.contains("onCreate")) addFragmentCreateMethod();

            //判断onDestroyView是否存在，不存在重写一个
            if (!allMethodNames.contains("onDestroyView")) addDestroyViewMethod();

            //判断onResume是否存在，不存在重写一个
            if (!allMethodNames.contains("onResume")) addResumeMethod();

            //判断startActivityForResult是否存在，不存在重写一个onStop
            if (!allMethodNames.contains("startActivityForResult"))
                addStartActivityForResultMethod();

            //判断onHiddenChanged是否存在，fragment独有的
            if (!allMethodNames.contains("onHiddenChanged"))
                addOnHiddenChangedMethod();

            //判断onHiddenChanged是否存在，fragment独有的
            if (!allMethodNames.contains("setUserVisibleHint"))
                addSetUserVisibleHintMethod();

        }

    }

    /**
     * 一个ViewPage多个fragment显示影藏会被调用
     */
    private void addSetUserVisibleHintMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "setUserVisibleHint", "(Z)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "setUserVisibleHint", "(Z)V", false);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "getUserVisibleHint", "()Z", false);
        Label l2 = new Label();
        mv.visitJumpInsn(IFEQ, l2);
        setTimeTimeByName(mv, proxyTimeStart);
        Label l4 = new Label();
        mv.visitJumpInsn(GOTO, l4);
        mv.visitLabel(l2);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        injectStopLogic(mv);
        mv.visitLabel(l4);
        mv.visitFrame(F_SAME, 0, null, 0, null);

        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /**
     * 一个activity多个fragment显示影藏会被调用
     */
    private void addOnHiddenChangedMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "onHiddenChanged", "(Z)V", null, null);
        mv.visitCode();
        injectOnHiddenLogic(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "onHiddenChanged", "(Z)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 2);
        mv.visitEnd();
    }

    /**
     * fragment hide show 逻辑处理
     *
     * @param mv
     */
    private void injectOnHiddenLogic(MethodVisitor mv) {
        mv.visitVarInsn(ILOAD, 1);
        Label l2 = new Label();
        mv.visitJumpInsn(IFEQ, l2);
        injectStopLogic(mv);
        Label l5 = new Label();
        mv.visitJumpInsn(GOTO, l5);
        mv.visitLabel(l2);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        setTimeTimeByName(mv, proxyTimeStart);
        mv.visitLabel(l5);
        mv.visitFrame(F_SAME, 0, null, 0, null);
    }

    /**
     * 由于onStop在透明主题下是不会被调用的而统计页面停留时间 与页面是否跳转有直接关系
     */
    private void addStartActivityForResultMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "startActivityForResult", "(Landroid/content/Intent;ILandroid/os/Bundle;)V", null, null);
        AnnotationVisitor av0 = mv.visitParameterAnnotation(2, "Landroid/support/annotation/Nullable;", false);
        av0.visitEnd();
        mv.visitCode();
        injectStopLogic(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "startActivityForResult", "(Landroid/content/Intent;ILandroid/os/Bundle;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(9, 4);
        mv.visitEnd();
    }

    /**
     * 插入实际页面停留逻辑
     *
     * @param mv
     */
    private void injectStopLogic(MethodVisitor mv) {
        //记录stop时间
        setTimeTimeByName(mv, proxyTimeStop);
        //回调页面停留
        callbackBuryTime(mv, false);
    }

    /**
     * fragment独有的 让它的逻辑等价于finish
     */
    private void addDestroyViewMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "onDestroyView", "()V", null, null);
        mv.visitCode();
        injectDestroyLogic(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "onDestroyView", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 记录可见时间根据不能重复覆盖
     */
    private void addResumeMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "onResume", "()V", null, null);
        mv.visitCode();
        injectResumeLogic(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "onResume", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();
    }

    /**
     * 实际注入onResume的方法实体
     * if(proxyTimeStart == 0){
     * proxyTimeStart = System.currentTimeMillis();
     * }
     *
     * @param mv
     */
    private void injectResumeLogic(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, proxyTimeStart, "J");
        mv.visitInsn(LCONST_0);
        mv.visitInsn(LCMP);
        Label l1 = new Label();
        mv.visitJumpInsn(IFNE, l1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        mv.visitFieldInsn(PUTFIELD, className, proxyTimeStart, "J");
        mv.visitLabel(l1);
        mv.visitFrame(F_SAME, 0, null, 0, null);
    }

    /**
     * 删除activity堆栈数据
     */
    private void addFinishMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "finish", "()V", null, null);
        mv.visitCode();
        injectDestroyLogic(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "finish", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(9, 3);
        mv.visitEnd();
    }


    /**
     * 添加fragment的onCreate方法
     */
    private void addFragmentCreateMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onCreate", "(Landroid/os/Bundle;)V", null, null);
        mv.visitCode();
        injectOnCreateLogic(mv);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "onCreate", "(Landroid/os/Bundle;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(5, 5);
        mv.visitEnd();
    }

    /**
     * 完整的添加onCreate方法
     */
    private void addCreateMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onCreate", "(Landroid/os/Bundle;)V", null, null);
        mv.visitParameterAnnotation(0, "Landroid/support/annotation/Nullable;", false).visitEnd();
        mv.visitEnd();
        injectOnCreateLogic(mv);
        //调用父类方法
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "onCreate", "(Landroid/os/Bundle;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(5, 3);
        mv.visitEnd();
    }

    /**
     * 注入onCreate实体逻辑
     *
     * @param mv
     */
    private void injectOnCreateLogic(MethodVisitor mv) {
        //给createTime赋值
        setTimeTimeByName(mv, proxyTimeCreate);
        //给stayTime赋值
        initProxyClass(mv);
        if (isActivity) {
            //把当前类名路径记录下来做成简单的堆栈列表
            addToStackProxy(mv);
        }
    }

    /**
     * 注入destroy实体逻辑 跟create成对
     */
    private void injectDestroyLogic(MethodVisitor mv) {
        //记录销毁时间
        setTimeTimeByName(mv, proxyTimeDestroy);
        //执行回调
        callbackBuryTime(mv, true);
        if (isActivity) {
            removeToStackProxy(mv);
        }

    }

    /**
     * 回调用户方法
     * 当isDestroy为true时 a=proxyTimeCreate,b=proxyTimeDestroy; false为a=proxyTimeStart,b=proxyTimeStop
     * if (a > 0L) {
     * timeProxy.buryTime(new BuryTimeInfo(this.getContext(), this, b - a, true, true));
     * a = 0L;
     * }
     */
    private void callbackBuryTime(MethodVisitor mv, boolean isDestroy) {
        String start = isDestroy ? proxyTimeCreate : proxyTimeStart;
        String stop = isDestroy ? proxyTimeDestroy : proxyTimeStop;
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, start, "J");
        mv.visitInsn(LCONST_0);
        mv.visitInsn(LCMP);
        Label l4 = new Label();
        mv.visitJumpInsn(IFLE, l4);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, proxyTimeClassName, "L" + TransformUtils.INJECT_TIME_CLASS_NAME + ";");
        mv.visitTypeInsn(NEW, TransformUtils.TAG_BURY_TIME_INFO);
        mv.visitInsn(DUP);
        if (isActivity) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ACONST_NULL);
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "getContext", "()Landroid/content/Context;", false);
            mv.visitVarInsn(ALOAD, 0);
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, stop, "J");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, start, "J");
        mv.visitInsn(LSUB);
        mv.visitInsn(isActivity ? ICONST_0 : ICONST_1);
        mv.visitInsn(isDestroy ? ICONST_0 : ICONST_1);
        mv.visitMethodInsn(INVOKESPECIAL, TransformUtils.TAG_BURY_TIME_INFO, "<init>", "(Landroid/content/Context;Landroid/support/v4/app/Fragment;JZZ)V", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, TransformUtils.INJECT_TIME_CLASS_NAME, "buryTime", "(L" + TransformUtils.TAG_BURY_TIME_INFO + ";)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(LCONST_0);
        mv.visitFieldInsn(PUTFIELD, className, start, "J");
        mv.visitLabel(l4);
        mv.visitFrame(F_SAME, 0, null, 0, null);
    }

    /**
     * 给时间变量赋值
     * proxyTimeCreate = System.currentTimeMillis()
     *
     * @param mv
     */
    private void setTimeTimeByName(MethodVisitor mv, String proxyTime) {
        //给createTime赋值
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        mv.visitFieldInsn(PUTFIELD, className, proxyTime, "J");
    }

    /**
     * 实例化代理类
     * proxyTime = new ProxyTime()
     *
     * @param mv
     */
    private void initProxyClass(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, TransformUtils.INJECT_TIME_CLASS_NAME);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, TransformUtils.INJECT_TIME_CLASS_NAME, "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, className, proxyTimeClassName, "L" + TransformUtils.INJECT_TIME_CLASS_NAME + ";");
    }

    /**
     * 把当前类名路径记录下来做成简单的堆栈列表
     * AbstractTimeProxy.getStackActivities().add(this.getClass().getName());
     *
     * @param mv
     */
    private void addToStackProxy(MethodVisitor mv) {
        if (isActivity) {
            mv.visitMethodInsn(INVOKESTATIC, TransformUtils.TAG_TIME_PROXY, "getStackActivities", "()Ljava/util/List;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
        }
    }

    /**
     * 把当前类名路径记录下来做成简单的堆栈列表
     * AbstractTimeProxy.getStackActivities().remove(this.getClass().getName());
     *
     * @param mv
     */
    private void removeToStackProxy(MethodVisitor mv) {
        if (isActivity) {
            mv.visitMethodInsn(INVOKESTATIC, TransformUtils.TAG_TIME_PROXY, "getStackActivities", "()Ljava/util/List;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
        }
    }

    /**
     * 打印辅助方法
     *
     * @param mv
     */
    private void printlnInfo(MethodVisitor mv) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn("\u6d4b\u8bd5onCreate:");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    /**
     * 方法插桩(页面停留时间统计)
     */
    private class ModifyPageTimeMethodAdapter extends AdviceAdapter {

        private String methodName;
        private int maxStack;
        private int maxLocal;
        private AnalyzerAdapter analyzerAdapter;

        public ModifyPageTimeMethodAdapter(MethodVisitor mv, String owner, int access, String name, String desc) {
            super(ASM5, mv, access, name, desc);
            this.methodName = name;
            analyzerAdapter = new AnalyzerAdapter(owner, access, name, desc, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
        }

        @Override
        protected void onMethodEnter() {
            super.onMethodEnter();
            if ("onCreate".equals(methodName)) {//如果onCreate方法存在就添加一下数据
                //实例化时间代理类
                injectOnCreateLogic(mv);
            } else if ("finish".equals(methodName) && isActivity) {
                injectDestroyLogic(mv);
            } else if ("onResume".equals(methodName)) {
                injectResumeLogic(mv);
            } else if ("onDestroyView".equals(methodName) && isFragment) {
                injectDestroyLogic(mv);
            } else if ("startActivityForResult".equals(methodName)) {
                injectStopLogic(mv);
            } else if ("onHiddenChanged".equals(methodName)) {
                injectStopLogic(mv);
            }
            maxStack = Math.max(analyzerAdapter.stack.size() + 3, maxStack);
            maxLocal = Math.max(analyzerAdapter.locals.size() + 3, maxLocal);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(Math.max(maxStack, this.maxStack), Math.max(maxLocals, maxLocal));
        }
    }
}
