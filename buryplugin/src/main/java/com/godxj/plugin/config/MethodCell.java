package com.godxj.plugin.config;

import java.util.Arrays;

public class MethodCell {

    //方法参数
    private int[] opcodes;

    //方法名称
    private String methodName;

    //方法参数
    private String methodDesc;

    //需要返回
    private boolean needReturn;

    public MethodCell(int[] opcodes, String methodName, String methodDesc, boolean needReturn) {
        this.opcodes = opcodes;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.needReturn = needReturn;
    }

    public int[] getOpcodes() {
        return opcodes;
    }

    public void setOpcodes(int[] opcodes) {
        this.opcodes = opcodes;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public boolean isNeedReturn() {
        return needReturn;
    }

    public void setNeedReturn(boolean needReturn) {
        this.needReturn = needReturn;
    }

    @Override
    public String toString() {
        return "MethodCell{" +
                "opcodes=" + Arrays.toString(opcodes) +
                ", methodName='" + methodName + '\'' +
                ", methodDesc='" + methodDesc + '\'' +
                ", needReturn=" + needReturn +
                '}';
    }
}
