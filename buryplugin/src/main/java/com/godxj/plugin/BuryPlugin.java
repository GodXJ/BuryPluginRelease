package com.godxj.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.AndroidSourceFile;
import com.android.build.gradle.api.AndroidSourceSet;
import com.android.build.gradle.internal.dsl.DefaultConfig;
import com.godxj.plugin.config.AppBaseInfo;
import com.godxj.plugin.config.BuryConfig;
import com.godxj.plugin.config.MethodCell;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicBoolean;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

import static org.objectweb.asm.Opcodes.*;

public class BuryPlugin implements Plugin<Project> {

    private static final String NAME = "{http://schemas.android.com/apk/res/android}name";
    private static final String ACTION_MAIN = "android.intent.action.MAIN";
    private AppBaseInfo appBaseInfo = AppBaseInfo.getInstance();

    @Override
    public void apply(Project project) {
        project.getExtensions().create("buryConfig", BuryConfig.class);
        AppExtension appExtension = project.getExtensions().getByType(AppExtension.class);
        initAppBaseInfo(appExtension);
        appExtension.registerTransform(new BuryTransform(project), Collections.EMPTY_LIST);
    }

    /**
     * 初始化App相关基本信息
     *
     * @param appExtension
     */
    private void initAppBaseInfo(AppExtension appExtension) {
        try {
            //app.build基本信息
            appBaseInfo.setCompileSdkVersion(appExtension.getCompileSdkVersion());
            DefaultConfig defaultConfig = appExtension.getDefaultConfig();
            appBaseInfo.setApplicationId(defaultConfig.getApplicationId());
            appBaseInfo.setMinSdkVersion(defaultConfig.getMinSdkVersion().getApiString());
            appBaseInfo.setTargetSdkVersion(defaultConfig.getTargetSdkVersion().getApiString());
            appBaseInfo.setVersionCode(defaultConfig.getVersionCode().toString());
            appBaseInfo.setVersionName(defaultConfig.getVersionName());

            //读取manifest中所有的Activity用于页面相关数据统计
            SortedMap<String, AndroidSourceSet> asMap = appExtension.getSourceSets().getAsMap();
            AndroidSourceFile manifest = asMap.get("main").getManifest();
            Node node = new XmlParser().parse(manifest.getSrcFile());
            //包名
            appBaseInfo.setPackageName((String) node.attribute("package"));
            //获取所有的权限
            List<String> permissions = new ArrayList<>();
            NodeList userPermissions = node.getAt(new QName("uses-permission"));
            if (userPermissions != null) {
                for (int i = 0; i < userPermissions.size(); i++) {
                    Node permission = (Node) userPermissions.get(i);
                    permission.attributes().forEach((k, v) -> {
                        if (NAME.equals(k.toString().trim())) {
                            permissions.add(v.toString().trim());
                        }
                    });
                }
            }
            appBaseInfo.setUsesPermissions(permissions);
            //获取所有的activity
            List<String> allActivities = new ArrayList<>();
            NodeList applicaiton = node.getAt(new QName("application"));
            if (applicaiton != null) {
                NodeList activitiesNode = applicaiton.getAt(new QName("activity"));
                for (int i = 0; i < activitiesNode.size(); i++) {
                    Node activity = (Node) activitiesNode.get(i);
                    activity.attributes().forEach((k, v) -> {
                        if (NAME.equals(k.toString().trim())) {
                            String value = v.toString().trim();
                            if (".".equals(value.substring(0, 1))) {
                                value = value.replaceFirst("\\.", appBaseInfo.getPackageName() + ".");
                            }
                            allActivities.add(value);
                        }
                    });

                    NodeList actionNode = activity.getAt(new QName("intent-filter")).getAt(new QName("action"));
                    if (actionNode != null && actionNode.size() != 0) {
                        AtomicBoolean find = new AtomicBoolean(false);
                        for (int j = 0; j < actionNode.size(); j++) {
                            Node action = (Node) actionNode.get(i);
                            action.attributes().forEach((k, v) -> {
                                if (NAME.equals(k.toString().trim())) {
                                    if (ACTION_MAIN.equals(v.toString().trim())) {
                                        find.set(true);
                                        appBaseInfo.setStartActivity(allActivities.get(allActivities.size() - 1));
                                        return;
                                    }
                                }
                            });
                            if (find.get()) break;
                        }
                    }
                }
            }
            appBaseInfo.setAllActivities(allActivities);

            //默认插桩的fragment
            List<String> fragments = new ArrayList<>();
            fragments.add("android.support.v4.app.Fragment");
//            fragments.add("android.app.Fragment");
            appBaseInfo.setAllFragments(fragments);
            //默认插桩的click接口
            Map<String, MethodCell> allInterfaceMethods = new HashMap<>();
            //View点击
            allInterfaceMethods.put("android/view/View$OnClickListener", new MethodCell(new int[]{ALOAD}, "onClick", "(Landroid/view/View;)V",false));
            allInterfaceMethods.put("android/view/View$OnTouchListener", new MethodCell(new int[]{ALOAD,ALOAD}, "onTouch", "(Landroid/view/View;)Z",true));
            //View长按
            allInterfaceMethods.put("android/view/View$OnLongClickListener", new MethodCell(new int[]{ALOAD}, "onLongClick", "(Landroid/view/View;Landroid.view.MotionEvent;)Z",true));
            //dialog 点击事件
            allInterfaceMethods.put("android/content/DialogInterface$OnClickListener", new MethodCell(new int[]{ALOAD, ILOAD}, "onClick", "(Landroid/content/DialogInterface;I)V",false));
            allInterfaceMethods.put("android/content/DialogInterface$OnCancelListener", new MethodCell(new int[]{ALOAD}, "onCancel", "(Landroid/content/DialogInterface;)V",false));
            allInterfaceMethods.put("android/content/DialogInterface$OnShowListener", new MethodCell(new int[]{ALOAD}, "onShow","(Landroid/content/DialogInterface;)V",false));
            allInterfaceMethods.put("android/content/DialogInterface$OnDismissListener", new MethodCell(new int[]{ALOAD}, "onDismiss","(Landroid/content/DialogInterface;)V",false));
            allInterfaceMethods.put("android/widget/AdapterView$OnItemClickListener", new MethodCell(new int[]{ALOAD, ALOAD, ILOAD, LLOAD}, "onItemClick", "(Landroid/widget/AdapterView;Landroid/view/View;IJ)V",false));
            allInterfaceMethods.put("android/widget/AdapterView$OnItemLongClickListener", new MethodCell(new int[]{ALOAD, ALOAD, ILOAD, LLOAD}, "onItemLongClick", "(Landroid/widget/AdapterView;Landroid/view/View;IJ)Z",true));
            allInterfaceMethods.put("android/widget/AdapterView$OnItemSelectedListener", new MethodCell(new int[]{ALOAD, ALOAD, ILOAD, LLOAD}, "onItemSelected", "(Landroid/widget/AdapterView;Landroid/view/View;IJ)Z",true));
            allInterfaceMethods.put("android/widget/ExpandableListView$OnGroupClickListener", new MethodCell(new int[]{ALOAD, ALOAD, ILOAD, LLOAD}, "onGroupClick", "(Landroid/widget/ExpandableListView;Landroid/view/View;IJ)Z",true));
            allInterfaceMethods.put("android/widget/ExpandableListView$OnGroupExpandListener", new MethodCell(new int[]{ILOAD}, "onGroupExpand", "(I)Z",false));
            allInterfaceMethods.put("android/widget/ExpandableListView$OnChildClickListener", new MethodCell(new int[]{ALOAD, ALOAD, ILOAD, ILOAD, LLOAD}, "onChildClick", "(Landroid/widget/ExpandableListView;Landroid/view/View;IIJ)Z",true));
            allInterfaceMethods.put("android/widget/RatingBar$OnRatingBarChangeListener", new MethodCell(new int[]{ALOAD, FLOAD, ILOAD}, "onRatingChanged", "(Landroid/widget/RatingBar;FZ)V",false));
            allInterfaceMethods.put("android/widget/RadioGroup$OnCheckedChangeListener", new MethodCell(new int[]{ALOAD, ILOAD}, "onCheckedChanged", "(Landroid/widget/CompoundButton;Z)V",false));
            allInterfaceMethods.put("android/widget/CompoundButton$OnCheckedChangeListener", new MethodCell(new int[]{ALOAD, ILOAD}, "onCheckedChanged", "(Landroid/widget/RadioGroup;I)V",false));

            appBaseInfo.setClickInterfaceMethods(allInterfaceMethods);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
