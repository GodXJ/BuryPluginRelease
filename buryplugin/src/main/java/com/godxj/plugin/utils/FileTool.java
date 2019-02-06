package com.godxj.plugin.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileTool {

    /**
     * 多去jar中的所有class文件路径
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static List<String> findClassesFromJar(String path)
            throws IOException {
        List<String> classes = new ArrayList<String>();
        JarFile jar = new JarFile(path);
        Enumeration<JarEntry> files = jar.entries();
        while (files.hasMoreElements()) {
            JarEntry jarEntry = files.nextElement();
            String name = jarEntry.getName();
            if (name.matches("^.*class$"))
                classes.add(name.replaceAll("/", "."));
        }
        return classes;
    }

    /**
     * 获取文件下面所有的class文件路径
     *
     * @param file
     * @return
     */
    public static List<File> findClassesByFile(File file) {
        List<File> classes = new ArrayList<>();
        if (file == null) return classes;
        if (file.isDirectory()) {
            //遍历所有的file
            for (File childFile : file.listFiles()) {
                if(childFile.isFile()){
                    classes.add(childFile);
                }else{
                    classes.addAll(findClassesByFile(childFile));
                }
            }
        } else if (file.isFile()) {
            classes.add(file);
        }
        return classes;
    }
}
