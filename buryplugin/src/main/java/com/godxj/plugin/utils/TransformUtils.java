package com.godxj.plugin.utils;

import com.android.ide.common.internal.WaitableExecutor;
import com.godxj.plugin.adapter.InjectClassAdapter;
import com.godxj.plugin.adapter.ModifyClickClassAdapter;
import com.godxj.plugin.adapter.ModifyTimeClassAdapter;
import com.godxj.plugin.config.BuryConfig;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;


/**
 * 真是的插桩辅助类
 */
public class TransformUtils {

    //开启线程池做并发
    private static WaitableExecutor waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool();
    public static final String TAG_ONCLICK_PROXY = "com/godxj/plugin/imp/AbstractOnClickProxy";//代理执行的方法
    public static final String TAG_TIME_PROXY = "com/godxj/plugin/imp/AbstractTimeProxy";//代理执行的方法
    public static final String TAG_BURY_TIME_INFO = "com/godxj/plugin/BuryTimeInfo";
    //用户实现的代理点击类
    public static String INJECT_ONCLICK_CLASS_NAME = null;
    //用户实现的代理时间处理类
    public static String INJECT_TIME_CLASS_NAME = null;


    /**
     * 对文件方法进行数据插桩
     *
     * @param srcFile
     */
    public static void injectFile(File srcFile, com.godxj.plugin.config.BuryConfig buryConfig) {
        try {
            if (!injectEnable(buryConfig)) return;
            List<File> paths = FileTool.findClassesByFile(srcFile);
            for (File path : paths) {
                if (path.getAbsolutePath().contains("R$")) continue;
                waitableExecutor.execute(() -> {
                    //写入更改后的数据
                    if (injectTimeEnable(buryConfig)) { //时间处理的类不能为空
                        ClassWriter classWriter = modifyByFileOrByteArray(true, ModifyType.TIME, path, null);
                        //写入到目标文件
                        FileOutputStream fos = new FileOutputStream(path);
                        fos.write(classWriter.toByteArray());
                        fos.close();
                    }
                    if (injectClickEnable(buryConfig)){ //点击事件的处理类
                        ClassWriter classWriter = modifyByFileOrByteArray(true, ModifyType.ONCLICK, path, null);
                        //写入到目标文件
                        FileOutputStream fos = new FileOutputStream(path);
                        fos.write(classWriter.toByteArray());
                        fos.close();
                    }
                    return null;
                });
            }
            waitableExecutor.waitForTasksWithQuickFail(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改class根据文件或者byte数组
     */
    private static ClassWriter modifyByFileOrByteArray(boolean isFile, ModifyType modifyType, File file, byte[] bytes) throws IOException {
        ClassReader classReader = null;
        //写入更改后的数据
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor classAdapter = null;
        switch (modifyType) {
            case TIME://时间适配
                classAdapter = new ModifyTimeClassAdapter(classWriter);
                break;//点击事件适配
            case ONCLICK:
                classAdapter = new ModifyClickClassAdapter(classWriter);
                break;
        }
        //读取原文件数据
        if (isFile) {
            FileInputStream is = new FileInputStream(file.getAbsolutePath());
            classReader = new ClassReader(is);
        } else {
            classReader = new ClassReader(bytes);
        }
        classReader.accept(classAdapter, ClassReader.EXPAND_FRAMES);
        return classWriter;
    }

    /**
     * 对jar所有class文件方法进行数据插桩
     *
     * @param jarFile
     */
    public static void injectJarFile(File jarFile,File destFile, com.godxj.plugin.config.BuryConfig buryConfig) {
        try {
            if (!injectEnable(buryConfig)) return;
            waitableExecutor.execute(() -> {
                //零时jar文件存放位置
                String tempFile = DigestUtils.md5Hex(jarFile.getAbsolutePath()).substring(0, 8);
                File optJar = new File(jarFile.getParent(), tempFile + jarFile.getName());
                JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));
                JarFile file = new JarFile(jarFile);
                Enumeration enumeration = file.entries();
                //遍历所有class文件
                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                    InputStream inputStream = file.getInputStream(jarEntry);
                    String entryName = jarEntry.getName();
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    jarOutputStream.putNextEntry(zipEntry);
                    byte[] modifiedClassBytes = null;
                    byte[] sourceClassBytes = IOUtils.toByteArray(inputStream);
                    if (entryName.endsWith(".class")) {
                        if (injectTimeEnable(buryConfig)) {//时间处理父类不能为空
                            ClassWriter classWriter = modifyByFileOrByteArray(false, ModifyType.TIME, null, sourceClassBytes);
                            modifiedClassBytes = classWriter.toByteArray();
                        }
                        if (injectClickEnable(buryConfig)) {//时间处理父类不能为空
                            ClassWriter classWriter = modifyByFileOrByteArray(false, ModifyType.ONCLICK, null, sourceClassBytes);
                            modifiedClassBytes = classWriter.toByteArray();
                        }
                    }
                    if (modifiedClassBytes == null) {
                        jarOutputStream.write(sourceClassBytes);
                    } else {
                        jarOutputStream.write(modifiedClassBytes);
                    }
                    jarOutputStream.closeEntry();
                }
                jarOutputStream.close();
                file.close();
                //写入到目标文件
                FileUtils.copyFile(optJar, destFile);
                FileUtils.forceDelete(optJar);
                return null;
            });
            waitableExecutor.waitForTasksWithQuickFail(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 搜索继承了com/godxj/plugin/imp/AbstractOnClickProxy | com/godxj/plugin/imp/AbstractTimeProxy该类的类
     *
     * @param srcFile
     */
    public static void findInjectClass(File srcFile, com.godxj.plugin.config.BuryConfig buryConfig) {
        try {
            List<File> paths = FileTool.findClassesByFile(srcFile);
            for (File path : paths) {
                if (path.getAbsolutePath().contains("R$")) continue;
                waitableExecutor.execute(() -> {
                    //写入更改后的数据
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    InjectClassAdapter classAdapter = new InjectClassAdapter(classWriter);
                    //读取原文件数据
                    FileInputStream is = new FileInputStream(path.getAbsolutePath());
                    ClassReader classReader = new ClassReader(is);
                    classReader.accept(classAdapter, ClassReader.EXPAND_FRAMES);
                    //写入到目标文件
                    FileOutputStream fos = new FileOutputStream(path);
                    fos.write(classWriter.toByteArray());
                    fos.close();
                    if (buryConfig.isCollectClickBury() && classAdapter.isFindOnClickProxyClass()) {
                        INJECT_ONCLICK_CLASS_NAME = classAdapter.getOnClickProxyClassName();
                    }
                    if (buryConfig.isCollectStayTimeBury() && classAdapter.isFindTimeProxyClass()) {
                        INJECT_TIME_CLASS_NAME = classAdapter.getTimeProxyClassName();
                    }
                    return null;
                });
            }
            waitableExecutor.waitForTasksWithQuickFail(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从jar中寻找 优先从App file中查找如果找到就定为唯一
     *
     * @param jarFile
     * @param buryConfig
     */
    public static void findInjectClassFromJar(File jarFile, com.godxj.plugin.config.BuryConfig buryConfig) {
        try {
            waitableExecutor.execute(() -> {
                //零时jar文件存放位置
                JarFile file = new JarFile(jarFile);
                Enumeration enumeration = file.entries();
                //遍历所有class文件
                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                    InputStream inputStream = file.getInputStream(jarEntry);
                    String entryName = jarEntry.getName();
                    byte[] sourceClassBytes = IOUtils.toByteArray(inputStream);
                    if (entryName.endsWith(".class")) {
                        //写入更改后的数据
                        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        com.godxj.plugin.adapter.InjectClassAdapter classAdapter = new InjectClassAdapter(classWriter);
                        //读取原文件数据
                        ClassReader classReader = new ClassReader(sourceClassBytes);
                        classReader.accept(classAdapter, ClassReader.EXPAND_FRAMES);

                        if (INJECT_ONCLICK_CLASS_NAME == null && buryConfig.isCollectClickBury() && classAdapter.isFindOnClickProxyClass()) {
                            INJECT_ONCLICK_CLASS_NAME = classAdapter.getOnClickProxyClassName();
                        }
                        if (INJECT_TIME_CLASS_NAME == null && buryConfig.isCollectStayTimeBury() && classAdapter.isFindTimeProxyClass()) {
                            INJECT_TIME_CLASS_NAME = classAdapter.getTimeProxyClassName();
                        }
                    }

                }
                file.close();
                return null;
            });
            waitableExecutor.waitForTasksWithQuickFail(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否需要插桩处理
     * 如果没有查询到实现对应的实现类，直接停止插桩
     *
     * @return
     */
    private static boolean injectEnable(com.godxj.plugin.config.BuryConfig buryConfig) {
        if (!buryConfig.pluginEnable()) return false;
        //实现类都没有 自动停止
        if (INJECT_ONCLICK_CLASS_NAME == null && INJECT_TIME_CLASS_NAME == null) return false;
        //实现类都存在 允许插桩
        if (INJECT_ONCLICK_CLASS_NAME != null && INJECT_TIME_CLASS_NAME != null) return true;
        //实现类跟开关存在一个满足要求 允许插桩
        if (injectTimeEnable(buryConfig) || injectClickEnable(buryConfig)) return true;
        return false;
    }

    /**
     * 是否允许插桩 时间
     *
     * @param buryConfig
     * @return
     */
    public static boolean injectTimeEnable(com.godxj.plugin.config.BuryConfig buryConfig) {
        if (buryConfig.isCollectStayTimeBury() && INJECT_TIME_CLASS_NAME != null) return true;
        return false;
    }

    /**
     * 是否允许插桩 点击事件
     *
     * @param buryConfig
     * @return
     */
    public static boolean injectClickEnable(BuryConfig buryConfig) {
        if (buryConfig.isCollectClickBury() && INJECT_ONCLICK_CLASS_NAME != null) return true;
        return false;
    }

    public enum ModifyType {
        TIME,
        ONCLICK
    }
}
