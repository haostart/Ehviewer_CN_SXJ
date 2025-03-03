package com.hippo.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import okhttp3.*;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import javax.net.ssl.*;

public class HistoryUtils {
    // 用于发送历史记录的API
    private static final String HISTORY_API = "https://server.haostart.cn:5005/api/v1/history";

    // 用于发送历史记录的OkHttpClient，配置为忽略证书验证
    private static final OkHttpClient client = getUnsafeOkHttpClient();

    private static final HashMap<String, String> likedMap = new HashMap<>();
    static {
        likedMap.put("read", "已读");
        likedMap.put("unread", "未读");
        likedMap.put("favorite", "喜欢");
        likedMap.put(null, "未知");
    }

    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // 创建一个信任所有证书的X509TrustManager
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // 创建一个SSLContext，使用上面的TrustManager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建一个SSLSocketFactory
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // 构建OkHttpClient
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendHistoryData(VisitedEhviewer e, HistoryResponseCallback callback, HistoryType type) {
        String api = HISTORY_API + "/visited_ehviewer";
        // 将数据转换为JSON
        JSONObject jsonData = JSONObject.parseObject(JSON.toJSONString(e));
        jsonData.put("type", type);
        String jsonDataStr = jsonData.toJSONString();

        // 创建请求体
        RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonDataStr);

        // 创建请求
        Request request = new Request.Builder()
                .url(api)
                .post(body)
                .build();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, IOException ex) {
                // 处理失败
                ex.printStackTrace();
                // 显示Toast消息
                showToast("Failed to send history data: " + ex.getMessage());
                if (callback != null) {
                    callback.onFailure(ex);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 处理响应
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonObject = JSONObject.parseObject(responseBody);
                        String status = jsonObject.getString("status");
                        Log.d("HistoryUtils", "History data sent successfully for gid: " + e.getGid() + ". Status: " + status);
                        if (callback != null) {
                            status = likedMap.get(status);
                            callback.onSuccess(status);
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                        if (callback != null) {
                            callback.onFailure(ex);
                        }
                    }
                } else {
                    Log.e("HistoryUtils", "Failed to send history data for gid: " + e.getGid() + ". " + response.message());
                    showToast("Failed send history: " + e.getGid() + ". " + response.message());
                    if (callback != null) {
                        callback.onFailure(new IOException("Failed to send history data: " + response.message()));
                    }
                }
            }
        });
    }

    public static void showToast(String message) {
        // 需要在主线程中显示Toast
        new android.os.Handler(appContext.getMainLooper()).post(() ->
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show());
    }
}