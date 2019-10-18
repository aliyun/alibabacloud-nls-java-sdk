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

package com.alibaba.nls.client.transport;

/**
 * @author zhishen.ml
 * @date 2017/11/02
 *
 */

import java.nio.ByteBuffer;

/**
 * Simple interface to be implemented in order to react on the given WebSocket events
 */
public interface ConnectionListener {

    /**
     * Invoked once the connection to the remote URL has been established.
     */
    void onOpen();

    /**
     * Invoked after the connection was closed.
     *
     * @param closeCode the RFC 6455 status code
     * @param reason    a string description for the reason of the close
     */
    void onClose(int closeCode, String reason);

    /**
     * Invoked on arrival of a text message.
     *
     * @param message the text message.
     */
    void onMessage(String message);

    /**
     * Invoked on arrival of a binary message.
     *
     * @param message the binary message.
     */
    void onMessage(ByteBuffer message);
}
