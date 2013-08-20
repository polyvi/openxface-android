
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

package com.polyvi.xface;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.polyvi.xface.configXml.XTagNotFoundException;
import com.polyvi.xface.core.XConfiguration;
import com.polyvi.xface.core.XISystemContext;
import com.polyvi.xface.core.XRuntime;
import com.polyvi.xface.event.XEvent;
import com.polyvi.xface.event.XEventType;
import com.polyvi.xface.event.XSystemEventCenter;
import com.polyvi.xface.extension.XActivityResultListener;
import com.polyvi.xface.ssl.XSSLManager;
import com.polyvi.xface.util.XConstant;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XNotification;
import com.polyvi.xface.util.XStrings;
import com.polyvi.xface.util.XUtils;
import com.polyvi.xface.view.XAppWebView;
import com.umeng.analytics.MobclickAgent;

/**
 * 该类是android程序的主activity，也是整个程序的入口.
 * 主要管理整个程序的生命周期以及执行程序的初始化操作
 */
/**
 * TODO:XFaceMainActivity抽象出如下接口： <br/>
 * 1、getActivity，扩展等其他地方用到Activity（context）都由这个方法获取 <br/>
 * 2、startActivityForResult与其他类似接口 <br/>
 * 这样XFaceMainActivity就避免暴露过多的接口，又不用强转。<br/>
 */
public class XFaceMainActivity extends Activity implements XISystemContext{

    private static final String CLASS_NAME = XFaceMainActivity.class.getName();

    private static final int ANDROID4_2_API_LEVEL = 17;
    /** The content view of activity */
    private ViewGroup mContentView = null;

    protected ViewGroup mBootSplashView;

    private View mAppSplashView;

    private Bitmap mAppSplashImage;
    protected TextView mVersionText;

    protected RelativeLayout.LayoutParams mVersionParams;

    protected XRuntime mRuntime;

    private XNotification mWaitingNotification = new XNotification(this);;

    private HashMap<Integer, XActivityResultListener> mActivityResultListenerMap = null;

    private XStartParams mStartParams;

