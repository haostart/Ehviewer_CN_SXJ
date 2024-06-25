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
import java.util.HashMap;


public class HistoryUtils {
    // 用于发送历史记录的API
    private static final String HISTORY_API = "https://server.haostart.cn:5005/api/v1/history";

    // 用于发送历史记录的OkHttpClient
    private static final OkHttpClient client = new OkHttpClient();

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

    public static void sendHistoryData(VisitedEhviewer e, HistoryResponseCallback callback,HistoryType type) {
        String api = HISTORY_API + "/visited_ehviewer";
        // 将数据转换为JSON
        JSONObject jsonData = JSONObject.parseObject(JSON.toJSONString(e));
        jsonData.put("type",type);
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
