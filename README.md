#openxface for Android

This is Android port for openxface.

##Get started with the source
###Requirement
+ Android runtime enviroment (ADT 21.0.X)
+ Eclipse 3.8.0 or higher
+ Python 2.7.x
+ [Jake](https://github.com/mde/jake) (to build js, better install it globally)

###Step by Step
1. Clone a local copy
2. Config Eclipse:
    + Open menu **Window->Preferences->General->Workspace->Linked Resources**, click **New Variable**, then input **xFace\_apollo\_src** in label **Name**, click **Folder** choose path to **.../openxface-android/framework/android/src/**
    + Choose **Other:UTF-8** in **Text file encoding** of **Window->Preferences->General->Workpace**
    + Config **Complier compliance level** to **1.6** in **Window->Preferences->Java->Compiler**
    + Click **ok**
3. Eclipse open menu **File->Import->General->Existing Projects into Workspace** to import xFace player project
4. Prepare application
    - Put application source under **sdcard/xFacePlayer/applications/app**
5. Execute js.py to generate xface.js
6. Run with your app in the emulator or in the device

###Directory Structure

	├── framework
	|   ├── android
	|	|   ├── libs
	|	|   ├── src
	|	|   └── contents.txt
	├── project
	|   ├── engine
	|	|   ├── xface
	|	|	|   ├── android
	|	|	|	|   └── xFaceApolloPlayer
	|	|	|   └── js
	├── .gitignore
	├── COPYING
	├── NOTICE
	└── README.md

An overview of the main directories:

| FILE / DIRECTORY         | DESCRIPTION                                             |
| -------------------------| :-------------------------------------------------------|
| framework/android            | This is where the openxface Android source code resides    |
| project/engine/xface/android/xFaceApolloPlayer | Project files for xFacePlayer.              |
| project/engine/xface/js  | These are the JavaScript source codes which will be used to generate xface.js |

##Further Reading
+ Please visit [openxface](http://polyvi.github.io/openxface/)

#openxface
这里是openxface的Android分支

##如何使用源码
###开发环境
+ Android运行环境（ADT 21.0.x）
+ eclipse 3.8.0或更高版本
+ Python 2.7.x

###开发步骤
1. 使用git克隆一份本地代码
2. 配置eclipse：
    + 打开菜单**Window->Preferences->General->Workspace->Linked Resources**点击**New Variable**按钮,在对话框的 **Name**中输入**xFace\_apollo\_src**,点击 **Folder**选择路径到 **.../openxface-android/framework/android/src/**
    + 在菜单**Window->Preferences->General->Workspace**中的**Text file encoding**选项选择 **Other:UTF-8**
    + 在菜单**Window->Preferences->Java->Compiler**中选择 **Complier compliance level**为 **1.6**
    + 点击 **OK**保存设置
3. eclipse打开菜单**File->Import->General->Existing Projects into Workspace**导入xFace player工程
4. 准备好应用源码
    + 请把应用源码放到**sdcard/xFacePlayer/applications/app**下面
5. 执行js.py生成xface.js
6. 在模拟器或设备上运行

###目录结构

	├── framework
	|   ├── android
	|	|   ├── libs
	|	|   ├── src
	|	|   └── contents.txt
	├── project
	|   ├── engine
	|	|   ├── xface
	|	|	|   ├── android
	|	|	|	|   └── xFaceApolloPlayer
	|	|	|   └── js
	├── .gitignore
	├── COPYING
	├── NOTICE
	└── README.md

主要目录说明:

| FILE / DIRECTORY          | DESCRIPTION                         |
| ------------------------- | :-----------------------------------|
| framework/android         | 所有 openxface Android 源码所在目录   |
| project/engine/xface/android/xFaceApolloPlayer | xFacePlayer 工程文件所在目录|
| project/engine/xface/js   | 用于生成 xface.js 的 JavaScript 源码  |

##更多参考
+ 请访问[openxface](http://polyvi.github.io/openxface/)

---

xFace dev team, 2013 Polyvi Inc.

mail to: opensource@polyvi.com