    private XSecurityPolicy mSecurityPolicy;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        systemBoot();
    }

    /**
     * 初始化系统事件处理器
     */
    private void initSystemEventCenter() {
        XSystemEventCenter.init(this);
    }

    /**
     * 创建app安全策略
     * @return 安全策略
     */
    protected XSecurityPolicy createSecurityPolicy() {
        return new XDefaultSecurityPolicy(this);
    }

    /**
     * 创建系统启动组件
     * @return
     */
    protected XSystemBootstrap createSystemBootstrap() {
        return new XSystemInitializer(this);
    }

    /**
     * 创建管理与https有关证书库对象
     */
    protected void createSSLManager(){
        XSSLManager.createInstance(this);
    }

    /**
     * 创建系统运行时对象
     */
    public XRuntime initRuntime() {
        mRuntime = new XRuntime();
        initActivityResultListenerMap();
        mSecurityPolicy = createSecurityPolicy();
        mRuntime.init(this);
        return mRuntime;
    }

    /**
     * 初始化保存XActivityResultListener的Map
     */
    private void initActivityResultListenerMap() {
        mActivityResultListenerMap = new HashMap<Integer, XActivityResultListener>();
    }

    /**
     * 程序的入口函数
     */
    private void systemBoot() {
        initMobclickAgent();
        initSystemEventCenter();
        XConfiguration.getInstance().loadPlatformStrings(getContext());
        mStartParams = XStartParams.parse(getIntent().getStringExtra(
                XConstant.TAG_APP_START_PARAMS));
        // 解析系统配置
        try {
            initSystemConfig();
        } catch (IOException e) {
            this.toast("Loading System Config Failure.");
            XLog.e(CLASS_NAME, "Loading system config failure!");
            e.printStackTrace();
            return;
        } catch (XTagNotFoundException e) {
            this.toast("Loading System Config Failure.");
            XLog.e(CLASS_NAME, "parse config.xml error:" + e.getMessage());
            e.printStackTrace();
            return;
        }
        // 配置系统LOG等级
        XLog.setLogLevel(XConfiguration.getInstance().readLogLevel());
        // 配置系统的工作目录
        XConfiguration.getInstance()
                .configWorkDirectory(this, getWorkDirName());
        // 设置window的样式
        setWindowStyle();
        // 创建view容器
        createContentView();
        // 根据平台配置决定是否启动splash
        startBootSplashIfNeeded();
        // 根据平台配置决定是否检测更新
        checkUpdateIfNeeded();
        createSSLManager();
        // 系统启动
        XSystemBootstrap bootstrap = createSystemBootstrap();
        new XPrepareWorkEnvronmentTask(bootstrap, this).execute();
    }

    /**
     * 解析系统配置
     * @throws IOException
     * @throws XTagNotFoundException
     */
    private void initSystemConfig() throws IOException, XTagNotFoundException {
        XConfiguration.getInstance().readConfig(this.getAssets().open(
                XConstant.PRE_INSTALL_SOURCE_ROOT + XConstant.CONFIG_FILE_NAME));
    }

    /**
     * 获得手机的deviceId
     * @return
     */
    protected String getKey() {
        // TODO:用更合理的方式来屏蔽4.2上获取不到deviceId的问题
        if (Build.VERSION.SDK_INT == ANDROID4_2_API_LEVEL) {
            return null;
        }
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String deviceID = tm.getDeviceId();
        return deviceID;
    }

    /**
     * 设置窗口的一些参数
     */
    private void setWindowStyle() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        // 根据平台配置决定是否显示标题栏
        if (isRequiredFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 添加一个子视图到Activity的content view，如果view是可见的，则view会被显示在屏幕上
     * @param view
     *            子视图
     */
    public void addView(XAppWebView view) {
        if (view instanceof View) {
            View subView = (View) view;
            subView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.FILL_PARENT));
            mContentView.addView(subView);
        }
    }

    /**
     * 从Activity的content view中remove掉一个子视图
     * @param view
     *            子视图
     */
    public void removeView(XAppWebView view) {
        if (view instanceof View) {
            mContentView.removeView((View) view);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        XEvent evt = XEvent.createEvent(XEventType.PAUSE);
        XSystemEventCenter.getInstance().sendEventSync(evt);
        MobclickAgent.onPause(this);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        XEvent evt = XEvent.createEvent(XEventType.RESUME);
        XSystemEventCenter.getInstance().sendEventSync(evt);
        MobclickAgent.onResume(this);
    }

    /**
     * 获取工作目录的名字
     * @return
     */
    protected String getWorkDirName() {
        String packageName = getPackageName();
        String workDir = XConfiguration.getInstance().getWorkDirectory(this,
                packageName);
        return workDir;
    }

    /**
     * 是否需要显示splash
     * @return
     */
    private boolean isRequiredSplash() {
        return XConfiguration.getInstance().readShowSplash();
    }

    /**
     * 是否需要显示标题栏
     */
    private boolean isRequiredFullScreen() {
        return XConfiguration.getInstance().readFullscreen();
    }

    /**
     * 创建内容view
     */
    private void createContentView() {
        mContentView = new RelativeLayout(this);
        mContentView.setBackgroundColor(Color.WHITE);
        setContentView(mContentView);
    }

    /**
     * 启动时如果已配置则显示默认的SplashScreen图片
     * @param imagePath
     *            图片的绝对路径
     */
    public void startBootSplashIfNeeded() {
        // TODO:将splash做成动态的，资源拷贝，解压，app预装等放在后台进行
        if (!isRequiredSplash()) {
            return;
        }
        startBootSplash();
    }

    /**
     * 检测更新
     */
    public void checkUpdateIfNeeded() {
        // 判断是否需要检测更新
        if (XConfiguration.getInstance().readUpdateCheck()) {
            // 执行需要检测更新
            String address = XConfiguration.getInstance().readUpdateAddress();
            if (null != address) {
                (new XApkUpdater(this)).execute(address);
            } else {
                XLog.e("error", "please set the update address");
            }
        }
    }

    @Override
    public void startBootSplash() {
        if (mBootSplashView != null) {
            mContentView.addView(mBootSplashView);
            return;
        }
        mBootSplashView = new RelativeLayout(this);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        mBootSplashView.setLayoutParams(layoutParams);
        ImageView view = new ImageView(this);
        view.setBackgroundResource(R.drawable.xface_logo);
        mBootSplashView.addView(view, layoutParams);
        if (null != mVersionText && null != mVersionParams) {
            mBootSplashView.addView(mVersionText, mVersionParams);
        }
        mContentView.addView(mBootSplashView);
    }

    @Override
    public void stopBootSplash() {
        if (mBootSplashView != null) {
            try {
                String time = XConfiguration.getInstance().readSplashDelay();
                if (null == time) {
                    XLog.w(CLASS_NAME, "Please config splash delay in config.xml!");
                } else {
                    Thread.sleep(Integer.parseInt(time));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mBootSplashView.removeAllViews();
            mContentView.removeView(mBootSplashView);
            mBootSplashView = null;
            setBootWebViewVisible();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean doDefaultAction = true;
        // 对一些键进行处理特殊，例如back键
        if (needKeyUpEventBeNotified(keyCode, event)) {
            doDefaultAction = mRuntime.getKeyListener().onKeyUp(keyCode, event);
        }
        return doDefaultAction ? super.onKeyUp(keyCode, event) : true;
    }

    /**
     * 判断keyup event是否需要被通知到引擎中
     */
    private boolean needKeyUpEventBeNotified(int keyCode, KeyEvent event) {
        /*
         * 对于back键，如果不是在引擎处于活动状态时按下（key down和key up要配对），
         * 例如关闭软键盘和其他Activity的back键，就不通知引擎。
         */
        return (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking() && !event
                .isCanceled()) || keyCode != KeyEvent.KEYCODE_BACK;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean doDefaultAction = true;
        if (null != mRuntime.getKeyListener()) {
            doDefaultAction = mRuntime.getKeyListener().onKeyDown(keyCode, event);
        } else {
            XLog.e("error", "runtime is not init!");
        }
        // 对于back键，在父类方法中会调用event.startTracking();
        return doDefaultAction ? super.onKeyDown(keyCode, event) : true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XEvent evt = XEvent.createEvent(XEventType.DESTROY);
        XSystemEventCenter.getInstance().sendEventSync(evt);
        System.exit(0);
    }

    /**
     * 启动一个activity,当此activity退出时会调用onActivityResult
     * @param listener
     *            用于确认在哪一个扩展中调用此方法
     * @param requestCode
     *            传人参数
     * @param intent
     *            启动Activity的intent
     */
    @Override
    public void startActivityForResult(XActivityResultListener listener,
            Intent intent, int requestCode) {
        registerActivityResultListener(requestCode, listener);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void registerActivityResultListener(int requestCode,
            XActivityResultListener listener) {
        mActivityResultListenerMap.put(Integer.valueOf(requestCode), listener);
    }

    /**
     * 由startActivityForResult启动的activity退出时调用
     * @param requestCode
     *            startActivityForResult 提供的值
     * @param resultCode
     *            由启动的activity中 setResult确定.
     * @param intent
     *            从新的Activity传过来的intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        XActivityResultListener callback = mActivityResultListenerMap
                .get(Integer.valueOf(requestCode));
        mActivityResultListenerMap.remove(Integer.valueOf(requestCode));
        if (callback != null) {
            callback.onActivityResult(requestCode, resultCode, intent);
        } else {
            XLog.e("error", "XActivityResultListener is null!");
        }
    }

    /**
     * 应用中调用显示指定的SplashScreen图片，如果指定的图片为空则加载默认图片
     * @param imagePath
     *            图片的绝对路径
     */
    public void startAppSplash(String imagePath) {
        // TODO:将splash做成动态的，资源拷贝，解压，app预装等放在后台进行
        stopAppSplash();
        mAppSplashView = new RelativeLayout(this);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        mAppSplashView.setLayoutParams(layoutParams);
        mAppSplashImage = XUtils.decodeBitmap(imagePath);
        if (mAppSplashImage == null) {
            /**如果前面获取到的图片为空，则默认采用xface_logo*/
            InputStream is = getActivity().getResources().openRawResource(R.drawable.xface_logo);
            mAppSplashImage = XUtils.decodeBitmap(is);
        }
        mAppSplashView.setBackgroundDrawable(new BitmapDrawable( mAppSplashImage));
        mContentView.addView(mAppSplashView);
    }

    /**
     * 应用停止显示SplashScreen图片
     */
    public void stopAppSplash() {
        if (mAppSplashView != null) {
            mContentView.removeView(mAppSplashView);
            mAppSplashView = null;
            if (mAppSplashImage != null) {
                if (!mAppSplashImage.isRecycled()) {
                    mAppSplashImage.recycle();
                }
                mAppSplashImage = null;
            }
        }
    }

    @Override
    public boolean isBootSplashShowing() {
        return mBootSplashView != null;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void waitingDialogForAppStart() {
        if (isBootSplashShowing()) {
            return;
        }
        mWaitingNotification.activityStart(
                XStrings.getInstance()
                        .getString(XStrings.WAITING_MESSAGE_TITLE),
                XStrings.getInstance().getString(
                        XStrings.WAITING_MESSAGE_CONTENT));
    }

    @Override
    public void waitingDialogForAppStartFinished() {
        if (mBootSplashView != null) {
            /**根据config.xml的配置决定是否自动隐藏SplashScreen*/
            if(XConfiguration.getInstance().readAutoHideSplash()) {
                stopBootSplash();
            }
            return;
        }
        mWaitingNotification.activityStop();
    }

    @Override
    public void toast(String message) {
        mWaitingNotification.toast(message);
    }

    @Override
    public XStartParams getStartParams() {
        return mStartParams;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    /**
     * 初始化UMeng统计代理
     */
    protected void initMobclickAgent()
    {
        //注意：这里需要将SessionContinue的设置为0，不然连续启动程序的间隔时间果断的话，不会发送数据
        MobclickAgent.setSessionContinueMillis(0);
        MobclickAgent.onError(this);
    }

    /**
     * 设置BootWebView可见
     */
    private void setBootWebViewVisible() {
        int childCount = mContentView.getChildCount();
        int i = 0;
        while( i < childCount) {
            View view = mContentView.getChildAt(i);
            if(view instanceof XAppWebView) {
                view.setVisibility(View.VISIBLE);
                view.requestFocus();
                break;
            }
            i++;
        }
    }

    @Override
    public boolean isSplashShowing() {
        return null != mBootSplashView;
    }

    @Override
    public XSecurityPolicy getSecurityPolily() {
        return mSecurityPolicy;
    }
}
