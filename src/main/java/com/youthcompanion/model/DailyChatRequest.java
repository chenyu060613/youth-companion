package com.youthcompanion.model;

import java.util.List;

public record DailyChatRequest(
        List<ChatMessage> messages
) {
}