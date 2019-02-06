package com.godxj.plugin.config;

public class BuryConfig {

    /**
     * 是否启用插件
     */
    private boolean startPlugin = false;

    /**
     * 是否页面时间停留时间统计
     * 必须有一个类继承com/godxj/plugin/imp/AbstractTimeProxy
     * 默认收集
     */
    private boolean collectStayTimeBury = true;

    /**
     * 是否收集点击事件处理相关埋点
     * 必须有一个类继承com/godxj/plugin/imp/AbstractOnClickProxy
     * 默认收集
     */
    private boolean collectClickBury = true;

    /**
     * 该字段可选 如果配置该字段 将只埋点配置对应的页面，页面以类名全路径，多个之间用","隔开
     * 埋点所有配置页面
     * 不配置时，某人读取manifest中所有的activity进行埋点
     */
    private String buryActivities;

    /**
     * 同理buryActivities
     * 不配置时，将埋点所有继承android.support.v4.app.Fragment、android.app.Fragment 所有的fragment
     */
    private String buryFragments;

    public boolean isStartPlugin() {
        return startPlugin;
    }

    public void setStartPlugin(boolean startPlugin) {
        this.startPlugin = startPlugin;
    }

    public boolean isCollectStayTimeBury() {
        return collectStayTimeBury;
    }

    public void setCollectStayTimeBury(boolean collectStayTimeBury) {
        this.collectStayTimeBury = collectStayTimeBury;
    }

    public boolean isCollectClickBury() {
        return collectClickBury;
    }

    public void setCollectClickBury(boolean collectClickBury) {
        this.collectClickBury = collectClickBury;
    }

    /**
     * 插件是否执行
     * @return
     */
    public boolean pluginEnable() {
        if (!startPlugin) return false;//未启动插件
        if (!collectClickBury && !collectStayTimeBury) return false;//什么都不统计
        return true;
    }


    public String getBuryActivities() {
        return buryActivities;
    }

    public void setBuryActivities(String buryActivities) {
        this.buryActivities = buryActivities;
    }

    public String getBuryFragments() {
        return buryFragments;
    }

    public void setBuryFragments(String buryFragments) {
        this.buryFragments = buryFragments;
    }

}
