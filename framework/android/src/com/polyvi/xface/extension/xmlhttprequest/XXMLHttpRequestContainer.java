
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

package com.polyvi.xface.extension.xmlhttprequest;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;

/**
 * ajax容器 维护所有的ajax对象
 */
public class XXMLHttpRequestContainer {
    /**
     * ajax 管理map
     */
    private ConcurrentHashMap<String, XXMLHttpRequest> mAjaxMap = new ConcurrentHashMap<String, XXMLHttpRequest>();
    private Context mContext;

    public XXMLHttpRequestContainer(Context context) {
        mContext = context;
    }

    /**
     * 删除所有的请求对象
     */
    public void removeAllRequestObj() {
        // 断开监听
        Set<Entry<String, XXMLHttpRequest>> entrys = mAjaxMap.entrySet();
        Iterator<Entry<String, XXMLHttpRequest>> entry = entrys.iterator();
        while (entry.hasNext()) {
            Entry<String, XXMLHttpRequest> e = entry.next();
            XXMLHttpRequest xhr = e.getValue();
            xhr.setRequestListener(null);
        }
        //清除所有的ajax对象
        mAjaxMap.clear();
    }

    /**
     * 增加ajax对象到map中 如果ajax对象已经存在 则直接返回
     * 
     * @param id
     */
    public XXMLHttpRequest getXMLRequestObj(String id) {
        if (!mAjaxMap.containsKey(id)) {
            XXMLHttpRequest xhr = new XXMLHttpRequest(mContext);
            mAjaxMap.put(id, xhr);
        }
        return mAjaxMap.get(id);
    }

}
