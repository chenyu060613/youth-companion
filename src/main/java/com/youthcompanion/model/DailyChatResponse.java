package com.youthcompanion.model;

import java.util.List;

public record DailyChatResponse(

        String reply,

        int detectedLevel,

        String mode,

        List<String> recommendations,

        List<String> plan,

        String riskLevel,

        boolean showCrisisModal
) {
}