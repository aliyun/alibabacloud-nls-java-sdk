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

package com.alibaba.nls.client.protocol;

import java.util.ResourceBundle;

/**
 * @author zhishen.ml
 * @date 2018/06/01
 */
public class Constant {
    public static String sdkVersion = "";

    static {
        ResourceBundle rb = ResourceBundle.getBundle("nls-sdk");
        sdkVersion = rb.getString("version");
    }

    public static final String HEADER_TOKEN = "X-NLS-Token";

    public static final String PROP_CONTEXT_SDK = "sdk";

    public static final String PROP_APP_KEY = "appkey";
    public static final String PROP_NAMESPACE = "namespace";
    public static final String PROP_NAME = "name";
    public static final String PROP_STATUS = "status";
    public static final String PROP_STATUS_TEXT = "status_text";
    public static final String PROP_MESSAGE_ID = "message_id";
    public static final String PROP_TASK_ID = "task_id";

    public static final String PROP_ASR_FORMAT = "format";
    public static final String PROP_ASR_SAMPLE_RATE = "sample_rate";
    public static final String PROP_ASR_ENABLE_ITN = "enable_inverse_text_normalization";
    public static final String PROP_ASR_ENABLE_INTERMEDIATE_RESULT = "enable_intermediate_result";
    public static final String PROP_ASR_ENABLE_PUNCTUATION_PREDICTION = "enable_punctuation_prediction";

    public static final String VALUE_NAME_TASK_FAILE = "TaskFailed";
    public static final String VALUE_NAMESPACE_ASR = "SpeechRecognizer";
    public static final String VALUE_NAME_ASR_COMPLETE = "RecognitionCompleted";
    public static final String VALUE_NAME_ASR_START = "StartRecognition";
    public static final String VALUE_NAME_ASR_STARTED = "RecognitionStarted";
    public static final String VALUE_NAME_ASR_STOP = "StopRecognition";
    public static final String VALUE_NAME_ASR_RESULT_CHANGE = "RecognitionResultChanged";

    public static final String VALUE_NAMESPACE_ASR_TRANSCRIPTION = "SpeechTranscriber";
    public static final String VALUE_NAME_ASR_TRANSCRIPTION_START = "StartTranscription";
    public static final String VALUE_NAME_ASR_TRANSCRIPTION_STOP = "StopTranscription";
    public static final String VALUE_NAME_ASR_TRANSCRIPTION_STARTED = "TranscriptionStarted";
    public static final String VALUE_NAME_ASR_TRANSCRIPTION_RESULT_CHANGE = "TranscriptionResultChanged";
    public static final String VALUE_NAME_ASR_TRANSCRIPTION_COMPLETE = "TranscriptionCompleted";
    public static final String VALUE_NAME_ASR_NLP_RESULT = "SentenceSemantics";
    public static final String VALUE_NAME_ASR_SENTENCE_BEGIN = "SentenceBegin";
    public static final String VALUE_NAME_ASR_SENTENCE_END = "SentenceEnd";
}
