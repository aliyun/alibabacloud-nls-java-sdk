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

package com.alibaba.nls.client.protocol.tts;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import com.alibaba.fastjson.JSON;
import com.alibaba.nls.client.protocol.Constant;
import com.alibaba.nls.client.transport.ConnectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhishen on 2017/11/2.
 */
public abstract class SpeechSynthesizerListener implements ConnectionListener {
    Logger logger = LoggerFactory.getLogger(SpeechSynthesizerListener.class);
    private CountDownLatch completeLatch;
    private CountDownLatch readyLatch;

    private SpeechSynthesizer speechSynthesizer;

    void setSpeechSynthesizer(SpeechSynthesizer speechSynthesizer){
        this.speechSynthesizer=speechSynthesizer;
    }

    /**
     * 语音合成结束
     *
     * @param response
     */
    abstract public void onComplete(SpeechSynthesizerResponse response);

    @Override
    public void onOpen() {
        logger.info("connection is ok");

    }

    @Override
    public void onClose(int closeCode, String reason) {
        if(speechSynthesizer!=null){
            speechSynthesizer.markClosed();
        }
        logger.info("connection is closed due to {},code:{}", reason, closeCode);

    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("error occurred", throwable);

    }

    @Override
    public void onMessage(String message) {
        if (message == null || message.trim().length() == 0) {
            return;
        }
        logger.debug("on message:{}", message);
        SpeechSynthesizerResponse response = JSON.parseObject(message, SpeechSynthesizerResponse.class);
        if (isComplete(response)) {
            completeLatch.countDown();
            onComplete(response);
        } else if (isTaskFailed(response)) {
            completeLatch.countDown();
            onFail(response.getStatus(), response.getStatusText());
        } else {
            logger.error(message);
        }

    }

    /**
     * 失败状况处理
     *
     * @param status
     * @param reason
     */
    @Override
    public void onFail(int status, String reason) {
        logger.error("fail status:{},reasone:{}", status, reason);

    }

    @Override
    abstract public void onMessage(ByteBuffer message);


    private boolean isComplete(SpeechSynthesizerResponse response) {
        String name = response.getName();
        if (name.equals(TTSConstant.VALUE_NAME_TTS_COMPLETE)) {
            return true;
        }
        return false;
    }

    private boolean isTaskFailed(SpeechSynthesizerResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_TASK_FAILE)) {
            return true;
        }
        return false;
    }

    public void setCompleteLatch(CountDownLatch latch) {
        completeLatch = latch;
    }

}
