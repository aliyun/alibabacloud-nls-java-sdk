package com.alibaba.nls.client.protocol.dm;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.alibaba.nls.client.protocol.Constant;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.SpeechReqProtocol;
import com.alibaba.nls.client.util.IdGen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_CLOSED;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_COMPLETE;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_CONNECTED;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_FAIL;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_REQUEST_CONFIRMED;
import static com.alibaba.nls.client.protocol.dm.UDSConstant.VALUE_NAMESPACE_DIALOG;
import static com.alibaba.nls.client.protocol.dm.UDSConstant.VALUE_NAME_DIALOG_STOP_RECOGNITION;

/**
 * @author zhishen.ml
 * @date 2017/11/24
 *
 * 对话助手
 * 非线程安全
 */
public class DialogAssistant extends SpeechReqProtocol {
    static Logger logger = LoggerFactory.getLogger(DialogAssistant.class);

    private CountDownLatch wwvCompleteLatch;
    private CountDownLatch asrCompleteLatch;
    private CountDownLatch udsCompleteLatch;
    private CountDownLatch readyLatch;
    protected long lastSendTime=-1;

    protected List<Map<String, Object>> parmas = new ArrayList<Map<String, Object>>();

    private boolean isWakeWordVerified;

    public void setIsWakeWordVerified(boolean isWakeWordVerified) {
        this.isWakeWordVerified = isWakeWordVerified;
    }
    public boolean isWakeWordVerified() {
        return isWakeWordVerified;
    }

    /**
     * 如果没有设置format,默认为pcm
     */
    protected static final String DEFAULT_FORMAT = "pcm";
    /**
     * 如果没有设置sampleRate,默认为16000
     */
    protected static final Integer DEFAULT_SAMPLE_RATE = 16000;

    public String getFormat() {
        return (String)payload.get(Constant.PROP_ASR_FORMAT);
    }

    /**
     * 输入音频编码格式
     *
     * @param format
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
     * 需要验证的唤醒词
     *
     * @param wakeWord
     */
    public void setWakeWord(String wakeWord) {
        payload.put(UDSConstant.PROP_TIANGONG_WAKE_WORD, wakeWord);
    }

    /**
     * 唤醒词服务的模型名称
     *
     * @param wakeWordModel
     */
    public void setWakeWordModel(String wakeWordModel) {
        payload.put(UDSConstant.PROP_TIANGONG_WAKE_WORD_MODEL, wakeWordModel);
    }

    /**
     * 是否开启唤醒词验证
     *
     * @param isEnable
     */
    public void setEnableWakeWordVerification(boolean isEnable) {
        payload.put(UDSConstant.PROP_TIANGONG_ENABLE_WAKE_WORD_VERIFICATION, isEnable);
    }


    /**
     * 设置对话上下文id
     *
     * @param dialogId
     */
    public void setSessionId(String dialogId) {
        payload.put(UDSConstant.PROP_DIALOG_SESSION_ID, dialogId);
    }

