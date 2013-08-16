
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

package com.polyvi.xface.extension.push;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.NotificationIQ;
import org.jivesoftware.smack.NotificationIQProvider;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.ProviderManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;

/**
 * 该类主要实现了客户端对服务器端的连接操作
 */
public class XConnectionManager {

    /** 程序客户端名 */
    private static final String XFACE_CLIENT_NAME = "XfaceApolloClient";

    /** 保存信息的文件名 */
    private static final String SHARED_PREFERENCE_NAME = "push_preferences";

    /** 登录服务器账号的保存名 */
    private static final String USERNAME = "USERNAME";

    /** 登陆服务器密码的保存名 */
    private static final String PASSWORD = "PASSWORD";

    /** 收到通知的xml文件的元素名 */
    private static final String ELEMENT_NAME = "notification";

    /** 收到服务器发过来的通知的xml文件的xmlns的名字，这样才能区分 */
    private static final String NAME_SPACE = "androidpn:iq:notification";

    /** 分隔符 */
    private static final String SEPARATE_SIGN = "_";

    /** 客户端用户名 */
    private static final String KEY_USERNAME = "username";

    /** 客户端密码 */
    private static final String KEY_PASSWORD = "password";

    /** 连接错误类型 */
    private static final String INVALID_CREDENTIALS_ERROR_CODE = "401";

    /** service */
    private XNotificationService mNotificationService;

    private SharedPreferences mSharedPrefs;

    private XMPPConnection mConnection;

    /** 客户端用户名 */
    private String mUsername;

    /** 客户端密码 */
    private String mPassword;

    /** 连接监听 */
    private ConnectionListener mConnectionListener;
    
    private ExecutorService mExecutorService;

    public XConnectionManager(XNotificationService notificationService) {
        init(notificationService);
    }

    /**
     * 初始化操作
     * @param notificationService service
     */
    private void init(XNotificationService notificationService) {
        mSharedPrefs = notificationService.getSharedPreferences(
                SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        mNotificationService = notificationService;
        mUsername = mSharedPrefs.getString(USERNAME, "");
        mPassword = mSharedPrefs.getString(PASSWORD, "");
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 对连接出错监听的初始化以及实现重连
     */
    private void initConnectionListener() {
        if (null == mConnectionListener) {
            mConnectionListener = new ConnectionListener() {
                @Override
                public void connectionClosedOnError(Exception arg0) {
                    if (mConnection != null && mConnection.isConnected()) {
                        mConnection.disconnect();
                    }
                    startReconnection();
                }

                @Override
                public void reconnectingIn(int arg0) {
                }

                @Override
                public void reconnectionFailed(Exception arg0) {
                }

                @Override
                public void reconnectionSuccessful() {
                }

                @Override
                public void connectionClosed() {
                }
            };
        }
    }

    /**
     * 连接服务器
     */
    public void connect() {
        Runnable connectRunnable = new Runnable() {

            @Override
            public void run() {
                initConnect();
                register();
                login();
            }
        };
        mExecutorService.submit(connectRunnable);
    }

    /**
     * 通过主机名和端口号去对服务器进行连接和一些初始化
     */
    private void initConnect() {
        if (!isConnected()) {
            String connectionHost = mNotificationService.getHost();
            int connectionPort = Integer.parseInt(mNotificationService
                    .getPort());
            ConnectionConfiguration connConfig = new ConnectionConfiguration(
                    connectionHost, connectionPort);
            connConfig.setSecurityMode(SecurityMode.required);
            connConfig.setSASLAuthenticationEnabled(false);
            connConfig.setCompressionEnabled(false);
            mConnection = new XMPPConnection(connConfig);
            try {
                mConnection.connect();
                ProviderManager.getInstance().addIQProvider(ELEMENT_NAME,
                        NAME_SPACE, new NotificationIQProvider());
            } catch (XMPPException e) {
                startReconnection();
            }
        }
    }

    /**
     * 首次连接的时候需要去注册账号,将账号保存,然后去执行注册
     */
    private void register() {
        if (!isRegistered()) {
            // 第一次登陆以uuid和报名作为用户名,包名作为密码,这样就能很好的区别
            // 因为uuid可以区别设备而包名可以区别应用。这样用户名就不会有相同的存在，
            // 这样通知就能准确的发送到要发送指定的设备和应用之上。
            mUsername = createUserName();
            mPassword = mNotificationService.getPackageName();
            Registration registration = new Registration();
            PacketFilter packetFilter = new AndFilter(new PacketIDFilter(
                    registration.getPacketID()), new PacketTypeFilter(IQ.class));
            PacketListener packetListener = new PacketListener() {
                public void processPacket(Packet packet) {
                    if (packet instanceof IQ) {
                        IQ response = (IQ) packet;
                        if (response.getType() == IQ.Type.RESULT) {
                            Editor editor = mSharedPrefs.edit();
                            editor.putString(USERNAME, mUsername);
                            editor.putString(PASSWORD, mPassword);
                            editor.commit();
                        }
                    }
                }
            };
            mConnection.addPacketListener(packetListener, packetFilter);
            registration.setType(IQ.Type.SET);
            registration.addAttribute(KEY_USERNAME, mUsername);
            registration.addAttribute(KEY_PASSWORD, mPassword);
            mConnection.sendPacket(registration);
        }
    }

    /**
     * 通过用户名密码去登陆服务器,连接服务器之后在设定时间内断开连接然后又去重连,这样就实现了循环断开连接
     */
    private void login() {
        if (!isAuthenticated()) {
            try {
                mConnection.login(mUsername, mPassword, XFACE_CLIENT_NAME);
                initConnectionListener();
                mConnection.addConnectionListener(mConnectionListener);
                PacketFilter packetFilter = new PacketTypeFilter(
                        NotificationIQ.class);
                mConnection.addPacketListener(
                        mNotificationService.getNotificationPacketListener(),
                        packetFilter);
            } catch (XMPPException e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null
                        && errorMessage
                                .contains(INVALID_CREDENTIALS_ERROR_CODE)) {
                    deleteAccount();
                    startReconnection();
                    return;
                }
                startReconnection();
            } catch (Exception e) {
                startReconnection();
            }
        }
    }

    /**
     * 断开服务器
     */
    public void disconnect() {
        if (isConnected()) {
            mConnection.removePacketListener(mNotificationService
                    .getNotificationPacketListener());
            mConnection.disconnect();
        }
    }

    /**
     * 重连服务器
     */
    private void startReconnection() {
        connect();
    }

    /**
     * 当401错误发生去执行删除账号
     */
    private void deleteAccount() {
        Editor editor = mSharedPrefs.edit();
        editor.remove(USERNAME);
        editor.remove(PASSWORD);
        editor.commit();
    }

    /**
     * 判断是否对服务器进行连接
     * @return true:已经连接,false:未连接
     */
    private boolean isConnected() {
        return mConnection != null && mConnection.isConnected();
    }

    /**
     * 判断是否登陆服务器
     * @return true:已经登陆,false:未登录
     */
    private boolean isAuthenticated() {
        return isConnected()&& mConnection.isAuthenticated();
    }

    /**
     * 判断是否注册用户名和密码
     * @return true:已经注册,false:未注册
     */
    private boolean isRegistered() {
        return mSharedPrefs.contains(USERNAME)
                && mSharedPrefs.contains(PASSWORD);
    }

    /**
     * 创建用户登陆的用户名
     * @return 用户名
     */
    private String createUserName() {
        return Settings.Secure.getString(
                mNotificationService.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID)
                + SEPARATE_SIGN
                + mNotificationService.getPackageName();
    }
}