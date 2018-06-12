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

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.alibaba.nls.client.protocol.Constant;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.SpeechReqProtocol;
import com.alibaba.nls.client.transport.Connection;
import com.alibaba.nls.client.util.IdGen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.nls.client.protocol.Constant.VALUE_NAMESPACE_ASR;
import static com.alibaba.nls.client.protocol.Constant.VALUE_NAME_ASR_STOP;
import static com.alibaba.nls.client.protocol.asr.SpeechRecognizer.State.STATE_CLOSED;
import static com.alibaba.nls.client.protocol.asr.SpeechRecognizer.State.STATE_COMPLETE;
import static com.alibaba.nls.client.protocol.asr.SpeechRecognizer.State.STATE_CONNECTED;
import static com.alibaba.nls.client.protocol.asr.SpeechRecognizer.State.STATE_FAIL;
import static com.alibaba.nls.client.protocol.asr.SpeechRecognizer.State.STATE_INIT;
import static com.alibaba.nls.client.protocol.asr.SpeechRecognizer.State.STATE_REQUEST_CONFIRMED;
import static com.alibaba.nls.client.protocol.asr.SpeechRecognizer.State.STATE_REQUEST_SENT;
import static com.alibaba.nls.client.protocol.asr.SpeechRecognizer.State.STATE_STOP_SENT;

/**
 * Created by zhishen on 2017/11/24.
 * 语音识别器,用于设置及发送识别请求,处理识别结果回调
 * 非线程安全
 */
public class SpeechRecognizer extends SpeechReqProtocol {
    static Logger logger = LoggerFactory.getLogger(SpeechRecognizer.class);
    private Connection conn;
    private String currentTaskId;
    private CountDownLatch completeLatch;
    private CountDownLatch readyLatch;

    private State state = STATE_INIT;

    /**
     * 状态
     */
    public enum State {
        /**
         * 错误状态
         */
        STATE_FAIL(-1) {
            @Override
            void checkStart() {
                throw new RuntimeException("can't start,current state is " + this);
            }

            @Override
            void checkSend() {
                throw new RuntimeException("can't send,current state is " + this);
            }

            @Override
            void checkStop() {
                throw new RuntimeException("can't stop,current state is " + this);
            }
        },
        /**
         * 初始状态
         */
        STATE_INIT(0) {
            @Override
            void checkStart() {
                throw new RuntimeException("can't start,current state is " + this);
            }

            @Override
            void checkSend() {
                throw new RuntimeException("can't send,current state is " + this);
            }

            @Override
            void checkStop() {
                throw new RuntimeException("can't stop,current state is " + this);
            }
        },
        /**
         * 已连接状态
         */
        STATE_CONNECTED(10) {
            @Override
            void checkSend() {
                throw new RuntimeException("can't send,current state is " + this);
            }

            @Override
            void checkStart() {
                return;
            }

            @Override
            void checkStop() {
                throw new RuntimeException("can't stop,current state is " + this);
            }
        },
        /**
         * 语音识别请求(json)已发送
         */
        STATE_REQUEST_SENT(20) {
            @Override
            void checkSend() {
                throw new RuntimeException("can't send,current state is " + this);
            }

            @Override
            void checkStart() {
                throw new RuntimeException("can't start,current state is " + this);
            }

            @Override
            void checkStop() {
                throw new RuntimeException("can't stop,current state is " + this);
            }
        },
        /**
         * 已收到服务端对请求的确认,服务端准备接受音频
         */
        STATE_REQUEST_CONFIRMED(30) {
            @Override
            void checkSend() {
                return;
            }

            @Override
            void checkStart() {
                throw new RuntimeException("can't start,current state is " + this);
            }

            @Override
            void checkStop() {
                return;
            }
        },
        /**
         * 语音发送完毕,识别结束指令已发送
         */
        STATE_STOP_SENT(40) {
            @Override
            void checkSend() {
                throw new RuntimeException("only STATE_REQUEST_CONFIRMED can send,current state is " + this);
            }

            @Override
            void checkStart() {
                throw new RuntimeException("can't start,current state is " + this);
            }

            @Override
            void checkStop() {
                throw new RuntimeException("can't stop,current state is " + this);
            }
        },

