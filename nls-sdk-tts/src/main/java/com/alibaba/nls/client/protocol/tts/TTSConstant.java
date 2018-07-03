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

/**
 * Created by zhishen on 2018/6/1.
 */
public class TTSConstant {
    public static final String PROP_TTS_FORMAT = "format";
    public static final String PROP_TTS_SAMPLE_RATE = "sample_rate";
    public static final String PROP_TTS_TEXT = "text";
    public static final String PROP_TTS_VOICE = "voice";
    public static final String PROP_TTS_SPEECH_RATE = "speech_rate";
    public static final String PROP_TTS_PITCH_RATE = "pitch_rate";
    public static final String PROP_TTS_VOLUME = "volume";
    public static final String PROP_TTS_METHOD = "method";
    public static final String VALUE_NAMESPACE_TTS = "SpeechSynthesizer";
    public static final String VALUE_NAME_TTS_START = "StartSynthesis";
    public static final String VALUE_NAME_TTS_STARTED = "SynthesisStarted";
    public static final String VALUE_NAME_TTS_COMPLETE = "SynthesisCompleted";
}
