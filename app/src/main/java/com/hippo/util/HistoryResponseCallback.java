package com.hippo.util;

public interface HistoryResponseCallback {
    void onSuccess(String status);

    void onFailure(Exception ex);
}
