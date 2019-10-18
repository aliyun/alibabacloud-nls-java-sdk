package com.alibaba.nls.client.protocol.dm;

/**
 * @author zhishen.ml
 * @date 2017/11/24
 *
 */
public class UDSConstant {
    public static final String VALUE_NAMESPACE_DIALOG = "DialogAssistant";
    public static final String VALUE_NAMESPACE_DIALOG2 = "DialogAssistant.v2";
    public static final String VALUE_NAME_DIALOG_START = "StartRecognition";
    public static final String VALUE_NAME_DIALOG_STARTED = "RecognitionStarted";
    public static final String VALUE_NAME_TIANGONG_WWV_COMPLETED = "WakeWordVerificationCompleted";
    public static final String VALUE_NAME_DIALOG_REC_RESULT_CHANGED="RecognitionResultChanged";
    public static final String VALUE_NAME_TIANGONG_STOP_WWV= "StopWakeWordVerification";
    public static final String VALUE_NAME_DIALOG_STOP_RECOGNITION="StopRecognition";
    public static final String VALUE_NAME_DIALOG_REC_COMPLETED="RecognitionCompleted";
    public static final String VALUE_NAME_DIALOG_RESULT_GENERATED="DialogResultGenerated";
    public static final String VALUE_NAME_DIALOG_TEXT = "ExecuteDialog";
    public static final String PROP_MULTI_GROUP = "enable_multi_group";
    public static final String PROP_DIALOG_SESSION_ID="session_id";
    public static final String PROP_DIALOG_QUERY_TEXT="query";
    public static final String PROP_DIALOG_QUERY_CONTEXT="query_context";
    public static final String PROP_TIANGONG_WAKE_WORD = "wake_word";
    public static final String PROP_TIANGONG_DIALOG_ID = "dialog_id";
    public static final String PROP_TIANGONG_ENABLE_WAKE_WORD_VERIFICATION = "enable_wake_word_verification";

    public static final String PROP_TIANGONG_WAKE_WORD_MODEL = "wake_word_model";
}
