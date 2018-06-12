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

package com.alibaba.nls.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;

/**
 * 访问令牌是用户访问智能语音服务的凭证
 *
 * @author xuebin
 */
public class AccessToken {
    private String token;
    private long expireTime;

    /**
     * @return
     */
    public String getToken() {
        return token;
    }

    public long getExpireTime() {
        return expireTime;
    }

    private AccessToken(String token, long expireTime) {
        this.token = token;
        this.expireTime = expireTime;
    }

    /**
     * 从阿里云服务申请访问令牌
     *
     * @param akId      access key id
     * @param akSecrete access key secret
     * @return 访问令牌(AccessToken)
     * @throws ClientException
     */
    public static AccessToken apply(String akId, String akSecrete) throws ClientException {
        // 创建DefaultAcsClient实例并初始化
        DefaultProfile profile = DefaultProfile.getProfile(
            "cn-shanghai",          // 您的地域ID
            akId,      // 您的Access Key ID
            akSecrete); // 您的Access Key Secret
        IAcsClient client = new DefaultAcsClient(profile);
        CommonRequest request = new CommonRequest();
        request.setDomain("nls-meta.cn-shanghai.aliyuncs.com");
        request.setUriPattern("/pop/2018-05-18/tokens");
        request.setMethod(MethodType.POST);
        CommonResponse response = client.getCommonResponse(request);
        if (response.getHttpStatus() == 200) {
            JSONObject result = JSON.parseObject(response.getData());
            System.out.println(response.getData());
            return new AccessToken(result.getJSONObject("Token").getString("Id"), result
                .getJSONObject("Token").getLongValue("ExpireTime"));
        } else {
            throw new ClientException(String.format("Got token response: status=%d, body=%s",
                response.getHttpStatus(), response.getData()));
        }
    }
}
