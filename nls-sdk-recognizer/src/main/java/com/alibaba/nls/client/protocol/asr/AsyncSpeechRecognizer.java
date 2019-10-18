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

package com.alibaba.nls.client.protocol.asr;

import com.alibaba.nls.client.protocol.Constant;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SpeechReqProtocol;
import com.alibaba.nls.client.util.IdGen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.nls.client.protocol.Constant.VALUE_NAMESPACE_ASR;
import static com.alibaba.nls.client.protocol.Constant.VALUE_NAME_ASR_STOP;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_CLOSED;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_COMPLETE;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_FAIL;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_REQUEST_CONFIRMED;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_REQUEST_SENT;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_STOP_SENT;

/**
 * @author zhishen.ml
 * @date 2017/11/24
 *
 * 语音识别器,用于设置及发送识别请求,处理识别结果回调
 * 非线程安全
 */
public class AsyncSpeechRecognizer extends SpeechRecognizer {
    static Logger logger = LoggerFactory.getLogger(AsyncSpeechRecognizer.class);

    public AsyncSpeechRecognizer(NlsClient client, SpeechRecognizerListener listener) throws Exception {
        super(client, listener);
    }

    public AsyncSpeechRecognizer(NlsClient client, String token, SpeechRecognizerListener listener) throws Exception {
        super(client, token, listener);
    }

    /**
     * 内部调用方法
     */
    @Override
    void markReady() {
        state = STATE_REQUEST_CONFIRMED;

    }

    /**
     * 内部调用方法
     */
    @Override
    void markComplete() {
        state = STATE_COMPLETE;

    }

    /**
     * 内部调用方法
     */
    @Override
    void markFail() {
        state = STATE_FAIL;

    }

    /**
     * 内部调用方法
     */
    @Override
    void markClosed() {
        state = STATE_CLOSED;

    }

    /**
     * 开始语音识别:发送识别请求,同步接收服务端确认
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        state.checkStart();
        String taskId = IdGen.genId();
        currentTaskId = taskId;
        this.setTaskId(taskId);
        conn.sendText(this.serialize());
        state = STATE_REQUEST_SENT;

    }

    /**
     * 结束语音识别:发送结束识别通知,接收服务端确认
     *
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        state.checkStop();
        SpeechReqProtocol req = new SpeechReqProtocol();
        req.setAppKey(getAppKey());
        req.header.put(Constant.PROP_NAMESPACE, VALUE_NAMESPACE_ASR);
        req.header.put(Constant.PROP_NAME, VALUE_NAME_ASR_STOP);
        req.header.put(Constant.PROP_TASK_ID, currentTaskId);
        conn.sendText(req.serialize());
        state = STATE_STOP_SENT;

    }

}
