package com.youthcompanion.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrisisService {

    /*
     * =========================================================
     * 1. 自杀 / 自伤相关敏感词
     * =========================================================
     *
     * 这里只表示：
     * “这句话涉及自杀、自伤相关内容”
     *
     * 不代表一定需要触发邮件预警。
     *
     * 例如：
     *
     * “为什么有人会自杀？”
     *
     * 会被识别为 sensitive，
     * 但不应该仅仅因此触发 crisis。
     */
    private static final List<String> SENSITIVE_TERMS = List.of(
            "自杀",
            "自残",
            "想死",
            "不想活",
            "不想活了",
            "不想活下去",
            "伤害自己",
            "伤害我自己",
            "结束生命",
            "结束自己的生命",
            "结束我的生命",
            "活不下去",
            "活不下去了",
            "不想继续活",
            "消失算了",
            "死了算了",
            "结束自己",
            "了结自己",
            "杀死自己",
            "弄死自己"
    );


    /*
     * =========================================================
     * 2. 明确表达本人当前自杀 / 自伤意愿
     * =========================================================
     *
     * 这些表达已经比较明确。
     *
     * 一旦检测到，
     * 直接判定 crisis。
     *
     * 例如：
     *
     * 我想自杀
     * 我要自杀
     * 我想死
     * 我不想活了
     * 我想自残
     * 我想伤害自己
     *
     * 都直接进入 crisis。
     */
    private static final List<String> DIRECT_CRISIS_EXPRESSIONS = List.of(

            // ===== 自杀 =====
            "我想自杀",
            "我要自杀",
            "我准备自杀",
            "我决定自杀",
            "我打算自杀",
            "我计划自杀",
            "我真的想自杀",
            "我好想自杀",
            "我就想自杀",

            // ===== 想死 =====
            "我想死",
            "我想死了",
            "我好想死",
            "我真的想死",
            "我现在想死",
            "我只想死",
            "我想去死",
            "我想要死",
            "我还是死了算了",
            "我死了算了",

            // ===== 不想活 =====
            "我不想活",
            "我不想活了",
            "我不想活下去了",
            "我不想继续活了",
            "我不想继续活",
            "我活不下去了",
            "我活不下去",
            "我不想再活了",

            // ===== 自残 =====
            "我想自残",
            "我要自残",
            "我准备自残",
            "我打算自残",
            "我决定自残",
            "我想伤害自己",
            "我要伤害自己",
            "我准备伤害自己",
            "我打算伤害自己",
            "我想伤害我自己",
            "我要伤害我自己",

            // ===== 结束生命 =====
            "我想结束生命",
            "我想结束自己的生命",
            "我想结束我的生命",
            "我要结束生命",
            "我要结束自己的生命",
            "我要结束我的生命",
            "我想结束自己",
            "我要结束自己",
            "我想了结自己",
            "我要了结自己",
            "我想杀死自己",
            "我要杀死自己",
            "我想弄死自己",

            // ===== 消失 / 死了算了 =====
            "我想消失",
            "我想永远消失",
            "我消失算了",
            "死了算了",
            "我还是死了算了"
    );


    /*
     * =========================================================
     * 3. 表示时间迫近
     * =========================================================
     */
    private static final List<String> IMMEDIATE_TIME_TERMS = List.of(
            "现在",
            "马上",
            "立刻",
            "很快",
            "等会",
            "等一下",
            "一会儿",
            "待会",
            "待会儿",
            "今晚",
            "今天",
            "今天晚上",
            "这就",
            "马上就"
    );


    /*
     * =========================================================
     * 4. 表示明确行动意图 / 准备
     * =========================================================
     */
    private static final List<String> INTENT_TERMS = List.of(
            "打算",
            "准备",
            "决定",
            "计划",
            "想要",
            "就要",
            "会去做",
            "准备去做",
            "真的要",
            "一定要",
            "马上要"
    );


    /*
     * =========================================================
     * 5. 明确否认当前实施意图
     * =========================================================
     *
     * 注意：
     *
     * 这个列表主要用来处理：
     *
     * “以前想过自残，但现在没有打算做”
     *
     * 而不是处理：
     *
     * “我想自杀，但是还没准备”
     *
     * 后者仍然表达了明确的当前自杀意愿。
     */
    private static final List<String> CURRENT_INTENT_NEGATIONS = List.of(

            "现在没有打算",
            "现在没打算",
            "目前没有打算",
            "目前没打算",
            "暂时没有打算",
            "暂时没打算",

            "没有打算做",
            "没打算做",
            "不打算做",

            "现在没有准备",
            "现在没准备",
            "目前没有准备",
            "目前没准备",
            "暂时没有准备",
            "暂时没准备",

            "没有准备做",
            "没准备做",
            "不准备做",

            "没有计划",
            "没计划",

            "现在不会",
            "目前不会",
            "暂时不会",

            "不会马上",
            "不会立刻",

            "现在不准备这么做",
            "目前不准备这么做",
            "暂时不准备这么做",

            "现在不会这么做",
            "目前不会这么做",
            "暂时不会这么做"
    );


    /*
     * =========================================================
     * 6. 明确否认自己有自杀 / 自伤想法
     * =========================================================
     *
     * 这是为了避免下面这种明显误判：
     *
     * “我不想自杀”
     * “我没有想过自杀”
     * “我不会伤害自己”
     *
     * 不能因为出现“自杀”两个字就触发 crisis。
     */
    private static final List<String> SELF_HARM_NEGATIONS = List.of(

            // 自杀
            "我不想自杀",
            "我没有想自杀",
            "我没想自杀",
            "我没有想过自杀",
            "我没想过自杀",
            "我不会自杀",
            "我不准备自杀",
            "我不打算自杀",
            "我没有自杀的想法",
            "我没有自杀想法",
            "我没自杀的想法",
            "我没自杀想法",

            // 自残
            "我不想自残",
            "我没有想自残",
            "我没想自残",
            "我没有想过自残",
            "我没想过自残",
            "我不会自残",
            "我不准备自残",
            "我不打算自残",

            // 伤害自己
            "我不想伤害自己",
            "我不会伤害自己",
            "我没有想伤害自己",
            "我没想伤害自己",
            "我不准备伤害自己",
            "我不打算伤害自己",

            // 想死
            "我现在不想死",
            "我已经不想死了",
            "我不想死",
            "我没有想死",
            "我没想死",

            // 安全确认
            "我现在是安全的",
            "我现在很安全",
            "我不会伤害我自己",
            "我能保证自己安全"
    );


    /*
     * =========================================================
     * 7. 历史性表达
     * =========================================================
     *
     * 例如：
     *
     * “我以前想过自杀”
     * “我曾经自残过”
     *
     * 这类情况依然值得关注，
     * 但是单靠 Java 关键词不要直接触发 crisis。
     *
     * 后续交给 AI 判断 concern / crisis。
     */
    private static final List<String> HISTORICAL_TERMS = List.of(
            "以前",
            "过去",
            "曾经",
            "之前",
            "从前",
            "小时候"
    );


    /*
     * =========================================================
     * 普通敏感内容检测
     * =========================================================
     */
    public boolean containsSensitiveTerms(String message) {

        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = normalize(message);

        return SENSITIVE_TERMS
                .stream()
                .anyMatch(normalized::contains);
    }


    /*
     * =========================================================
     * 检测是否存在明确的即时危险 / 高风险表达
     * =========================================================
     *
     * true：
     * 应当直接按照 crisis 处理，
     * 可以触发你的安全预警邮件。
     *
     * false：
     * 不代表完全安全，
     * 仍然可以交给 AI 进行进一步上下文判断。
     */
    public boolean containsImmediateCrisis(String message) {

        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = normalize(message);


        /*
         * -----------------------------------------------------
         * 第一优先级：
         * 已经正在实施 / 已经开始伤害自己。
         *
         * 这种情况优先级最高，
         * 不受普通否定规则影响。
         * -----------------------------------------------------
         */
        if (containsActiveCrisisExpression(normalized)) {
            return true;
        }


        /*
         * -----------------------------------------------------
         * 第二优先级：
         * 用户明确否认自己具有自杀 / 自伤想法。
         *
         * 例如：
         *
         * “我不想自杀”
         * “我不会伤害自己”
         *
         * 不直接判 crisis。
         * -----------------------------------------------------
         */
        if (
                containsSelfHarmNegation(normalized)
                        && !containsAnotherDirectCrisisExpression(normalized)
        ) {
            return false;
        }


        /*
         * -----------------------------------------------------
         * 第三优先级：
         * 当前明确表达本人想自杀 / 自残 / 想死。
         *
         * 例如：
         *
         * “我想自杀”
         * “我想死了”
         * “我不想活了”
         * “我想伤害自己”
         *
         * 直接 crisis。
         * -----------------------------------------------------
         */
        if (containsDirectCrisisExpression(normalized)) {

            /*
             * 如果明显是在描述过去，而且没有表示当前仍然存在，
             * 不由 Java 直接判 crisis。
             *
             * 例如：
             *
             * “我以前想自杀，但现在已经没有这种想法了”
             */
            if (
                    containsHistoricalContext(normalized)
                            && containsSelfHarmNegation(normalized)
            ) {
                return false;
            }

            return true;
        }


        /*
         * -----------------------------------------------------
         * 第四优先级：
         * 明确否认当前实施意图。
         *
         * 例如：
         *
         * “我以前想过自残，
         *  但现在没有打算做。”
         *
         * 这种情况不要让 Java 强行升级 crisis。
         *
         * 后续 AI 仍然可以判断成 concern。
         * -----------------------------------------------------
         */
        if (containsCurrentIntentNegation(normalized)) {
            return false;
        }


        /*
         * -----------------------------------------------------
         * 第五优先级：
         *
         * 同时存在：
         *
         * 1. 自杀 / 自伤含义
         * 2. 行动意图
         * 3. 时间迫近
         *
         * 也按照 crisis 处理。
         *
         * 例如：
         *
         * “我今晚准备结束生命”
         * -----------------------------------------------------
         */

        boolean hasSelfHarmMeaning =
                containsSensitiveTerms(normalized);

        boolean hasImmediateTime =
                IMMEDIATE_TIME_TERMS
                        .stream()
                        .anyMatch(normalized::contains);

        boolean hasIntent =
                INTENT_TERMS
                        .stream()
                        .anyMatch(normalized::contains);


        return hasSelfHarmMeaning
                && hasImmediateTime
                && hasIntent;
    }


    /*
     * =========================================================
     * 检测：
     * 已经开始 / 正在进行 / 无法保证自己安全
     * =========================================================
     */
    private boolean containsActiveCrisisExpression(
            String message
    ) {

        /*
         * 已经开始伤害自己
         */
        if (
                message.contains("已经开始伤害自己")
                        || message.contains("正在伤害自己")
                        || message.contains("已经伤害自己")
                        || message.contains("刚刚伤害了自己")

                        || message.contains("已经开始自残")
                        || message.contains("正在自残")
                        || message.contains("已经自残")
                        || message.contains("刚刚自残")

                        || message.contains("正在自杀")
                        || message.contains("已经开始自杀")
        ) {
            return true;
        }


        /*
         * 用户明确表示无法控制当前行为。
         */
        if (
                message.contains("控制不住自己")
                        || message.contains("我控制不住了")
                        || message.contains("无法控制自己")
                        || message.contains("不能控制自己")

                        || message.contains("不能保证自己安全")
                        || message.contains("无法保证自己安全")
                        || message.contains("我不能保证自己的安全")
                        || message.contains("我无法保证自己的安全")
        ) {
            return true;
        }


        /*
         * 时间非常迫近。
         */
        if (
                message.contains("可能马上伤害自己")
                        || message.contains("马上就会伤害自己")
                        || message.contains("很快就会伤害自己")
                        || message.contains("现在就要伤害自己")

                        || message.contains("现在就要自残")
                        || message.contains("马上要自残")
                        || message.contains("马上就要自残")

                        || message.contains("现在就要自杀")
                        || message.contains("马上要自杀")
                        || message.contains("马上就要自杀")
                        || message.contains("今晚要自杀")
                        || message.contains("今天要自杀")
                        || message.contains("今天晚上要自杀")
        ) {
            return true;
        }


        return false;
    }


    /*
     * =========================================================
     * 判断是否存在明确的本人当前危机表达
     * =========================================================
     */
    private boolean containsDirectCrisisExpression(
            String message
    ) {

        return DIRECT_CRISIS_EXPRESSIONS
                .stream()
                .anyMatch(message::contains);
    }


    /*
     * =========================================================
     * 当句子中存在一个否定表达时，
     * 检查后面是否又出现了另一个明确危机表达。
     *
     * 例如：
     *
     * “我本来不想自杀，
     *  但现在我真的想死。”
     *
     * 这种不能因为前面有“不想自杀”
     * 就认为安全。
     * =========================================================
     */
    private boolean containsAnotherDirectCrisisExpression(
            String message
    ) {

        /*
         * 转折词后面的内容通常更加重要。
         */
        String[] transitionWords = {
                "但是",
                "但",
                "可是",
                "不过",
                "然而",
                "现在",
                "后来"
        };


        for (String transition : transitionWords) {

            int index = message.lastIndexOf(transition);

            if (index >= 0) {

                String afterTransition =
                        message.substring(index + transition.length());

                boolean directCrisis =
                        DIRECT_CRISIS_EXPRESSIONS
                                .stream()
                                .anyMatch(afterTransition::contains);

                /*
                 * 有些表达可能没有“我”。
                 *
                 * 例如：
                 *
                 * “我之前不想自杀，但现在想死了”
                 */
                if (
                        directCrisis
                                || afterTransition.contains("想死")
                                || afterTransition.contains("不想活")
                                || afterTransition.contains("想自残")
                                || afterTransition.contains("要自残")
                                || afterTransition.contains("想伤害自己")
                                || afterTransition.contains("要伤害自己")
                                || afterTransition.contains("想自杀")
                                || afterTransition.contains("要自杀")
                ) {
                    return true;
                }
            }
        }

        return false;
    }


    /*
     * =========================================================
     * 判断用户是否明确否认自杀 / 自伤想法
     * =========================================================
     */
    private boolean containsSelfHarmNegation(
            String message
    ) {

        return SELF_HARM_NEGATIONS
                .stream()
                .anyMatch(message::contains);
    }


    /*
     * =========================================================
     * 判断用户是否明确否认当前实施意图
     * =========================================================
     */
    private boolean containsCurrentIntentNegation(
            String message
    ) {

        return CURRENT_INTENT_NEGATIONS
                .stream()
                .anyMatch(message::contains);
    }


    /*
     * =========================================================
     * 判断是否明显在描述过去
     * =========================================================
     */
    private boolean containsHistoricalContext(
            String message
    ) {

        return HISTORICAL_TERMS
                .stream()
                .anyMatch(message::contains);
    }


    /*
     * =========================================================
     * 文本标准化
     * =========================================================
     *
     * 去掉：
     *
     * 空格
     * 换行
     * 常见中英文标点
     *
     * 这样：
     *
     * “我 想 自 杀”
     *
     * 也可以识别。
     */
    private String normalize(String message) {

        return message
                .trim()
                .replaceAll("\\s+", "")
                .replace("，", "")
                .replace("。", "")
                .replace("！", "")
                .replace("？", "")
                .replace("；", "")
                .replace("：", "")
                .replace("、", "")
                .replace(",", "")
                .replace(".", "")
                .replace("!", "")
                .replace("?", "")
                .replace(";", "")
                .replace(":", "");
    }


    /*
     * =========================================================
     * 合并 Java 规则 + AI 判断
     * =========================================================
     *
     * 风险优先级：
     *
     * crisis
     *   >
     * concern
     *   >
     * normal
     */
    public String combineRisk(
            String aiRisk,
            boolean sensitiveWordDetected,
            boolean immediateCrisisDetected
    ) {

        /*
         * Java 已经检测到明确危机。
         *
         * 优先级最高。
         *
         * 这意味着即使 AI 错误返回：
         *
         * normal
         *
         * 最终仍然是：
         *
         * crisis
         */
        if (immediateCrisisDetected) {
            return "crisis";
        }


        /*
         * 防止 AI 返回 null。
         */
        String normalizedAiRisk =
                aiRisk == null
                        ? "normal"
                        : aiRisk.trim().toLowerCase();


        /*
         * AI 根据完整上下文判断 crisis。
         */
        if ("crisis".equals(normalizedAiRisk)) {
            return "crisis";
        }


        /*
         * AI 判断 concern。
         */
        if ("concern".equals(normalizedAiRisk)) {
            return "concern";
        }


        /*
         * AI 判断 normal，
         * 但是 Java 检测到了自杀 / 自伤敏感内容。
         *
         * 为了安全，
         * 至少提升为 concern。
         */
        if (sensitiveWordDetected) {
            return "concern";
        }


        return "normal";
    }
}