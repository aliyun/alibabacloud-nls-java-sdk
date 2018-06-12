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

import com.alibaba.fastjson.JSON;
import com.alibaba.nls.client.util.IdGen;

/**
 * Created by zhishen on 2017/11/24.
 *
 * 语音请求基础协议类
 */
public class SpeechReqProtocol {
    protected String accessToken;

    public Map<String, String> header = new HashMap<String, String>();
    public Map<String, Object> payload;
    public Map<String, Object> context = new HashMap<String, Object>();

    public SpeechReqProtocol() {
        header.put(Constant.PROP_MESSAGE_ID, IdGen.genId());
        SdkInfo sdk = new SdkInfo();
        sdk.version = Constant.sdkVersion;
        context.put(Constant.PROP_CONTEXT_SDK, sdk);
    }

    public String getAppKey() {
        return header.get(Constant.PROP_APP_KEY);
    }

    public void setAppKey(String appKey) {
        header.put(Constant.PROP_APP_KEY, appKey);
    }

    public String getTaskId() {
        return header.get(Constant.PROP_TASK_ID);
    }

    protected void setTaskId(String requestId) {
        header.put(Constant.PROP_TASK_ID, requestId);
    }

    public void putContext(String key, Object obj) {
        context.put(key, obj);
    }

    public void addCustomedParam(String key, Object value) {
        payload.put(key, value);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String serialize() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("header", header);
        if (payload != null) {
            result.put("payload", payload);
            result.put("context", context);
        }
        return JSON.toJSONString(result);
    }

}
