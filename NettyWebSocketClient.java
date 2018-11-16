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

import java.net.URI;

import com.alibaba.nls.client.protocol.Constant;
import com.alibaba.nls.client.transport.Connection;
import com.alibaba.nls.client.transport.ConnectionListener;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhishen.ml
 * @date 2017/11/27
 *
 */
public final class NettyWebSocketClient {
    private static Logger logger = LoggerFactory.getLogger(NettyWebSocketClient.class);
    private URI websocketURI;
    private int port;
    private SslContext sslCtx;
    EventLoopGroup group = new NioEventLoopGroup(0);
    Bootstrap bootstrap = new Bootstrap();

    public NettyWebSocketClient(final String uriStr) throws Exception {
        this.websocketURI = new URI(uriStr);
        final boolean ssl = "wss".equalsIgnoreCase(websocketURI.getScheme());
        port = websocketURI.getPort();
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            if (port == -1) {
                port = 443;
            }
        }
        final String isCompression = System.getProperty("nls.ws.compression", "false");
        bootstrap.option(ChannelOption.TCP_NODELAY, true)
            .group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                if (sslCtx != null) {
                    p.addLast(sslCtx.newHandler(ch.alloc(), websocketURI.getHost(), 443));
                }
                if ("true".equalsIgnoreCase(isCompression)) {
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192),
                        WebSocketClientCompressionHandler.INSTANCE);
                } else {
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192));
                }

                p.addLast("hookedHandler", new WebSocketClientHandler());

            }
        });

    }

    public Connection connect(String token, ConnectionListener listener, int connectionTimeout) throws Exception {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout);
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.set(Constant.HEADER_TOKEN, token);
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory
            .newHandshaker(websocketURI, WebSocketVersion.V13, null, true, httpHeaders);

        Channel channel = bootstrap.connect(websocketURI.getHost(), port).sync().channel();
        logger.debug("websocket channel is established after sync,connectionId:{}", channel.id());
        WebSocketClientHandler handler = (WebSocketClientHandler)channel.pipeline().get("hookedHandler");
        handler.setListener(listener);
        handler.setHandshaker(handshaker);
        handshaker.handshake(channel);
        handler.handshakeFuture().sync();
        logger.debug("websocket connection is established after handshake,connectionId:{}", channel.id());
        return new NettyConnection(channel);
    }

    public void shutdown() {
        group.shutdownGracefully();
    }

}
