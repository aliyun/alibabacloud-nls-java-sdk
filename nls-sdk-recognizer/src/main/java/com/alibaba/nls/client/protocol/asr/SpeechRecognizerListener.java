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

import java.nio.ByteBuffer;

import com.alibaba.fastjson.JSON;
import com.alibaba.nls.client.protocol.Constant;
import com.alibaba.nls.client.transport.ConnectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhishen.ml
 * @date 2017/11/24
 *
 */
public abstract class SpeechRecognizerListener implements ConnectionListener {
    Logger logger = LoggerFactory.getLogger(SpeechRecognizerListener.class);
    protected SpeechRecognizer recognizer;

    public void setSpeechRecognizer(SpeechRecognizer recognizer) {
        this.recognizer = recognizer;
    }

    /**
     * 语音识别过程中返回的结果
     *
     * @param response
     */
    public abstract void onRecognitionResultChanged(SpeechRecognizerResponse response);

    /**
     * 语音识别结束后返回的最终结果
     *
     * @param response
     */
    public abstract void onRecognitionCompleted(SpeechRecognizerResponse response);

    /**
     * 识别开始
     *
     * @param response
     */
    public abstract void onStarted(SpeechRecognizerResponse response);

    /**
     * 识别错误
     *
     * @param response
     */
    public abstract void onFail(SpeechRecognizerResponse response);

    @Override
    public void onOpen() {
        logger.debug("connection is ok");

    }

    @Override
    public void onClose(int closeCode, String reason) {
        if (recognizer != null) {
            recognizer.markClosed();
        }
        logger.info("connection is closed due to {},code:{}", reason, closeCode);

    }

    @Override
    public void onMessage(String message) {
        if (message == null || message.trim().length() == 0) {
            return;
        }
        logger.debug("on message:{}", message);
        SpeechRecognizerResponse response = JSON.parseObject(message, SpeechRecognizerResponse.class);
        if (isRecReady(response)) {
            recognizer.markReady();
            onStarted(response);
        } else if (isRecResult(response)) {
            onRecognitionResultChanged(response);
        } else if (isRecComplete(response)) {
            recognizer.markComplete();
            onRecognitionCompleted(response);
        } else if (isTaskFailed(response)) {
            recognizer.markFail();
            onFail(response);
        } else {
            logger.error(message);
        }

    }

    @Override
    public void onMessage(ByteBuffer message) {

    }

    private boolean isRecReady(SpeechRecognizerResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_STARTED)) {
            return true;
        }
        return false;
    }

    private boolean isRecResult(SpeechRecognizerResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_RESULT_CHANGE)) {
            return true;
        }
        return false;
    }

    private boolean isRecComplete(SpeechRecognizerResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_COMPLETE)) {
            return true;
        }
        return false;
    }

    private boolean isTaskFailed(SpeechRecognizerResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_TASK_FAILE)) {
            return true;
        }
        return false;
    }

}
