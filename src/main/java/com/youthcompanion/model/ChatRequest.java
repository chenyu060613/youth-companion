package com.youthcompanion.model;

import java.util.List;

public record ChatRequest(
        List<ChatMessage> messages
) {
}