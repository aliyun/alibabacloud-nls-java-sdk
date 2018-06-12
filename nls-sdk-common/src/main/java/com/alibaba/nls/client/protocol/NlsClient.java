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

import com.alibaba.nls.client.transport.Connection;
import com.alibaba.nls.client.transport.ConnectionListener;
import com.alibaba.nls.client.transport.netty4.NettyWebSocketClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhishen on 2017/11/2.
 * 语音处理client,全局维护一个实例即可
 */
public class NlsClient {
    static Logger logger = LoggerFactory.getLogger(NlsClient.class);
    private static final String DEFAULT_SERVER_ADDR = "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1";
    NettyWebSocketClient client;
    String token;
    /**
     * 连接建立默认超时时间,单位毫秒
     */
    public int DEFAULT_CONNECTION_TIMEOUT = 5000;

    /**
     * 传入accessToken,访问阿里云线上服务
     *
     * @param token
     */
    public NlsClient(String token) {
        try {
            this.token = token;
            client = new NettyWebSocketClient(DEFAULT_SERVER_ADDR);
        } catch (Exception e) {
            logger.error("fail to create NlsClient", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 传入accessToken,根据传入url访问指定环境的服务
     *
     * @param url
     * @param token
     */
    public NlsClient(String url, String token) {
        try {
            this.token = token;
            client = new NettyWebSocketClient(url);
        } catch (Exception e) {
            logger.error("fail to create NlsClient", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新token.token有有效期,过了有效期需要设置新的token
     *
     * @param token
     */
    public void setToken(String token) {
        this.token = token;
    }

    public Connection connect(ConnectionListener listener) {
        return client.connect(token, listener, DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * 在应用的最后调用此方法,释放资源
     */
    public void shutdown() {
        client.shutdown();
    }
}
