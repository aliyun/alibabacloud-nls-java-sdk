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

import com.alibaba.nls.client.transport.ConnectionListener;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private static Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    public void setListener(ConnectionListener listener) {
        this.listener = listener;
    }

    ConnectionListener listener;

    private WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public WebSocketClientHandler() {

    }

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    public void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        logger.debug("handler added channelid:{}", ctx.channel().id());
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.debug("channel active,id:{},{}", ctx.channel().id(), Thread.currentThread().getId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (ctx.channel() != null) {
            logger.debug("channelInactive:" + ctx.channel().id());
        } else {
            logger.debug("channelInactive");
        }
        if (listener != null) {
            listener.onClose(-1, "channelInactive");
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                FullHttpResponse response = (FullHttpResponse)msg;
                handshaker.finishHandshake(ch, response);
                handshakeFuture.setSuccess();
                logger.info("WebSocket Client connected! response headers[sec-websocket-extensions]:{}",
                    response.headers().get("sec-websocket-extensions"));
            } catch (WebSocketHandshakeException e) {
                FullHttpResponse res = (FullHttpResponse)msg;
                String errorMsg = String.format("WebSocket Client failed to connect,status:%s,reason:%s", res.status(),
                    res.content().toString(CharsetUtil.UTF_8));
                logger.error(errorMsg);
                handshakeFuture.setFailure(new Exception(errorMsg));
            }
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse)msg;
            listener.onFail(response.status().code(), response.content().toString(CharsetUtil.UTF_8));
            throw new IllegalStateException(
                "Unexpected FullHttpResponse (getStatus=" + response.status() +
                    ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame)msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame)frame;
            listener.onMessage(textFrame.text());
        } else if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binFrame = (BinaryWebSocketFrame)frame;
            listener.onMessage(binFrame.content().nioBuffer());
        } else if (frame instanceof PongWebSocketFrame) {
            logger.debug("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            logger.debug("receive close frame");
            listener.onClose(((CloseWebSocketFrame)frame).statusCode(), ((CloseWebSocketFrame)frame).reasonText());
            ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        listener.onError(cause);
        logger.error("error", cause);
        ctx.close();
    }
}
