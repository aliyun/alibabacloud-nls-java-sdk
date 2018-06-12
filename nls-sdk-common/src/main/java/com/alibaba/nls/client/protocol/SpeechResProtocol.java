/*
 * Copyright 2015 Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nls.client.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhishen on 2017/11/27.
 *
 * 服务端应答基础协议类
 */
public class SpeechResProtocol {
    public Map<String, Object> header = new HashMap<String, Object>();
    public Map<String, Object> payload = new HashMap<String, Object>();

    public String getNameSpace() {
        return (String)header.get(Constant.PROP_NAMESPACE);
    }

    public String getName() {
        return (String)header.get(Constant.PROP_NAME);
    }

    public int getStatus() {
        return (Integer)header.get(Constant.PROP_STATUS);
    }

    public String getStatusText() {
        return (String)header.get(Constant.PROP_STATUS_TEXT);
    }
}
