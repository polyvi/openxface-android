
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

import com.polyvi.xface.app.XAppInfo;
import com.polyvi.xface.util.XLog;

/**
 * 用来解析不含schema标签的应用配置文件
 */
public class XAppConfigParserNoSchema extends XAbstractAppConfigParser {
    private static final String CLASS_NAME = XAppConfigParserNoSchema.class.getSimpleName();
    public XAppConfigParserNoSchema(Document doc) {
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
            parseExtensionsTag();
            parseDistributionTag();
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
        Element appElement = getElementByTagName(mDoc,TAG_APP);
        mAppInfo.setAppId(appElement.getAttribute(ATTR_ID));
        mAppInfo.setVersion(appElement.getAttribute(ATTR_VERSION));
        mAppInfo.setWidth(Integer.valueOf(appElement.getAttribute(ATTR_WIDTH)));
        mAppInfo.setHeight(Integer.valueOf(appElement.getAttribute(ATTR_HEIGHT)));
        mAppInfo.setEncrypted(Boolean.valueOf(appElement.getAttribute(ATTR_IS_ENCRYPTED)));
    }

    /**
     * 解析app.xml中的<description>标签
     *
     * @throws XTagNotFoundException
     */
    protected void parseDescriptionTag() throws XTagNotFoundException {
        Element descriptionElement = (Element) mDoc.getElementsByTagName(TAG_DESCRIPTION).item(0);
        mAppInfo.setName(getElementValueByNode(descriptionElement, TAG_NAME));
        mAppInfo.setType(getElementValueByAttribute(descriptionElement, TAG_TYPE, ATTR_VALUE));
        mAppInfo.setIcon(getElementValueByAttribute(descriptionElement, TAG_ICON, ATTR_SRC));
        mAppInfo.setEntry(getElementValueByAttribute(descriptionElement, TAG_ENTRY, ATTR_SRC));
        try {
            mAppInfo.setRunModeConfig(getElementValueByAttribute(
                    descriptionElement,
                    TAG_APP_RUNNING_MODE,
                    ATTR_VALUE));
        } catch (XTagNotFoundException e) {
            mAppInfo.setRunModeConfig(null);
        }
    }

    protected void  parseDistributionTag() throws XTagNotFoundException{
    }
}
