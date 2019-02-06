package com.godxj.plugin;

import android.content.Context;
import android.support.v4.app.Fragment;

public class BuryTimeInfo {

    /**
     * 上下文
     */
    private Context context;

    /**
     * 纯activity 该值为空
     */
    private Fragment fragment;

    /**
     * 页面停留时间
     */
    private long time;

    /**
     * 是否是fragment
     */
    private boolean isFragment;

    /**
     * 是否还存活
     */
    private boolean actived;

    /**
     * @param context    上下文
     * @param fragment   纯activity 该值为空
     * @param time       页面停留时间
     * @param isFragment 是否是fragment
     * @param actived    是否还存活
     */
    public BuryTimeInfo(Context context, Fragment fragment, long time, boolean isFragment, boolean actived) {
        this.context = context;
        this.fragment = fragment;
        this.time = time;
        this.isFragment = isFragment;
        this.actived = actived;
    }

    public Context getContext() {
        return context;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isFragment() {
        return isFragment;
    }

    public boolean isActived() {
        return actived;
    }

}
