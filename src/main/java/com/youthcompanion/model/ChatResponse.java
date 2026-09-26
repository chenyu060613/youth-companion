package com.youthcompanion.model;

public record ChatResponse(
        String reply,
        String riskLevel,
        boolean showCrisisModal
) {
}