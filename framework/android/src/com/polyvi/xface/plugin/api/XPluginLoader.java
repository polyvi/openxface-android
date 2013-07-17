
/*
 Copyright 2012-2013, Polyvi Inc. (http://polyvi.github.io/openxface)
 This program is distributed under the terms of the GNU General Public License.

 This file is part of xFace.

 xFace is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 xFace is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with xFace.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.polyvi.xface.plugin.api;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.polyvi.xface.core.XConfiguration;
import com.polyvi.xface.core.XISystemContext;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.util.XLog;

import dalvik.system.DexClassLoader;

public class XPluginLoader {

    private static final String CLASS_NAME = XPluginLoader.class
            .getSimpleName();
    private static final String PLUGIN_JAR_ENTRY_TAG = "Main-Class";
    private static final String JAR_SUFFIX = ".jar";

    /**
     * 管理插件的map
     */
    private ConcurrentHashMap<String, XPluginBase> mExtenalPlugins;
    private XIWebContext mPluginExecuteContext;

    /**
     * jar包的loader map
     */
    private static HashMap<String, XEntry> mJarLoaderMap = new HashMap<String, XEntry>();

    /**
     * jar库存放的目录
     */
    private static String mJarFolder;

    /**
     * 可以目录 系统动态加载jar文件需要使用
     */
    private static String mWriteableDir;

    /**
     * 构造一个键值对结构
     */
    private static class XEntry {
        public XEntry(ClassLoader loader, HashMap<String, String> map) {
            mLoader = loader;
            mPluginDescription = map;
        }

        public ClassLoader mLoader;
        public HashMap<String, String> mPluginDescription;
    }

    public XPluginLoader(XIWebContext webCtx) {
        mPluginExecuteContext = webCtx;
        mExtenalPlugins = new ConcurrentHashMap<String, XPluginBase>();
    }

    /**
     * 加载插件
     *
     * @param cordovaInterface
     */
    public ConcurrentHashMap<String, XPluginBase> loadPlugins(
            XISystemContext ctx, XExtensionContext extContext) {
        // 目前有三种加载插件的方式
        // 从xml中读取插件
        loadPluginFromMap(extContext,
                extContext.getSystemContext().getContext().getClassLoader(),
                XConfiguration.getInstance().readPluginsConfig());
        // 从jar包中读取插件
        loadPluginFromJar(extContext);
        // 从xml中读取插件描述 具体插件的配置通过描述返回
        loadPluginByDescription(extContext,
                extContext.getSystemContext().getContext().getClassLoader(),
                XConfiguration.getInstance().readPluginDescriptions());

        return mExtenalPlugins;
    }

    private void loadPluginFromMap(XExtensionContext extContext,
            ClassLoader loader, HashMap<String, String> map) {
        Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            XPluginBase p = createPlugin(extContext, entry.getValue(), loader);
            if (null != p) {
                registerPlugin(entry.getKey(), p);
            }
        }
    }

    /**
     * 卸载插件 在app被卸载的时候需要清空插件
     */
    public void unloadPlugins() {
        // TODO 是否需要发送一个事件给每个插件
        mExtenalPlugins.clear();
    }

    /**
     * 创建插件
     *
     * @param className
     * @param loader
     * @return
     */
    private XPluginBase createPlugin(XExtensionContext ectx, String className,
            ClassLoader loader) {
        try {
            Class<?> cls = loader.loadClass(className);
            Object obj = cls.newInstance();
            if (!(obj instanceof XPluginBase)) {
                XLog.e(CLASS_NAME, "Class (" + className
                        + ") not a sub class of XExtension!");
                return null;
            }
            XPluginBase plugin = (XPluginBase) obj;
            plugin.initialize(ectx, mPluginExecuteContext);
            return plugin;
        } catch (ClassNotFoundException e) {
            XLog.e(CLASS_NAME, "Class:" + className + " not found!");
        } catch (InstantiationException e) {
            XLog.e(CLASS_NAME, "Can't create object of class " + className);
        } catch (IllegalAccessException e) {
            XLog.e(CLASS_NAME, "Can't create object of class " + className);
        }
        return null;
    }

    /**
     * 注册插件
     *
     * @param name
     * @param plugin
     */
    private void registerPlugin(String name, XPluginBase plugin) {
        mExtenalPlugins.put(name, plugin);
    }

    /**
     * 获取插件
     *
     * @return
     */
    public ConcurrentHashMap<String, XPluginBase> getPlugins() {
        return mExtenalPlugins;
    }

    /**
     * 设置动态记载库的目录以及提供一个系统可写目录(系统需要将dex文件缓存到这个可写目录)
     *
     * @param jarDir
     */
    public static void setJarDir(String jarDir, String writeableDir) {
        mJarFolder = jarDir;
        mWriteableDir = writeableDir;
    }

    /**
     * 根据插件描述加载插件
     */
    private void loadPluginByDescription(XExtensionContext ctx,
            ClassLoader loader, Set<String> deses) {
        try {
            Iterator<String> iter = deses.iterator();
            while (iter.hasNext()) {
                String des = iter.next();
                Class<?> libProviderClazz = loader.loadClass(des);
                Object obj = libProviderClazz.newInstance();
                if (obj instanceof XIPluginDescription) {
                    XIPluginDescription desObj = (XIPluginDescription) libProviderClazz
                            .newInstance();
                    HashMap<String, String> map = desObj.getPluginDesciption();
                    loadPluginFromMap(ctx, loader, map);
                } else {
                    XLog.e(CLASS_NAME, "instance XPluginDescription error");
                }

            }
        } catch (ClassNotFoundException e) {
            XLog.e(CLASS_NAME, "ClassNotFoundException  when load plugin description");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            XLog.e(CLASS_NAME, "IllegalAccessException when load plugin description");
            e.printStackTrace();
        } catch (InstantiationException e) {
            XLog.e(CLASS_NAME, "InstantiationException when load plugin description");
            e.printStackTrace();
        }
    }

    /**
     * 动态从jar文件中读取插件配置
     *
     * @param ctx
     * @param jarPath
     *            jar文件的文件夹
     * @return
     */
    private void loadPluginFromJar(XExtensionContext ctx) {
        // 参数检查
        if (null == mJarFolder || null == mWriteableDir
                || !new File(mWriteableDir).exists()
                || !new File(mWriteableDir).exists()) {
            return;
        }
        // 读取思路：
        // 1. 通过Manifest读取插件描述类 插件描述类必须实现接口XIPluginDescription
        // 2. 通过描述类找到对应的插件类以及js上的访问名称
        // 3. TODO 如何动态卸载？
        // 4. FIXME:如果jar动态调用so文件,还需要测试

        if (!mJarLoaderMap.isEmpty()) {
            // 通过这里保证引擎启动 只加载一次
            for (XEntry value : mJarLoaderMap.values()) {
                loadPluginFromMap(ctx, value.mLoader, value.mPluginDescription);
            }
            return;
        }
        File[] files = new File(mJarFolder).listFiles(new ModuleFilter());
        for (File jar : files) {
            try {
                DexClassLoader cl = new DexClassLoader(jar.getAbsolutePath(),
                        mWriteableDir, null, ctx.getSystemContext()
                                .getContext().getClassLoader());
                JarFile jarFile = new JarFile(jar);
                Manifest manifest = jarFile.getManifest();
                String pluginDes = manifest.getMainAttributes().getValue(
                        PLUGIN_JAR_ENTRY_TAG);
                if (null == pluginDes) {
                    continue;
                }
                Class<?> libProviderClazz = cl.loadClass(pluginDes);
                XIPluginDescription obj = (XIPluginDescription) libProviderClazz
                        .newInstance();
                HashMap<String, String> map = obj.getPluginDesciption();
                loadPluginFromMap(ctx, cl, map);
                mJarLoaderMap.put(jar.getAbsolutePath(), new XEntry(cl, map));
            } catch (ClassNotFoundException e) {
                handleLoadExceptionFromJar(e, jar,
                        " Load class dynamicly error.");
            } catch (IllegalAccessException e) {
                handleLoadExceptionFromJar(e, jar,
                        " lllegal access error when instance plugin.");
            } catch (InstantiationException e) {
                handleLoadExceptionFromJar(e, jar, " instance plugin error.");
            } catch (IOException e) {
                handleLoadExceptionFromJar(e, jar, "init jar package error.");
            }
        }
    }

    /**
     * 处理从jar包中加载类的异常
     *
     * @param e
     *            异常对象
     * @param jar
     *            jar包文件
     * @param tag
     *            log信息
     */
    private void handleLoadExceptionFromJar(Exception e, File jar, String log) {
        mJarLoaderMap.remove(jar.getAbsolutePath());
        XLog.e(CLASS_NAME, log);
        e.printStackTrace();
    }

    /**
     * 过滤器 仅仅加载jar文件
     */
    private static class ModuleFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isFile()
                    && file.getName().toLowerCase().endsWith(JAR_SUFFIX);
        }
    }

}
