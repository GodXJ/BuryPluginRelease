package com.godxj.plugin.config;

import java.util.List;
import java.util.Map;

/**
 * 应用基本信息
 */
public class AppBaseInfo {

    private static AppBaseInfo instance;

    public static AppBaseInfo getInstance(){
        if (instance == null){
            instance = new AppBaseInfo();
        }
        return instance;
    }

    private AppBaseInfo(){}

    /**
     * 应用id
     */
    private String applicationId;

    /**
     * 编译版本
     */
    private String compileSdkVersion;

    /**
     * 支持最低sdk版本
     */
    private String minSdkVersion;

    /**
     * 目标sdk版本
     */
    private String targetSdkVersion;

    /**
     * 版本号
     */
    private String versionCode;

    /**
     * 版本名称
     */
    private String versionName;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 注册清单中注册的所有权限(不包括动态注册权限)
     */
    private List<String> usesPermissions;

    /**
     * 所有的activity路径
     */
    private List<String> allActivities;

    /**
     * 所有fragment
     */
    private List<String> allFragments;

    /**
     * 启动的Activity
     */
    private String startActivity;

    /**
     * 接口对应的方法
     */
    private Map<String, MethodCell> clickInterfaceMethods;

    public String getStartActivity() {
        return startActivity;
    }

    public void setStartActivity(String startActivity) {
        this.startActivity = startActivity;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getCompileSdkVersion() {
        return compileSdkVersion;
    }

    public void setCompileSdkVersion(String compileSdkVersion) {
        this.compileSdkVersion = compileSdkVersion;
    }

    public String getMinSdkVersion() {
        return minSdkVersion;
    }

    public void setMinSdkVersion(String minSdkVersion) {
        this.minSdkVersion = minSdkVersion;
    }

    public String getTargetSdkVersion() {
        return targetSdkVersion;
    }

    public void setTargetSdkVersion(String targetSdkVersion) {
        this.targetSdkVersion = targetSdkVersion;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getUsesPermissions() {
        return usesPermissions;
    }

    public void setUsesPermissions(List<String> usesPermissions) {
        this.usesPermissions = usesPermissions;
    }

    public List<String> getAllActivities() {
        return allActivities;
    }

    public void setAllActivities(List<String> allActivities) {
        this.allActivities = allActivities;
    }

    public List<String> getAllFragments() {
        return allFragments;
    }

    public void setAllFragments(List<String> allFragments) {
        this.allFragments = allFragments;
    }

    public Map<String, MethodCell> getClickInterfaceMethods() {
        return clickInterfaceMethods;
    }

    public void setClickInterfaceMethods(Map<String, MethodCell> clickInterfaceMethods) {
        this.clickInterfaceMethods = clickInterfaceMethods;
    }

    @Override
    public String toString() {
        return "AppBaseInfo{" +
                "applicationId='" + applicationId + '\'' +
                ", compileSdkVersion='" + compileSdkVersion + '\'' +
                ", minSdkVersion='" + minSdkVersion + '\'' +
                ", targetSdkVersion='" + targetSdkVersion + '\'' +
                ", versionCode='" + versionCode + '\'' +
                ", versionName='" + versionName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", usesPermissions=" + usesPermissions +
                ", allActivities=" + allActivities +
                ", allFragments=" + allFragments +
                ", startActivity='" + startActivity + '\'' +
                ", clickInterfaceMethods=" + clickInterfaceMethods +
                '}';
    }
}
