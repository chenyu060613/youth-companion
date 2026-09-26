package com.youthcompanion.model;

import java.util.List;

public record InterestChatRequest(
        String interestTitle,
        String mode,
        String projectIdea,
        List<ChatMessage> messages
) {
}