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

import com.alibaba.nls.client.protocol.SpeechResProtocol;

/**
 * Created by siwei 2018/05/14
 * 长语音的识别结果
 */
public class SpeechTranscriberResponse extends SpeechResProtocol {
    /**
     * 句子的index
     *
     * @return
     */
    public Integer getTransSentenceIndex() {
        return (Integer)payload.get("index");
    }

    /**
     * 当前已处理的音频时长，单位是毫秒
     *
     * @return
     */
    public Integer getTransSentenceTime() {
        return (Integer)payload.get("time");
    }

    /**
     * 最终识别结果
     *
     * @return
     */
    public String getTransSentenceText() {
        return (String)payload.get("result");
    }
}
