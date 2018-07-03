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
 * Created by siwei on 2018/05/14
 */
public abstract class SpeechTranscriberListener implements ConnectionListener {
    Logger logger = LoggerFactory.getLogger(SpeechTranscriberListener.class);
    private SpeechTranscriber transcriber;

    public void setSpeechTranscriber(SpeechTranscriber transcriber) {
        this.transcriber = transcriber;
    }

    /**
     * 服务端准备好了进行识别
     *
     * @param response
     */
    public void onTranscriberStart(SpeechTranscriberResponse response) {

    }

    /**
     * 服务端检测到了一句话的开始
     *
     * @param response
     */
    public void onSentenceBegin(SpeechTranscriberResponse response) {

    }

    /**
     * 服务端检测到了一句话的结束
     *
     * @param response
     */
    public void onSentenceEnd(SpeechTranscriberResponse response) {

    }

    /**
     * 语音识别过程中返回的结果
     *
     * @param response
     */
    public void onTranscriptionResultChange(SpeechTranscriberResponse response) {

    }

    /**
     * 识别结束后返回的最终结果
     *
     * @param response
     */
    public void onTranscriptionComplete(SpeechTranscriberResponse response) {

    }

    /**
     * 失败状况处理
     *
     * @param status
     * @param reason
     */
    @Override
    public void onFail(int status, String reason) {
        logger.error("fail status:{}, reasone:{}", status, reason);
    }

    @Override
    public void onOpen() {
        logger.debug("connection is ok");
    }

    @Override
    public void onClose(int closeCode, String reason) {
        if (transcriber != null) {
            transcriber.markClosed();
        }
        logger.info("connection is closed due to {},code:{}", reason, closeCode);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
    }

    @Override
    public void onMessage(String message) {
        if (message == null || message.trim().length() == 0) {
            return;
        }
        logger.debug("on message:{}", message);
        SpeechTranscriberResponse response = JSON.parseObject(message, SpeechTranscriberResponse.class);
        if (isTranscriptionStarted(response)) {
            onTranscriberStart(response);
            transcriber.markTranscriberReady();
        } else if (isSentenceBegin(response)) {
            onSentenceBegin(response);
        } else if (isSentenceEnd(response)) {
            onSentenceEnd(response);
        } else if (isTranscriptionResultChanged(response)) {
            onTranscriptionResultChange(response);
        } else if (isTranscriptionCompleted(response)) {
            onTranscriptionComplete(response);
            transcriber.markTranscriberComplete();
        } else if (isTaskFailed(response)) {
            onFail(response.getStatus(), response.getStatusText());
            transcriber.markFail();
        } else {
            logger.error("can not process this message: {}", message);
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {

    }

    private boolean isTranscriptionStarted(SpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_TRANSCRIPTION_STARTED)) {
            return true;
        }
        return false;
    }

    private boolean isSentenceBegin(SpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_SENTENCE_BEGIN)) {
            return true;
        }
        return false;
    }

    private boolean isSentenceEnd(SpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_SENTENCE_END)) {
            return true;
        }
        return false;
    }

    private boolean isTranscriptionResultChanged(SpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_TRANSCRIPTION_RESULT_CHANGE)) {
            return true;
        }
        return false;
    }

    private boolean isTranscriptionCompleted(SpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_TRANSCRIPTION_COMPLETE)) {
            return true;
        }
        return false;
    }

    private boolean isTaskFailed(SpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_TASK_FAILE)) {
            return true;
        }
        return false;
    }

}
