package com.godxj.plugin.adapter;

import com.godxj.plugin.utils.TransformUtils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 插桩类
 */
public class InjectClassAdapter extends ClassVisitor {

    private boolean findOnClickProxyClass = false, findTimeProxyClass = false;
    private String onClickProxyClassName, timeProxyClassName;

    public InjectClassAdapter(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        if (TransformUtils.TAG_ONCLICK_PROXY.equals(superName)) {
            findOnClickProxyClass = true;
            onClickProxyClassName = name;
        }else if(TransformUtils.TAG_TIME_PROXY.equals(superName)){
            findTimeProxyClass = true;
            timeProxyClassName = name;
        }
    }

    public String getOnClickProxyClassName() {
        return onClickProxyClassName;
    }

    public boolean isFindOnClickProxyClass() {
        return findOnClickProxyClass;
    }

    public boolean isFindTimeProxyClass() {
        return findTimeProxyClass;
    }

    public String getTimeProxyClassName() {
        return timeProxyClassName;
    }
}
