
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

package com.polyvi.xface.extension.trafficstats;

import org.json.JSONArray;
import org.json.JSONException;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.plugin.api.XIWebContext;

/**
 * 流量统计扩展模块，用获取流量的相关信息，获取的流量
 * 是从应用启动到调用接口其间的流量数据
 */
public class XTrafficStatsExt extends XExtension {

    private static final String COMMAND_GETMOBILETRAFFIC = "getMobileTraffic";
    private static final String COMMAND_GETWIFITRAFFIC = "getWifiTraffic";
    /** 负责统计和更新流量数据 */
    private XTrafficStats mTrafficStatic;

    @Override
    public void init(XExtensionContext extensionContext, XIWebContext webContext) {
        super.init(extensionContext, webContext);
        mTrafficStatic = new XTrafficStats(getContext());
    }

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }

    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
          if (action.equals(COMMAND_GETMOBILETRAFFIC)) {
              return new XExtensionResult(Status.OK,mTrafficStatic.getMobileTraffic() );
          }
          else if(action.equals(COMMAND_GETWIFITRAFFIC))
          {
              return new XExtensionResult(Status.OK, mTrafficStatic.getWifiTraffic());
          }
          else
          {
              return new XExtensionResult(Status.INVALID_ACTION,"");
          }
    }

    @Override
    public void destroy() {
        mTrafficStatic.destroy();
    }

}
