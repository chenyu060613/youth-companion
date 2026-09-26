package com.youthcompanion.model;

public record ChatMessage(
        String role,
        String content
) {
}