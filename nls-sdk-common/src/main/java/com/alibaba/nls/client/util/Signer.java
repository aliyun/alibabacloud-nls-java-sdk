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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author zhishen.ml
 */
public class Signer {

    private static final String ALGORITHM_NAME = "HmacSHA1";
    private static final String ENCODING = "UTF-8";
    private static final String GMT="GMT";

    public static String signString(String stringToSign, String accessKeySecret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM_NAME);
            mac.init(new SecretKeySpec(
                accessKeySecret.getBytes(ENCODING),
                ALGORITHM_NAME
            ));
            byte[] signData = mac.doFinal(stringToSign.getBytes(ENCODING));

            String encodeToString = Base64Encoder.encode(signData);

            return encodeToString.substring(0, (encodeToString + "\n").length() - 1);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    public static String toGMTString() {
        return toGMTString(new Date());
    }

    private static String toGMTString(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.UK);
        df.setTimeZone(new SimpleTimeZone(0, GMT));
        String text = df.format(date);
        if (!text.endsWith(GMT)) {
            // delete +00:00 from the end of string: Sun, 30 Sep 2018 08:42:06 GMT+00:00
            text = text.substring(0, text.length() - 6);
        }
        return text;
    }
}

