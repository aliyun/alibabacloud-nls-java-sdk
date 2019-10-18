package com.alibaba.nls.client.protocol.dm;

import com.alibaba.nls.client.protocol.Constant;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SpeechReqProtocol;

import static com.alibaba.nls.client.protocol.dm.UDSConstant.*;

/**
 * 地铁项目需要asr能同时调用多个分组，为此扩展dialogAssistant协议
 */
public class DialogAssistantV2 extends DialogAssistant {

    public DialogAssistantV2(NlsClient client, DialogAssistantListener listener) throws Exception{
        super(client, listener);
        header.put(Constant.PROP_NAMESPACE, VALUE_NAMESPACE_DIALOG2);
        // v2 协议是个临时协议只是给地铁调用多个asr分组时使用，场景只有multi_group为"true"，而且
        header.put(UDSConstant.PROP_MULTI_GROUP, "true");
    }

    @Override
    protected String buildStopMessage(){
        SpeechReqProtocol req = new SpeechReqProtocol();
        req.setAppKey(getAppKey());
        req.header.put(Constant.PROP_NAMESPACE, VALUE_NAMESPACE_DIALOG2);
        req.header.put(Constant.PROP_NAME, VALUE_NAME_DIALOG_STOP_RECOGNITION);
        req.header.put(Constant.PROP_TASK_ID, currentTaskId);
        return req.serialize();
    }

}