        /**
         * 收到全部识别结果,识别结束
         */
        STATE_COMPLETE(50) {
            @Override
            void checkSend() {
                throw new RuntimeException("can't send,current state is " + this);
            }

            @Override
            void checkStart() {
                return;
            }

            @Override
            void checkStop() {
                //开启静音监测时,服务端可能提前结束
                logger.warn("task is completed before sending stop command");
            }
        },
        /**
         * 连接关闭
         */
        STATE_CLOSED(60) {
            @Override
            void checkSend() {
                throw new RuntimeException("can't send,current state is " + this);
            }

            @Override
            void checkStart() {
                throw new RuntimeException("can't start,current state is " + this);
            }

            @Override
            void checkStop() {
                throw new RuntimeException("can't stop,current state is " + this);
            }
        };

        int value;

        abstract void checkSend();

        abstract void checkStart();

        abstract void checkStop();

        State(int value) {
            this.value = value;
        }
    }

    /**
     * 如果没有设置format,默认为pcm
     */
    private static final String DEFAULT_FORMAT = "pcm";
    /**
     * 如果没有设置sampleRate,默认为16000
     */
    private static final Integer DEFAULT_SAMPLE_RATE = 16000;

    public String getFormat() {
        return (String)payload.get(Constant.PROP_ASR_FORMAT);
    }

    /**
     * 输入音频格式
     *
     * @param format pcm wav opus speex
     */
    public void setFormat(InputFormatEnum format) {
        payload.put(Constant.PROP_ASR_FORMAT, format.getName());
    }

    public Integer getSampleRate() {
        return (Integer)payload.get(Constant.PROP_ASR_SAMPLE_RATE);
    }

    /**
     * 输入音频采样率 8000 16000
     *
     * @param sampleRate
     */
    public void setSampleRate(SampleRateEnum sampleRate) {
        payload.put(Constant.PROP_ASR_SAMPLE_RATE, sampleRate.value);
    }

    /**
     * 是否返回中间识别结果，默认为false
     *
     * @param isEnable
     */
    public void setEnableIntermediateResult(boolean isEnable) {
        payload.put(Constant.PROP_ASR_ENABLE_INTERMEDIATE_RESULT, isEnable);
    }

    /**
     * 是否在识别结果中添加标点，默认值为false
     *
     * @param isEnable
     */
    public void setEnablePunctuation(boolean isEnable) {
        payload.put("enable_punctuation_prediction", isEnable);
    }

    /**
     * 设置关键词列表id,开启关键词识别
     *
     * @param keywordListId
     */
    public void setKeywordListId(String keywordListId) {
        payload.put(Constant.PROP_ASR_KEYWORD_LIST_ID, keywordListId);
    }

    /**
     * 设置定制模型id,使用定制模型服务
     *
     * @param customizationId
     */
    public void setCustomizationId(String customizationId) {
        payload.put(Constant.PROP_ASR_CUSTOMIZATION_ID, customizationId);
    }

    /**
     * 设置热词词表id,使用热词服务
     *
     * @param vocabularyId
     */
    public void setVocabularyId(String vocabularyId) {
        payload.put(Constant.PROP_ASR_VOCABULARY_ID, vocabularyId);
    }

    /**
     * 设置即时编译的词表，Map的Key和Value分别是词和权重,权重范围是-6到5
     *
     * @param vocabulary
     */
    public void setVocabulary(Map<String, Integer> vocabulary) {
        payload.put(Constant.PROP_ASR_VOCABULARY, vocabulary);
    }