    public DialogAssistant(NlsClient client, DialogAssistantListener listener) throws Exception{
        this.conn = client.connect(listener);
        payload = new HashMap<String, Object>();
        header.put(Constant.PROP_NAMESPACE, VALUE_NAMESPACE_DIALOG);
        header.put(Constant.PROP_NAME, UDSConstant.VALUE_NAME_DIALOG_START);
        payload.put(Constant.PROP_ASR_FORMAT, DEFAULT_FORMAT);
        payload.put("query_params", parmas);
        payload.put(Constant.PROP_ASR_SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
        payload.put(UDSConstant.PROP_TIANGONG_ENABLE_WAKE_WORD_VERIFICATION, false);
        listener.setDialogAssistant(this);
        state = STATE_CONNECTED;
    }

    /**
     * 自己控制发送
     *
     * @param data
     */
    public void send(byte[] data) {
        long sendInterval;
        if (lastSendTime != -1 && (sendInterval=(System.currentTimeMillis() - lastSendTime)) > 5000) {
            logger.warn("too large binary send interval: {} million second",sendInterval);
        }
        state.checkSend();
        try {
            conn.sendBinary(Arrays.copyOfRange(data, 0, data.length));
            lastSendTime=System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{}", currentTaskId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 实时流发送
     *
     * @param ins
     */
    public void send(InputStream ins) {
        state.checkSend();
        try {
            byte[] bytes = new byte[8000];
            int len = 0;
            long sendInterval;
            if (lastSendTime != -1 && (sendInterval=(System.currentTimeMillis() - lastSendTime)) > 5000) {
                logger.warn("too large binary send interval: {} million seconds",sendInterval);
            }
            while ((len = ins.read(bytes)) > 0) {
                conn.sendBinary(Arrays.copyOfRange(bytes, 0, len));
                lastSendTime=System.currentTimeMillis();
            }
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{}", currentTaskId, e);
            throw new RuntimeException(e);

        }
    }

    /**
     * 离线文件发送.当声音来自离线文件时,推荐使用此方法
     *
     * @param ins
     * @param batchSize
     * @param sleepInterval
     */
    public void send(InputStream ins, int batchSize, int sleepInterval) {
        state.checkSend();
        try {
            byte[] bytes = new byte[batchSize];
            int len = 0;
            long sendInterval;
            if (lastSendTime != -1 && (sendInterval=(System.currentTimeMillis() - lastSendTime)) > 5000) {
                logger.warn("too large binary send interval: {} million seconds",sendInterval);
            }
            while ((len = ins.read(bytes)) > 0) {
                conn.sendBinary(Arrays.copyOfRange(bytes, 0, len));
                lastSendTime=System.currentTimeMillis();
                Thread.sleep(sleepInterval);
            }
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{}", currentTaskId, e);
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
        }
    }

    /**
     * 内部调用方法
     */
    void markWwvComplete(boolean isWaked) {
        if(!isWaked){
            state = STATE_COMPLETE;
        }
        if (wwvCompleteLatch != null) {
            wwvCompleteLatch.countDown();
        }

    }

    /**
     * 内部调用方法
     */
    void markAsrComplete() {
        if (asrCompleteLatch != null) {
            asrCompleteLatch.countDown();
        }

    }

    /**
     * 内部调用方法
     */
    void markUdsComplete() {
        state = STATE_COMPLETE;
        if (udsCompleteLatch != null) {
            udsCompleteLatch.countDown();
        }

    }

    /**
     * 内部调用方法
     */
    public void markFail() {
        state = STATE_FAIL;
        if (readyLatch != null) {
            readyLatch.countDown();
        }
        if (wwvCompleteLatch != null) {
            wwvCompleteLatch.countDown();
        }
        if (asrCompleteLatch != null) {
            asrCompleteLatch.countDown();
        }
        if (udsCompleteLatch != null) {
            udsCompleteLatch.countDown();
        }
    }

    /**
     * 内部调用方法
     */
    void markClosed() {
        state = STATE_CLOSED;
        if (readyLatch != null) {
            readyLatch.countDown();
        }
        if (wwvCompleteLatch != null) {
            wwvCompleteLatch.countDown();
        }
        if (asrCompleteLatch != null) {
            asrCompleteLatch.countDown();
        }
        if (udsCompleteLatch != null) {
            udsCompleteLatch.countDown();
        }
    }

    /**
     * 开始语音识别:发送识别请求,同步接收服务端确认
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        super.start();
        wwvCompleteLatch = new CountDownLatch(1);
        readyLatch = new CountDownLatch(1);
        asrCompleteLatch = new CountDownLatch(1);
        udsCompleteLatch = new CountDownLatch(1);
        boolean result = readyLatch.await(10, TimeUnit.SECONDS);
        if (!result) {
            String msg = String.format("timeout after 10 seconds waiting for start confirmation.task_id:%s",
                currentTaskId);
            logger.error(msg);
            throw new Exception(msg);
        }

    }

    public void stopWakeWordVerification() throws Exception {
        state.checkStop();
        SpeechReqProtocol req = new SpeechReqProtocol();
        req.setAppKey(getAppKey());
        req.header.put(Constant.PROP_NAMESPACE, VALUE_NAMESPACE_DIALOG);
        req.header.put(Constant.PROP_NAME, UDSConstant.VALUE_NAME_TIANGONG_STOP_WWV);
        req.header.put(Constant.PROP_TASK_ID, currentTaskId);
        conn.sendText(req.serialize());
        boolean result = wwvCompleteLatch.await(10, TimeUnit.SECONDS);
        if (!result) {
            String msg = String.format("timeout after 10 seconds waiting for complete confirmation.task_id:%s",
                    currentTaskId);
            logger.error(msg);
            throw new Exception(msg);
        }
    }

    public void stop() throws Exception {
        state.checkStop();
        conn.sendText(buildStopMessage());
        boolean result = udsCompleteLatch.await(10, TimeUnit.SECONDS);
        if (!result) {
            String msg = String.format("timeout after 10 seconds waiting for complete confirmation.task_id:%s",
                currentTaskId);
            logger.error(msg);
            throw new Exception(msg);
        }
    }

    protected String buildStopMessage(){
        SpeechReqProtocol req = new SpeechReqProtocol();
        req.setAppKey(getAppKey());
        req.header.put(Constant.PROP_NAMESPACE, VALUE_NAMESPACE_DIALOG);
        req.header.put(Constant.PROP_NAME, VALUE_NAME_DIALOG_STOP_RECOGNITION);
        req.header.put(Constant.PROP_TASK_ID, currentTaskId);
        return req.serialize();
    }

    /**
     * 设置对话相关参数
     *
     * @param key
     * @param value
     * @return
     */
    public DialogAssistant addDialogParam(String key, Object value) {
        Map<String, Object> param = new HashMap<String, Object>(2);
        param.put("name", key);
        param.put("value", value);
        parmas.add(param);
        return this;
    }

    public void query() throws Exception {
        header.put(Constant.PROP_NAME, UDSConstant.VALUE_NAME_DIALOG_TEXT);
        String taskId = IdGen.genId();
        currentTaskId = taskId;
        this.setTaskId(taskId);
        conn.sendText(this.serialize());
        udsCompleteLatch = new CountDownLatch(1);
        boolean result = udsCompleteLatch.await(10, TimeUnit.SECONDS);
        if (!result) {
            String msg = String.format("timeout after 10 seconds waiting for uds complete.task_id:%s",
                currentTaskId);
            logger.error(msg);
            throw new Exception(msg);
        }

    }

    /**
     * 设置对话上下文
     *
     * @param context
     */
    public void setDialogContext(String context) {
        payload.put(UDSConstant.PROP_DIALOG_QUERY_CONTEXT, context);
    }

    /**
     * 关闭连接
     */
    public void close() {
        conn.close();
    }

    public void waitForComplete() throws Exception {
        udsCompleteLatch.await();
    }

}