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

package com.alibaba.nls.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nls.client.util.Signer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 访问令牌是用户访问智能语音服务的凭证
 *
 * @author xuebin
 */
public class AccessToken {
    private static Logger logger = LoggerFactory.getLogger(AccessToken.class);
    private static final String TAG = "AliSpeechSDK";
    private static final String NODE_TOKEN = "Token";
    private String accessKeyId;
    private String accessKeySecret;
    private String token;
    private long expireTime;

    private final static int HTTP_SCCESS_CODE=200;
    private final static int HTTP_FAIL_CODE=500;

    private int statusCode;
    private String errorMessage;

    class HttpResponse {

        private int statusCode;
        private String errorMessage;
        String result;
        String text;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

    }

    class HttpRequest {

        public static final String CHARSET_UTF8 = "UTF-8";

        public static final String METHOD_POST = "POST";

        public static final String HEADER_CONTENT_TYPE = "Content-Type";
        public static final String HEADER_ACCEPT = "Accept";
        public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
        public static final String HEADER_DATE = "Date";
        public static final String HEADER_AUTHORIZATION = "Authorization";
        public static final String HEADER_CONTENT_MD5 = "Content-MD5";

        private static final String URL_PREFIX = "http://nls-meta.cn-shanghai.aliyuncs.com/pop/2018-05-18/tokens";
        public static final String CONTENT_TYPE = "application/octet-stream;charset=utf-8";
        public static final String ACCEPT = "application/json";

        protected String method = METHOD_POST;
        protected Map<String, String> header;

        private String url;

        public HttpRequest() {
            url = URL_PREFIX;
            header = new HashMap<String, String>();
            header.put(HEADER_ACCEPT, ACCEPT);
            header.put(HEADER_CONTENT_TYPE, CONTENT_TYPE);
            // POP doesn't support gzip
            header.put(HEADER_ACCEPT_ENCODING, "identity");
        }

        /**
         * 返回此 request 对应的 url 地址
         *
         * @return url 地址
         */
        public String getUrl() {
            return url;
        }

        /**
         * 返回 http body
         *
         * @return 二进制形式的http body
         */
        public byte[] getBodyBytes() {
            return null;
        }

        public String getMethod() {
            return METHOD_POST;
        }

        /**
         * 解析http返回结果,构建response对象
         *
         * @param statusCode http 状态码
         * @param bytes      返回的二进制数据
         * @return response对象
         */
        public HttpResponse parse(int statusCode, byte[] bytes) throws IOException {
            HttpResponse response = new HttpResponse();
            response.setStatusCode(statusCode);
            String result = new String(bytes, HttpRequest.CHARSET_UTF8);
            if (response.getStatusCode() == HTTP_SCCESS_CODE) {
                response.setResult(result);
            } else {
                response.setErrorMessage(result);
            }
            return response;
        }

        public Map<String, String> getHeader() {
            return header;
        }

        public void authorize(String akId, String akSecret) {
            String bodyMd5 = getBodyMd5();
            String dateString = Signer.toGMTString();
            header.put(HEADER_CONTENT_MD5, bodyMd5);
            header.put(HEADER_DATE, dateString);
            String stringToSign = method
                + "\n"
                + ACCEPT
                + "\n"
                + bodyMd5
                + "\n"
                + CONTENT_TYPE
                + "\n"
                + dateString
                + "\n"
                + "/pop/2018-05-18/tokens";
            String signature = Signer.signString(stringToSign, akSecret);
            String authHeader = "acs " + akId + ":" + signature;
            header.put(HEADER_AUTHORIZATION, authHeader);
        }

        protected String getBodyMd5() {
            // The bodyMD5 is hard coded because the body is always empty
            return "1B2M2Y8AsgTpgAmY7PhCfg==";
        }

    }

    /**
     * 构造实例
     *
     * @param accessKeyId 阿里云akid
     * @param accessKeySecret 阿里云secret key
     */
    public AccessToken(String accessKeyId, String accessKeySecret) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
    }

    /**
     * 向服务端申请访问令牌，调用即返回，任务在后台运行，申请成功后会更新token和expireTime
     *
     * @throws IOException https调用出错
     */
    public void apply() throws IOException {
        HttpRequest request = new HttpRequest();
        request.authorize(accessKeyId, accessKeySecret);
        HttpResponse response = send(request);
        if (response.getErrorMessage() == null) {
            String result = response.getResult();
            JSONObject jsonObject = JSON.parseObject(result);
            if (jsonObject.containsKey(NODE_TOKEN)) {
                this.token = jsonObject.getJSONObject(NODE_TOKEN).getString("Id");
                this.expireTime = jsonObject.getJSONObject(NODE_TOKEN).getIntValue("ExpireTime");
            } else {
                statusCode = HTTP_FAIL_CODE;
                errorMessage = "Received unexpected result: " + result;
            }
        } else {
            logger.error("error to get token,{}", response);
            statusCode = response.getStatusCode();
            errorMessage = response.getErrorMessage();
        }
    }

    public String getToken() {
        return token;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public static HttpResponse send(HttpRequest request) throws IOException {
        OutputStream out = null;
        InputStream inputStream = null;
        try {
            URL realUrl = new URL(request.getUrl());
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestMethod(request.getMethod());
            if (HttpRequest.METHOD_POST.equals(request.getMethod())) {
                // 发送POST请求必须设置如下两行
                conn.setDoOutput(true);
                conn.setDoInput(true);
            }
            conn.setUseCaches(false);
            // http header 参数
            Map<String, String> header = request.getHeader();
            for (String name : header.keySet()) {
                conn.setRequestProperty(name, header.get(name));
            }
            final byte[] bodyBytes = request.getBodyBytes();
            if (bodyBytes != null) {
                // 获取URLConnection对象对应的输出流
                out = conn.getOutputStream();
                // 发送请求参数
                out.write(bodyBytes);
                // flush输出流的缓冲
                out.flush();
            }
            final int code = conn.getResponseCode();
            Map<String, List<String>> headerFields = conn.getHeaderFields();
            String responseMessage = conn.getResponseMessage();
            if (code == HTTP_SCCESS_CODE) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }
            HttpResponse requestResponse = request.parse(code, readAll(inputStream));
            return requestResponse;
        } finally { // 使用finally块来关闭输出流、输入流
            try {
                if (out != null) {
                    out.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];
        int len = inputStream.read(bytes);
        while (len > 0) {
            byteArrayOutputStream.write(bytes, 0, len);
            len = inputStream.read(bytes);
        }
        return byteArrayOutputStream.toByteArray();
    }

}
