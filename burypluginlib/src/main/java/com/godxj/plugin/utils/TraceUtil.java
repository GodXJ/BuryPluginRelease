package com.godxj.plugin.utils;

public class TraceUtil {

    /**
     * 页面停留时间
     */
    public static void stayTime(long startTime, long stopTime) {
        System.out.println("页面停留时间：" + (stopTime - startTime));
    }

    /**
     * 页面存活时间
     */
    public static void surviveTime(long createTime, long destroyTime) {
        System.out.println("页面存活时间：" + (destroyTime - createTime));
    }
}
