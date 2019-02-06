package com.godxj.plugin.imp;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;

import com.godxj.plugin.BuryTimeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面停留时间代理类
 */
public abstract class AbstractTimeProxy {

    /**
     * 堆栈activity列表
     * 暂时不考虑多个Stack情况
     */
    private static final List<String> stackActivities = new ArrayList();

    /**
     * activity所有的 显示的fragment
     * 一对多关系
     */
    private static Map<String, String> activityPathFragment = new HashMap<>();


    /**
     * 当前统计时间是否为fragment
     */
    private boolean isFragment = false;

    /**
     * 页面存留时间
     * 该方法只会被调动多次 在onStop中调用
     *
     * @param context  页面的上下文信息
     * @param stayTime 页面创建时间 onStopTime-onStartTime
     * @param pagePath 页面路径 classA->classB[fragmentA, fragmentB]->classC
     */
    public abstract void stayTime(Context context, String pagePath, long stayTime);

    /**
     * 页面存留时间
     * 该方法只会被调动一次 在onDestroy中调用
     *
     * @param context     页面的上下文信息
     * @param surviveTime 页面创建时间 onStopTime-onStartTime
     * @param pagePath    页面路径 classA->classB[fragmentA][fragmentB]->classC
     */
    public abstract void surviveTime(Context context, String pagePath, long surviveTime);


    /**
     * 默认处理方法
     * 该方法将页面路由整理出来最上层为Manifest中间配置的主页面
     * 该方法也可重写
     * 层级关系为classA->classB[fragmentA, fragmentB]->classC
     */
    public void buryTime(BuryTimeInfo timeInfo) {
        this.isFragment = timeInfo.isFragment();
        if (isFragment) {
            activityPathFragment.put(timeInfo.getContext().getClass().getName(), timeInfo.getFragment().getClass().getName());
        }
        String pagePath = getPagePath();
        if (timeInfo.isActived()) {
            stayTime(timeInfo.getContext(), pagePath, timeInfo.getTime());
        } else {
            surviveTime(timeInfo.getContext(), pagePath, timeInfo.getTime());
        }
    }

    /**
     * 获得页面路径
     *
     * @return
     */
    public static String getPagePath() {
        return getParentPath().toString();
    }

    /**
     * 获得activity全路径
     *
     * @return
     */
    private static StringBuilder getParentPath() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String activity : stackActivities) {
            String fragment = activityPathFragment.get(activity);
            if (fragment != null && fragment.length() > 0) {
                activity += ("[" + fragment + "]");
            }
            stringBuilder.append("->").append(activity);
        }
        return stringBuilder;
    }

    public boolean isFragment() {
        return isFragment;
    }

    public static List<String> getStackActivities() {
        return stackActivities;
    }
}
