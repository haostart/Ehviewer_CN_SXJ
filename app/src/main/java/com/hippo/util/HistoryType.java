package com.hippo.util;

public enum HistoryType {
    UPLOAD(1),
    READ(2),
    FAVORITE(3);

    private final int value;

    HistoryType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
