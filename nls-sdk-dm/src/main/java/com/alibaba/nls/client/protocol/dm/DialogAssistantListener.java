package com.alibaba.nls.client.protocol.dm;

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
public abstract class DialogAssistantListener implements ConnectionListener {
    Logger logger = LoggerFactory.getLogger(DialogAssistantListener.class);
    private DialogAssistant dialogAssistant;

    public void setDialogAssistant(DialogAssistant assistant) {
        this.dialogAssistant = assistant;
    }

    /**
     * 服务端已经准备好接收音频数据
     *
     * @param response
     */
    public abstract void onRecognitionStarted(DialogAssistantResponse response);

    /**
     * 唤醒词云端验证结束,判断是否为唤醒词
     *
     * @param response
     */
    public boolean onWakeWordVerificationCompleted(DialogAssistantResponse response) {
        return false;
    }


    /**
     * 语音识别过程中返回的结果
     *
     * @param response
     */
    public abstract void onRecognitionResultChanged(DialogAssistantResponse response);

    /**
     * 语音识别结束,返回最终结果
     * @param response
     */
    public abstract void onRecognitionCompleted(DialogAssistantResponse response);

    /**
     * 对话结果生成
     * @param response
     */
    public abstract void onDialogResultGenerated(DialogAssistantResponse response);

    /**
     * 失败处理
     * @param response
     */
    public abstract void onFail(DialogAssistantResponse response);


    @Override
    public void onOpen() {
        logger.debug("connection is ok");

    }

    @Override
    public void onClose(int closeCode, String reason) {
        if (dialogAssistant != null) {
            dialogAssistant.markClosed();
        }
        logger.info("connection is closed due to {},code:{}", reason, closeCode);

    }



    @Override
    public void onMessage(String message) {
        if (message == null || message.trim().length() == 0) {
            return;
        }
        logger.debug("on message:{}", message);
        DialogAssistantResponse response = JSON.parseObject(message, DialogAssistantResponse.class);
        if (isReady(response)) {
            onRecognitionStarted(response);
            dialogAssistant.markReady();
        } else if (isVerificationCompleted(response)) {
            if (onWakeWordVerificationCompleted(response)) {
                dialogAssistant.setIsWakeWordVerified(true);
                dialogAssistant.markWwvComplete(true);
            } else {
                dialogAssistant.markWwvComplete(false);
            }
        } else if (isRecResult(response)) {
            onRecognitionResultChanged(response);
        } else if (isRecComplete(response)) {
            onRecognitionCompleted(response);
            dialogAssistant.markAsrComplete();
        } else if (isDialogResultGenerated(response)) {
            onDialogResultGenerated(response);
            dialogAssistant.markUdsComplete();
        } else if (isTaskFailed(response)) {
            onFail(response);
            dialogAssistant.markFail();
        } else {
            logger.error(message);
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {

    }

    private boolean isReady(DialogAssistantResponse response) {
        String name = response.getName();
        if (name.equals(UDSConstant.VALUE_NAME_DIALOG_STARTED)) {
            return true;
        }
        return false;
    }

    private boolean isVerificationCompleted(DialogAssistantResponse response) {
        String name = response.getName();
        if (name.equals(UDSConstant.VALUE_NAME_TIANGONG_WWV_COMPLETED)) {
            return true;
        }
        return false;
    }

    private boolean isRecResult(DialogAssistantResponse response) {
        String name = response.getName();
        if (name.equals(UDSConstant.VALUE_NAME_DIALOG_REC_RESULT_CHANGED)) {
            return true;
        }
        return false;
    }

    private boolean isRecComplete(DialogAssistantResponse response) {
        String name = response.getName();
        if (name.equals(UDSConstant.VALUE_NAME_DIALOG_REC_COMPLETED)) {
            return true;
        }
        return false;
    }

    private boolean isDialogResultGenerated(DialogAssistantResponse response) {
        String name = response.getName();
        if (name.equals(UDSConstant.VALUE_NAME_DIALOG_RESULT_GENERATED)) {
            return true;
        }
        return false;
    }



    private boolean isTaskFailed(DialogAssistantResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_TASK_FAILE)) {
            return true;
        }
        return false;
    }

}