
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

package com.polyvi.xface.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XStringUtils;

public class XWhiteList {
    private static final String CLASS_NAME = XWhiteList.class.getSimpleName();
    /**用来白名单匹配的正则表达式*/
    private static final String HTTP_SCHEME        = "http";
    private static final String HTTPS_SCHEME       = "https?://";
    private static final String HTTPS_SCHEME_START = "^https?://";
    private static final String HTTP_SUBDOMAINS    = "^https?://(.*\\\\.)?";//匹配子域名
    /**应用访问白名单匹配数组 **/
    private ArrayList<Pattern> mWhiteList = new ArrayList<Pattern>();
    /**应用访问白名单的历史缓存，用于提高检查速度*/
    private HashMap<String, Boolean> mWhiteListCache = new HashMap<String, Boolean>();
    /**标示访问url是否无限制*/
    private boolean mAccessNoLimit = true;

    /**
     * 添加允许访问的URL (白名单)
     *
     * @param origin        允许访问的URL正则表达式
     * @param subdomains    true:允许访问origin下所有子域名
     */
    public void addWhiteListEntry(String origin, String subdomains) {
        if(XStringUtils.isEmptyString(origin)){
            XLog.w(CLASS_NAME, "origin attribute is absent in access");
            return;
        }
        mAccessNoLimit = false;
        mWhiteList.add(Pattern.compile(getRexPattern(origin,
                (subdomains != null) && (subdomains.compareToIgnoreCase("true") == 0))));
    }

    /**
     * 检查url是否在白名单中
     *
     * @param url : 要检查的url
     * @return true：在白名单中，false：不在白名单中
     */
    public boolean isUrlWhiteListed(String url) {
        /** 如果对访问无限制，则直接返回true */
        if(mAccessNoLimit) {
            return true;
        }
        /** 首先在白名单缓存中检查是否存在url，如果有就直接返回true */
         if (mWhiteListCache.get(url) != null) {
            return true;
        }
        /** 检查白名单 */
        Iterator<Pattern> pit = mWhiteList.iterator();
        while (pit.hasNext()) {
            Pattern p = pit.next();
            Matcher m = p.matcher(url);
            /** 如果找到就在白名单缓存中放入url，这有助于加快查找速度 */
            if (m.find()) {
                mWhiteListCache.put(url, true);
                return true;
            }
        }
        return false;
    }

    /**
     * 根据传入的origin和subdomains获取要编译的正则表达式
     * @param origin:域名
     * @param subdomains:origin的子域名是否允许被访问
     * @return 要编译的正则表达式
     */
    private String getRexPattern(String origin, boolean subdomains) {
        /**如果origin为"*",则表示对访问的URL没有限制*/
        if (origin.compareTo("*") == 0) {
            XLog.d(CLASS_NAME, "Unlimited access to network resources");
            return ".*";
        } else {
            /**检查是否子域名也允许访问*/
            if (subdomains) {
                XLog.d(CLASS_NAME, "Origin to allow with subdomains: %s", origin);
                /**如果URL协议开头没有加上http,则自动加上*/
                return origin.startsWith(HTTP_SCHEME) ?
                        origin.replaceFirst(HTTPS_SCHEME, HTTP_SUBDOMAINS) :
                            HTTP_SUBDOMAINS + origin;
            } else {
                XLog.d(CLASS_NAME, "Origin to allow: %s", origin);
                /**如果URL协议开头没有加上http,则自动加上*/
                return origin.startsWith(HTTP_SCHEME) ?
                        origin.replaceFirst(HTTPS_SCHEME, HTTPS_SCHEME_START) :
                            HTTPS_SCHEME_START + origin;
            }
        }
    }
}
