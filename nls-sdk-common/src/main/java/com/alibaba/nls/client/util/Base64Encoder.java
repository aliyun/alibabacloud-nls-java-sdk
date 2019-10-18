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

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author zhishen.ml
 * A simple encoder from Java 1.8, since we are using 1.6.
 */
public class Base64Encoder {
    private static final char[] TO_BASE_64 = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    private static final int LINE_MAX = -1;
    private static final boolean DO_PADDING = true;
    private static final byte[] NEW_LINE = null;

    private static int outLength(int srclen) {
        int len = 0;
        if (DO_PADDING) {
            len = 4 * ((srclen + 2) / 3);
        } else {
            int n = srclen % 3;
            len = 4 * (srclen / 3) + (n == 0 ? 0 : n + 1);
        }
        if (LINE_MAX > 0) {
            len += (len - 1) / LINE_MAX * NEW_LINE.length;
        }
        return len;
    }

    private static int encode0(byte[] src, int off, int end, byte[] dst) {
        char[] base64 = TO_BASE_64;
        int sp = off;
        int slen = (end - off) / 3 * 3;
        int sl = off + slen;
        if (LINE_MAX > 0 && slen > LINE_MAX / 4 * 3) { slen = LINE_MAX / 4 * 3; }
        int dp = 0;
        while (sp < sl) {
            int sl0 = Math.min(sp + slen, sl);
            for (int sp0 = sp, dp0 = dp; sp0 < sl0; ) {
                int bits = (src[sp0++] & 0xff) << 16 |
                    (src[sp0++] & 0xff) << 8 |
                    (src[sp0++] & 0xff);
                dst[dp0++] = (byte)base64[(bits >>> 18) & 0x3f];
                dst[dp0++] = (byte)base64[(bits >>> 12) & 0x3f];
                dst[dp0++] = (byte)base64[(bits >>> 6) & 0x3f];
                dst[dp0++] = (byte)base64[bits & 0x3f];
            }
            int dlen = (sl0 - sp) / 3 * 4;
            dp += dlen;
            sp = sl0;
            if (dlen == LINE_MAX && sp < end) {
                for (byte b : NEW_LINE) {
                    dst[dp++] = b;
                }
            }
        }
        if (sp < end) {
            int b0 = src[sp++] & 0xff;
            dst[dp++] = (byte)base64[b0 >> 2];
            if (sp == end) {
                dst[dp++] = (byte)base64[(b0 << 4) & 0x3f];
                if (DO_PADDING) {
                    dst[dp++] = '=';
                    dst[dp++] = '=';
                }
            } else {
                int b1 = src[sp++] & 0xff;
                dst[dp++] = (byte)base64[(b0 << 4) & 0x3f | (b1 >> 4)];
                dst[dp++] = (byte)base64[(b1 << 2) & 0x3f];
                if (DO_PADDING) {
                    dst[dp++] = '=';
                }
            }
        }
        return dp;
    }

    public static String encode(byte[] src) {
        int len = outLength(src.length);
        byte[] dst = new byte[len];
        int ret = encode0(src, 0, src.length, dst);
        if (ret != dst.length) {
            dst = Arrays.copyOf(dst, ret);
        }

        return new String(dst, 0, dst.length, Charset.defaultCharset());
    }
}

