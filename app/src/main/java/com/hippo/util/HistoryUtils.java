package com.hippo.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryUtils {
    // 用于发送历史记录的API
    private static final String HISTORY_API = "http://47.97.222.233:5005/api/v1/history";

    // 用于发送历史记录的OkHttpClient
    private static final OkHttpClient client = new OkHttpClient();

    // 用于执行发送任务的线程池
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static void sendHistoryData(VisitedEhviewer e) {
        String api = HISTORY_API + "/visited_ehviewer";

        executorService.submit(() -> {
            // 将数据转换为JSON
            String jsonData = JSON.toJSONString(e);

            // 创建请求体
            RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonData);

            // 创建请求
            Request request = new Request.Builder()
                    .url(api)
                    .post(body)
                    .build();

            // 发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException ex) {
                    // 处理失败
                    ex.printStackTrace();
                    // 显示Toast消息
                    showToast("Failed to send history data: " + ex.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // 处理响应
                    if (response.isSuccessful()) {
                        Log.d("HistoryUtils", "History data sent successfully for gid: " + e.getGid());
                        showToast("History data sent successfully for gid: " + e.getGid());
                    } else {
                        Log.e("HistoryUtils", "Failed to send history data for gid: " + e.getGid() + ". " + response.message());
                        showToast("Failed to send history data for gid: " + e.getGid() + ". " + response.message());
                    }
                }
            });
        });
    }

    private static void showToast(String message) {
        // 需要在主线程中显示Toast
        new android.os.Handler(appContext.getMainLooper()).post(() ->
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show());
    }

    // 示例 main 方法只在非Android环境中使用
    public static void main(String[] args) {
        // 示例数据
        VisitedEhviewer exampleData1 = new VisitedEhviewer(2715592, VisitedEhviewer.Status.UNREAD, "2e0c937f9b", "Title1", "TitleJpn1", "Category1", "Thumb1", "Uploader1", "Tags1", 10, "http://example.com/1");

        // 发送示例数据
        sendHistoryData(exampleData1);
    }
}
