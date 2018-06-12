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

/**
 * SampleRateEnum enum
 *
 * @author siwei
 * @date 2018/5/30
 */
public enum SampleRateEnum {
    /**
     * 8000
     */
    SAMPLE_RATE_8K(8000),
    /**
     * 16000
     */
    SAMPLE_RATE_16K(16000);
    public int value;

    SampleRateEnum(int value) {
        this.value = value;
    }
}
