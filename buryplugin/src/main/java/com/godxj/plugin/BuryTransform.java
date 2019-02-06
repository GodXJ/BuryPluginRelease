package com.godxj.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.godxj.plugin.config.AppBaseInfo;
import com.godxj.plugin.config.BuryConfig;
import com.godxj.plugin.utils.TransformUtils;

import org.apache.commons.io.FileUtils;
import org.apache.http.util.TextUtils;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class BuryTransform extends Transform {

    private Project project;
    private BuryConfig buryConfig;
    private AppBaseInfo appBaseInfo = AppBaseInfo.getInstance();

    public BuryTransform(Project project) {
        this.project = project;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        buryConfig = project.getExtensions().getByType(BuryConfig.class);
        configGradleInfo();
        doTransform(transformInvocation);
    }

    /**
     * 配置工程信息
     */
    private void configGradleInfo() {
        String buryActivities = buryConfig.getBuryActivities();
        //获得需要插桩的配置acitivities
        if (!TextUtils.isEmpty(buryActivities)) {
            String[] activities = buryActivities.split(",");
            ArrayList arrayList = new ArrayList();
            for (String activity : activities) {
                arrayList.add(activity.trim());
            }
            appBaseInfo.setAllActivities(arrayList);
        }
        //获得需要插桩的配置fragments
        String buryFragments = buryConfig.getBuryFragments();
        if (!TextUtils.isEmpty(buryFragments)) {
            String[] fragments = buryFragments.split(",");
            ArrayList arrayList = new ArrayList();
            for (String fragment : fragments) {
                arrayList.add(fragment.trim());
            }
            appBaseInfo.setAllFragments(arrayList);
        }
    }

    /**
     * 开始插桩
     *
     * @param transformInvocation
     * @throws TransformException
     * @throws InterruptedException
     * @throws IOException
     */
    private void doTransform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        Collection<TransformInput> transformInputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        //增量 无需处理
        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs();
        boolean incremental = transformInvocation.isIncremental();
        //如果非增量，则清空旧的输出内容
        if (!incremental) {
            outputProvider.deleteAll();
        }
        //搜索代理类
        searchProxyClassName(transformInputs, outputProvider);
        //开始插桩
        for (TransformInput input : transformInputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                /*
                NOTCHANGED 文件未变动
                ADDED，CHANGED 有变动
                REMOVED 文件被删除
                 */
                Status status = jarInput.getStatus();
                File dest = outputProvider.getContentLocation(
                        jarInput.getFile().getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);
                if (incremental) {
                    switch (status) {
                        case CHANGED:
                        case ADDED:
                            transformJar(jarInput.getFile(), dest, status);
                            break;
                        case REMOVED:
                            if (dest.exists()) {
                                //删除文件
                                FileUtils.forceDelete(dest);
                            }
                            break;
                        case NOTCHANGED:
                            break;
                    }
                } else {
                    transformJar(jarInput.getFile(), dest, status);
                }
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                FileUtils.forceMkdir(dest);
                if (incremental) {
                    Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();
                    String srcPath = directoryInput.getFile().getAbsolutePath();
                    String destPath = dest.getAbsolutePath();
                    for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                        Status status = changedFile.getValue();
                        File inputFile = changedFile.getKey();
                        //更换父级路径
                        String destFilePath = inputFile.getAbsolutePath().replace(srcPath, destPath);
                        File destFile = new File(destFilePath);
                        switch (status) {
                            case NOTCHANGED:
                                break;
                            case REMOVED:
                                if (destFile.exists()) {
                                    FileUtils.forceDelete(destFile);
                                }
                                break;
                            case ADDED:
                            case CHANGED:
                                FileUtils.touch(destFile);//至少更新文件日期
                                transformSingleFile(inputFile, destFile, srcPath);
                                break;
                        }
                    }
                } else {
                    transformDir(directoryInput.getFile(), dest);
                }
            }
        }
    }

    private void searchProxyClassName(Collection<TransformInput> transformInputs, TransformOutputProvider outputProvider) {
        //遍历所有目录获取当前App程序包下面唯一一个继承了插件提供的一个抽象类，该类用于插桩
        for (TransformInput input : transformInputs) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File file = directoryInput.getFile();
                if (file != null) {
                    TransformUtils.findInjectClass(file, buryConfig);
                }
            }
        }
        //查询jar中是否同样存在插件处理方法（有些基础框架喜欢将统计类放到基础框架中）
        if ((buryConfig.isCollectClickBury() && TransformUtils.INJECT_ONCLICK_CLASS_NAME == null) || (buryConfig.isCollectStayTimeBury() && TransformUtils.INJECT_TIME_CLASS_NAME == null)) {
            for (TransformInput input : transformInputs) {
                for (JarInput jarInput : input.getJarInputs()) {
                    File jarFile = jarInput.getFile();
                    if (jarFile != null) {
                        TransformUtils.findInjectClassFromJar(jarFile, buryConfig);
                    }
                }
            }
        }
        //如果都没有找到采用默认
        if (TransformUtils.INJECT_ONCLICK_CLASS_NAME == null){
            TransformUtils.INJECT_ONCLICK_CLASS_NAME = "com.godxj.plugin.SimpleClickProxy";
        }
        if (TransformUtils.INJECT_TIME_CLASS_NAME == null){
            TransformUtils.INJECT_TIME_CLASS_NAME = "com.godxj.plugin.SimpleTimeProxy";
        }
        System.out.println(TransformUtils.INJECT_ONCLICK_CLASS_NAME);
        System.out.println(TransformUtils.INJECT_TIME_CLASS_NAME);

    }

    @Override
    public String getName() {
        return "BuryTransform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    /**
     * 修改字节
     *
     * @param file
     * @param dest
     * @param status
     */
    private void transformJar(File file, File dest, Status status) throws IOException {
        switch (status) {
            case CHANGED:
            case ADDED:
                if (buryConfig.pluginEnable()) {
                    TransformUtils.injectJarFile(file, dest, buryConfig);
                } else {
                    FileUtils.copyFile(file, dest);
                }
                break;
            case REMOVED:
                if (dest.exists()) {
                    //删除文件
                    FileUtils.forceDelete(dest);
                }
                break;
            case NOTCHANGED:
                if (buryConfig.pluginEnable()) {
                    TransformUtils.injectJarFile(file, dest, buryConfig);
                } else {
                    FileUtils.copyFile(file, dest);
                }
                break;
        }
    }

    private void transformDir(File file, File dest) throws IOException, InterruptedException {
        if (buryConfig.pluginEnable()) {
            TransformUtils.injectFile(file, buryConfig);
        }
        FileUtils.copyDirectory(file, dest);
    }

    private void transformSingleFile(File inputFile, File destFile, String srcPath) throws IOException {
        if (buryConfig.pluginEnable()) {
            TransformUtils.injectFile(inputFile, buryConfig);
        }
        FileUtils.copyFile(inputFile, destFile);
    }

}