    /**
     * 设置分类热词,Map的Key和Value分别是分类名称和词表id,key 当前只支持 PERSON ADDRESS,即:人名类热词,地名类热词
     *
     * @param classVocabulary
     */
    public void setClassVocabulary(Map<String, String> classVocabulary) {
        payload.put(Constant.PROP_ASR_CLASS_VOCABULARY, classVocabulary);
    }

    /**
     * 设置开启ITN(Inverse Text Normalization）,默认关闭
     * 注:此属性为语音识高级属性,普通用户无需关注
     *
     * @param enableITN
     */
    public void setEnableITN(boolean enableITN) {
        payload.put(Constant.PROP_ASR_ENABLE_ITN, enableITN);
    }

    /**
     * 设置开启语音活动检测(Voice Activity Detection,VAD),默认关闭
     * 注:此属性为语音识高级属性,普通用户无需关注
     *
     * @param enableVAD
     */
    public void setEnableVAD(boolean enableVAD) {
        payload.put(Constant.PROP_ASR_ENABLE_VAD, enableVAD);

    }

    /**
     * 允许的最大开始静音，单位是毫秒，超出后服务端将会发送RecognitionCompleted事件，结束本次识别
     * 仅当enableVAD为ture时有效
     * 注:此属性为语音识高级属性,普通用户无需关注
     *
     * @param time
     */
    public void setMaxStartSilence(int time) {
        payload.put(Constant.PROP_ASR_MAX_START_SILENCE, time);
    }

    /**
     * 允许的最大结束静音，单位是毫秒，超出后服务端将会发送RecognitionCompleted事件，结束本次识别
     * 仅当enableVAD为ture时有效
     * 注:此属性为语音识高级属性,普通用户无需关注
     *
     * @param time
     */
    public void setMaxEndSilence(int time) {
        payload.put(Constant.PROP_ASR_MAX_END_SILENCE, time);
    }

    /**
     * 返回最多备选识别结果的数量
     * 设置为0时将不返回备选识别结果,默认是0
     *
     * @param num
     */
    public void setMaxAlternates(int num) {
        payload.put(Constant.PROP_ASR_MAX_ALTERNATES, num);
    }

