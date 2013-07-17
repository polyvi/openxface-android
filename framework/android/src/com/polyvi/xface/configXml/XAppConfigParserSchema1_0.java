
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

package com.polyvi.xface.configXml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.polyvi.xface.app.XAppInfo;
import com.polyvi.xface.app.XApplicationCreator;
import com.polyvi.xface.app.XWhiteList;
import com.polyvi.xface.util.XLog;

/**
 * 用来解析schema为1.0的应用配置文件
 */
public class XAppConfigParserSchema1_0 extends XAbstractAppConfigParser {
    private static final String CLASS_NAME = XAppConfigParserSchema1_0.class
            .getSimpleName();


    public XAppConfigParserSchema1_0(Document doc) {
        super();
        mDoc = doc;
    }

    /**
     * 统一调用的解析接口
     *
     * @return 返回一个XAppInfo对象
     */
    public XAppInfo parseConfig() {
        try {
            parseAppTag();
            parseDescriptionTag();
            parseDistributionTag();
            parseSecurityTag();
            // TODO:临时代码实现,这个地方不应该就为空就抛出异常
            if (!XApplicationCreator.NATIVE_APP_TYPE.equals(mAppInfo.getType())) {
                parseExtensionsTag();
            }
            return mAppInfo;
        } catch (XTagNotFoundException e) {
            XLog.e(CLASS_NAME, e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解析app.xml中的<app>标签
     *
     * @throws XTagNotFoundException
     */
    protected void parseAppTag() throws XTagNotFoundException {
        Element appElement = getElementByTagName(mDoc,
                TAG_APP);
        mAppInfo.setAppId(appElement
                .getAttribute(ATTR_ID));
    }

    /**
     * 解析app.xml中的<description>标签
     *
     * @throws XTagNotFoundException
     */
    protected void parseDescriptionTag() throws XTagNotFoundException {
        /** 解析description标签 */
        Element descriptionElement = getElementByTagName(mDoc,
                TAG_DESCRIPTION);
        mAppInfo.setName(getElementValueByNode(descriptionElement,
                TAG_NAME));
        mAppInfo.setType(getElementValueByNode(descriptionElement,
                TAG_TYPE));
        mAppInfo.setIconBackgroudColor(getElementValueByAttribute(descriptionElement,
                TAG_ICON,
                ATTR_BACKGROUND_COLOR));
        mAppInfo.setIcon(getElementValueByAttribute(descriptionElement,
                TAG_ICON,
                ATTR_SRC));
        mAppInfo.setEntry(getElementValueByAttribute(descriptionElement,
                TAG_ENTRY,
                ATTR_SRC));
        mAppInfo.setVersion(getElementValueByNode(descriptionElement,
                TAG_VERSION));
        if (!XApplicationCreator.NATIVE_APP_TYPE.equals(mAppInfo.getType())) {
            Element displayElement = getElementByTagName(descriptionElement,
                    TAG_DISPLAY);
            mAppInfo.setDisplayMode(getElementValueByAttribute(
                    descriptionElement, TAG_DISPLAY,
                    ATTR_TYPE));
            mAppInfo.setWidth(Integer.valueOf(getElementValueByNode(
                    displayElement, TAG_WIDTH)));
            mAppInfo.setHeight(Integer.valueOf(getElementValueByNode(
                    displayElement, TAG_HEIGHT)));
            mAppInfo.setEngineType(getElementValueByNode(descriptionElement,
                    TAG_RUNTIME));
            try {
                mAppInfo.setRunModeConfig(getElementValueByAttribute(
                        descriptionElement,
                        TAG_APP_RUNNING_MODE,
                        ATTR_VALUE));
            } catch (XTagNotFoundException e) {
                mAppInfo.setRunModeConfig(null);
            }
        }
    }

    /**
     * 解析app.xml中的<distribution>标签
     *
     * @throws XTagNotFoundException
     */
    protected void parseDistributionTag() throws XTagNotFoundException {
        Element distributionElement = getElementByTagName(mDoc,
                TAG_DISTRIBUTION);
        if (!XApplicationCreator.NATIVE_APP_TYPE.equals(mAppInfo.getType())) {
            Element packageElement = getElementByTagName(distributionElement,
                    TAG_PACKAGE);
            mAppInfo.setEncrypted(Boolean.valueOf(getElementValueByNode(
                    packageElement, TAG_ENCRYPT)));
        }
        Element channelElement = getElementByTagName(distributionElement,
                TAG_CHANNEL);
        mAppInfo.setChannelId(getElementValueByAttribute(distributionElement,
                TAG_CHANNEL,
                ATTR_ID));
        mAppInfo.setChannelName(getElementValueByNode(channelElement,
                TAG_NAME));
    }

    /**
     * 解析<access>标签
     *
     * @throws XTagNotFoundException
     */
    protected void parseSecurityTag() {
        try {
             Element appElement = getElementByTagName(mDoc,
                    TAG_APP);
             NodeList accessList = getNodeListByTagName(appElement,
                    TAG_ACCESS);
             int len = accessList.getLength();
             XWhiteList appWhiteList = new XWhiteList();
             mAppInfo.setWhiteList(appWhiteList);
             for (int i = 0; i < len; i++) {
                 Element  item = (Element)accessList.item(i);
                 appWhiteList.addWhiteListEntry(item.getAttribute(TAG_ORIGIN),
                         item.getAttribute(TAG_SUBDOMAINS));
             }
        } catch (XTagNotFoundException e) {
            XLog.w(CLASS_NAME, "access tag is absent", e);
        }
    }
}
