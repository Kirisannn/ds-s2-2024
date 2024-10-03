package com.example.weather;

import com.google.gson.JsonObject;

// Request wrapper for PUT requests with Lamport timestamp
class LamportRequest implements Comparable<LamportRequest> {
    private final int lamportTimestamp;
    private final JsonObject content;

    public LamportRequest(int lamportTimestamp, JsonObject content) {
        this.lamportTimestamp = lamportTimestamp;
        this.content = content;
    }

    public int getLamportTimestamp() {
        return lamportTimestamp;
    }

    public JsonObject getContent() {
        return content;
    }

    // Ensure requests are ordered by Lamport timestamp
    @Override
    public int compareTo(LamportRequest other) {
        return Integer.compare(this.lamportTimestamp, other.lamportTimestamp);
    }
}