    public SpeechRecognizer(NlsClient client, SpeechRecognizerListener listener) {
        Connection conn = client.connect(listener);
        this.conn = conn;
        payload = new HashMap<String, Object>();
        header.put(Constant.PROP_NAMESPACE, VALUE_NAMESPACE_ASR);
        header.put(Constant.PROP_NAME, Constant.VALUE_NAME_ASR_START);
        payload.put(Constant.PROP_ASR_FORMAT, DEFAULT_FORMAT);
        payload.put(Constant.PROP_ASR_SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
        listener.setSpeechRecognizer(this);
        state = STATE_CONNECTED;
    }

    private int getBinaryBatchSentSize() {
        String format = getFormat();
        int sampleRate = getSampleRate();
        if (sampleRate == 16000) {
            if (format.equals("pcm") || format.equals("wav")) {
                return 8000;
            }
            if (format.equals("opu") || format.equals("opus")) {
                return 800;
            }
        }
        return 8000;
    }

    private int getBinarySentSleepInterval() {
        return 250;
    }

    /**
     * 自己控制发送，需要控制发送速率
     *
     * @param data
     */
    public void send(byte[] data) {
        state.checkSend();
        try {
            conn.sendBinary(Arrays.copyOfRange(data, 0, data.length));
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{},state:{}", currentTaskId, state, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 实时采集音频流
     *
     * @param ins
     */
    public void send(InputStream ins) {
        state.checkSend();
        try {
            byte[] bytes = new byte[8000];
            int len = 0;
            while ((len = ins.read(bytes)) > 0) {
                conn.sendBinary(Arrays.copyOfRange(bytes, 0, len));
            }
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{},state:{}", currentTaskId, state, e);
            throw new RuntimeException(e);

        }
    }

    /**
     * 语音数据来自文件，发送时需要控制速率，使单位时间内发送的数据大小接近单位时间原始语音数据存储的大小
     * <ul>
     *     <li><对于8k pcm 编码数据，建议每发送3200字节 sleep 200 ms/li>
     *     <li>对于16k pcm 编码数据，建议每发送6400字节 sleep 200 ms/li>
     *     <li>对于其它编码格式的数据，用户根据压缩比，自行估算，比如压缩比为10:1的 16k opus ，需要每发送6400/10=640 sleep 200ms/li>
     *</ul>
     *
     * @param ins           离线音频文件流
     * @param batchSize     每次发送到服务端的数据大小
     * @param sleepInterval 数据发送的间隔，即用于控制发送数据的速率，每次发送batchSize大小的数据后需要sleep的时间
     */
    public void send(InputStream ins, int batchSize, int sleepInterval) {
        state.checkSend();
        try {
            byte[] bytes = new byte[batchSize];
            int len = 0;
            while ((len = ins.read(bytes)) > 0) {
                conn.sendBinary(Arrays.copyOfRange(bytes, 0, len));
                Thread.sleep(sleepInterval);
            }
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{},state:{}", currentTaskId, state, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 内部调用方法
     */
    void markReady() {
        state = STATE_REQUEST_CONFIRMED;
        if (readyLatch != null) {
            readyLatch.countDown();
            readyLatch = null;
        }
    }

    /**
     * 内部调用方法
     */
    void markComplete() {
        state = STATE_COMPLETE;
        if (completeLatch != null) {
            completeLatch.countDown();
            completeLatch = null;
        }

    }

    /**
     * 内部调用方法
     */
    void markFail() {
        state = STATE_FAIL;
        if (readyLatch != null) {
            readyLatch.countDown();
            readyLatch = null;
        }
        if (completeLatch != null) {
            completeLatch.countDown();
            completeLatch = null;
        }
    }

    /**
     * 内部调用方法
     */
    void markClosed() {
        state = STATE_CLOSED;
        if (readyLatch != null) {
            readyLatch.countDown();
            readyLatch = null;
        }
        if (completeLatch != null) {
            completeLatch.countDown();
            completeLatch = null;
        }
    }

    /**
     * 开始语音识别:发送识别请求,同步接收服务端确认
     *
     * @throws Exception
     */
    public void start() throws Exception {
        state.checkStart();
        String taskId = IdGen.genId();
        currentTaskId = taskId;
        this.setTaskId(taskId);
        conn.sendText(this.serialize());
        state = STATE_REQUEST_SENT;
        completeLatch = new CountDownLatch(1);
        readyLatch = new CountDownLatch(1);
        boolean result = readyLatch.await(10, TimeUnit.SECONDS);
        if (!result) {
            String msg = String.format("timeout after 10 seconds waiting for start confirmation.task_id:%s,state:%s",
                currentTaskId, state);
            logger.error(msg);
            throw new Exception(msg);
        }

    }

    /**
     * 结束语音识别:发送结束识别通知,接收服务端确认
     *
     * @throws Exception
     */
    public void stop() throws Exception {
        state.checkStop();
        SpeechReqProtocol req = new SpeechReqProtocol();
        req.setAppKey(getAppKey());
        req.header.put(Constant.PROP_NAMESPACE, VALUE_NAMESPACE_ASR);
        req.header.put(Constant.PROP_NAME, VALUE_NAME_ASR_STOP);
        req.header.put(Constant.PROP_TASK_ID, currentTaskId);
        conn.sendText(req.serialize());
        state = STATE_STOP_SENT;
        boolean result = completeLatch.await(10, TimeUnit.SECONDS);
        if (!result) {
            String msg = String.format("timeout after 10 seconds waiting for complete confirmation.task_id:%s,state:%s",
                currentTaskId, state);
            logger.error(msg);
            throw new Exception(msg);
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        conn.close();
    }

}
