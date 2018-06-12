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

package com.alibaba.nls.client.transport.netty4;

import com.alibaba.nls.client.transport.Connection;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhishen on 2017/11/27.
 */
public class NettyConnection implements Connection {
    static Logger logger = LoggerFactory.getLogger(NettyConnection.class);
    Channel channel;

    public NettyConnection(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String getId() {
        if (channel != null) {
            return channel.id().toString();
        }
        return null;

    }

    @Override
    public void close() {
        channel.close();
    }

    @Override
    public void sendText(final String payload) {
        logger.debug("thread:{},send:{}", Thread.currentThread().getId(), payload);
        TextWebSocketFrame frame = new TextWebSocketFrame(payload);
        channel.writeAndFlush(frame);

    }

    @Override
    public void sendBinary(byte[] payload) {
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(payload));
        channel.writeAndFlush(frame);

    }
}
