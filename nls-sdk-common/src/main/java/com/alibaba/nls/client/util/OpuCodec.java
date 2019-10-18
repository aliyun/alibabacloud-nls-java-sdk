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

package com.alibaba.nls.client.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * opu编解码器
 *
 * @author zhishen.ml
 * @Date 2018/12/7.
 */
public class OpuCodec {
    public static final int RAW_BUF_SIZE = 640;
    public static final int OPU_BUF_SIZE = 512;

    public interface EncodeListener {
        void onEncodedData(byte[] data);
    }

    static Logger logger = LoggerFactory.getLogger(OpuCodec.class);

    static {
        try {
            NativeLibUtil.loadFromJar("nlsJniOpu");
        } catch (Throwable e1) {
            logger.warn("error to load nlsJniOpu :{}", e1.getMessage());
        }

    }

    public native long createOpuEncoder(int sampleRate);

    public native int encode(long encoder, byte[] frameBuff, int frameLen, byte[] outputBuffer, int outputLen);

    public native void destroyOpuEncoder(long handle);

    public void encode(int sampleRate, InputStream ins, OutputStream outs) throws Exception {
        long encoder = createOpuEncoder(sampleRate);
        try {
            byte[] buffer = new byte[RAW_BUF_SIZE];
            byte[] bytes = new byte[512];
            while (ins.available() >= RAW_BUF_SIZE) {
                ins.read(buffer, 0, RAW_BUF_SIZE);
                //exg: the pcm data must be 640 byte
                int encodeSize = encode(encoder, buffer, RAW_BUF_SIZE, bytes, OPU_BUF_SIZE);
                outs.write(bytes, 0, encodeSize);
            }
            int remainSize = ins.read(buffer, 0, RAW_BUF_SIZE);
            //填充
            if (remainSize > 0) {
                Arrays.fill(buffer, remainSize - 1, RAW_BUF_SIZE, (byte)0);
                int encodeSize = encode(encoder, buffer, RAW_BUF_SIZE, bytes, OPU_BUF_SIZE);
                outs.write(bytes, 0, encodeSize);
            }
            ins.close();
            outs.close();
        } catch (Exception e) {
            logger.error("encode error", e);
            throw e;

        } finally {
            destroyOpuEncoder(encoder);
        }

    }

    public void encode(int sampleRate, InputStream ins, EncodeListener listener) throws Exception {
        long encoder = createOpuEncoder(sampleRate);
        try {
            byte[] buffer = new byte[RAW_BUF_SIZE];
            byte[] bytes = new byte[512];
            while (ins.available() >= RAW_BUF_SIZE) {
                ins.read(buffer, 0, RAW_BUF_SIZE);
                //exg: the pcm data must be 640 byte
                int encodeSize = encode(encoder, buffer, RAW_BUF_SIZE, bytes, OPU_BUF_SIZE);
                listener.onEncodedData(Arrays.copyOfRange(bytes, 0, encodeSize));
            }
            int remainSize = ins.read(buffer, 0, RAW_BUF_SIZE);
            //填充
            if (remainSize > 0) {
                Arrays.fill(buffer, remainSize - 1, RAW_BUF_SIZE, (byte)0);
                int encodeSize = encode(encoder, buffer, RAW_BUF_SIZE, bytes, OPU_BUF_SIZE);
                listener.onEncodedData(Arrays.copyOfRange(bytes, 0, encodeSize));
            }
            ins.close();
        } catch (Exception e) {
            logger.error("encode error", e);
            throw e;
        } finally {
            destroyOpuEncoder(encoder);
        }
    }
}
