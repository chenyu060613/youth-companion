package com.youthcompanion.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
public class DailyService {

    private final List<DailyAction> actions;


    public DailyService() throws IOException {

        ClassPathResource resource =
                new ClassPathResource(
                        "data/actions.json"
                );


        JsonMapper jsonMapper =
                JsonMapper
                        .builder()
                        .build();


        try (
                InputStream inputStream =
                        resource.getInputStream()
        ) {

            actions =
                    jsonMapper.readValue(
                            inputStream,
                            new TypeReference<
                                    List<DailyAction>
                                    >() {
                            }
                    );
        }


        if (
                actions == null
                        ||
                        actions.isEmpty()
        ) {

            throw new IllegalStateException(
                    "actions.json 行动库为空"
            );
        }
    }



    /*
     * ==========================================
     * 解析 AI 分类
     * ==========================================
     */

    public DailyClassification parseClassification(
            String raw
    ) {

        int level = 2;

        String mode =
                "micro_action";


        if (
                raw == null
                        ||
                        raw.isBlank()
        ) {

            return new DailyClassification(
                    level,
                    mode
            );
        }


        String cleaned =
                raw
                        .trim()
                        .toLowerCase()
                        .replace("`", "")
                        .replace(" ", "")
                        .replace("\n", "")
                        .replace("\r", "");



        /*
         * 找 Level
         */
        for (
                int i = 0;
                i < cleaned.length();
                i++
        ) {

            char c =
                    cleaned.charAt(i);


            if (
                    c >= '1'
                            &&
                            c <= '4'
            ) {

                level =
                        c - '0';

                break;
            }
        }



        /*
         * 找 Mode
         *
         * task_plan 必须放在 daily_plan 前面判断。
         */
        if (
                cleaned.contains(
                        "task_plan"
                )
        ) {

            mode =
                    "task_plan";

        } else if (
                cleaned.contains(
                        "guided_step"
                )
        ) {

            mode =
                    "guided_step";

        } else if (
                cleaned.contains(
                        "daily_plan"
                )
        ) {

            mode =
                    "daily_plan";

        } else {

            mode =
                    "micro_action";
        }


        return new DailyClassification(
                level,
                mode
        );
    }



    /*
     * ==========================================
     * Java 模式兜底
     * ==========================================
     */

    public String resolveMode(

            String userText,

            String aiMode
    ) {

        String fallbackMode =
                normalizeMode(
                        aiMode
                );


        if (
                userText == null
                        ||
                        userText.isBlank()
        ) {

            return fallbackMode;
        }


        String text =
                userText
                        .trim()
                        .replace(" ", "")
                        .replace("\n", "")
                        .replace("\r", "");



        /*
         * ======================================
         * 1. 陪做优先
         * ======================================
         */

        if (
                containsAny(
                        text,

                        "一步一步",
                        "一步步",
                        "陪我做",
                        "陪着我做",
                        "带我做",
                        "陪我开始",
                        "带我开始",
                        "只告诉我下一步",
                        "只给我下一步",
                        "先告诉我做什么",
                        "我做完告诉你",
                        "一次只做一个"
                )
        ) {

            return "guided_step";
        }



        /*
         * ======================================
         * 2. 用户已经有具体任务
         * ======================================
         *
         * 同时满足：
         *
         * 有任务内容
         * +
         * 有安排 / 调整 / 规划意图
         */

        boolean hasTask =
                containsAny(
                        text,

                        "作业",
                        "功课",
                        "背单词",
                        "单词",
                        "复习",
                        "预习",
                        "考试",
                        "刷题",
                        "练习题",
                        "看书",
                        "读书",
                        "论文",
                        "报告",
                        "作文",
                        "整理笔记",
                        "做题",
                        "学习",
                        "课程",
                        "网课",
                        "任务",
                        "项目",
                        "稿子",
                        "PPT",
                        "汇报",
                        "收拾房间",
                        "整理房间",
                        "洗衣服",
                        "打扫",
                        "买东西",
                        "运动",
                        "跑步"
                );


        boolean hasPlanningIntent =
                containsAny(
                        text,

                        "安排",
                        "规划",
                        "计划",
                        "调整",
                        "怎么做",
                        "怎么完成",
                        "怎么安排",
                        "帮我理",
                        "帮我排",
                        "今天要",
                        "下午要",
                        "晚上要",
                        "明天要",
                        "需要完成",
                        "得完成",
                        "必须完成"
                );


        if (
                hasTask
                        &&
                        hasPlanningIntent
        ) {

            return "task_plan";
        }



        /*
         * ======================================
         * 3. 普通日程
         * ======================================
         */

        if (
                containsAny(
                        text,

                        "安排今天",
                        "安排下午",
                        "安排晚上",
                        "安排上午",

                        "今天怎么过",
                        "下午怎么过",
                        "晚上怎么过",
                        "接下来怎么过",

                        "帮我安排",
                        "帮我规划",
                        "帮我计划",

                        "理一下今天",
                        "理一下下午",
                        "理一下晚上",

                        "不要安排太满",
                        "别安排太满",
                        "轻轻安排"
                )
        ) {

            return "daily_plan";
        }


        return fallbackMode;
    }



    /*
     * ==========================================
     * 模式合法化
     * ==========================================
     */

    private String normalizeMode(
            String mode
    ) {

        if (
                "task_plan".equals(mode)
        ) {

            return "task_plan";
        }


        if (
                "guided_step".equals(mode)
        ) {

            return "guided_step";
        }


        if (
                "daily_plan".equals(mode)
        ) {

            return "daily_plan";
        }


        return "micro_action";
    }



    /*
     * ==========================================
     * 普通微行动
     * ==========================================
     */

    public List<String> getRecommendations(

            int level,

            int count
    ) {

        int safeLevel =
                normalizeLevel(
                        level
                );


        List<DailyAction> candidates =
                getActionsForLevel(
                        safeLevel
                );


        Collections.shuffle(
                candidates
        );


        int resultCount =
                Math.min(
                        Math.max(
                                count,
                                1
                        ),
                        candidates.size()
                );


        List<String> result =
                new ArrayList<>();


        for (
                int i = 0;
                i < resultCount;
                i++
        ) {

            DailyAction action =
                    candidates.get(i);


            if (
                    action.text() != null
                            &&
                            !action.text().isBlank()
            ) {

                result.add(
                        action
                                .text()
                                .trim()
                );
            }
        }


        return result;
    }



    /*
     * ==========================================
     * 给 task_plan 提供“放松候选动作”
     * ==========================================
     *
     * 注意：
     *
     * 这些不是最终下面的三张卡片。
     *
     * 它们只是给 AI 作为：
     *
     * 任务之间的休息 / 过渡动作。
     */

    public List<String> getRelaxationCandidates(
            int level
    ) {

        return getRecommendations(
                level,
                3
        );
    }



    /*
     * ==========================================
     * guided step
     * ==========================================
     */

    public List<String> getGuidedStep(
            int level
    ) {

        return getRecommendations(
                level,
                1
        );
    }



    /*
     * ==========================================
     * 没有具体任务时的轻量日程
     * ==========================================
     */

    public List<String> buildLightPlan(
            int level
    ) {

        int safeLevel =
                normalizeLevel(
                        level
                );


        int count =
                safeLevel <= 2
                        ?
                        3
                        :
                        4;


        List<String> selected =
                getRecommendations(
                        safeLevel,
                        count
                );


        List<String> plan =
                new ArrayList<>();


        if (
                selected.size() >= 1
        ) {

            plan.add(
                    "现在："
                            +
                            selected.get(0)
            );
        }


        if (
                selected.size() >= 2
        ) {

            plan.add(
                    "稍微缓一会儿之后："
                            +
                            selected.get(1)
            );
        }


        if (
                selected.size() >= 3
        ) {

            plan.add(
                    "如果状态还可以："
                            +
                            selected.get(2)
            );
        }


        if (
                selected.size() >= 4
        ) {

            plan.add(
                    "晚一点还有余力的话："
                            +
                            selected.get(3)
            );
        }


        return plan;
    }



    /*
     * ==========================================
     * 获取等级行动
     * ==========================================
     */

    private List<DailyAction> getActionsForLevel(
            int level
    ) {

        List<DailyAction> result =
                new ArrayList<>();


        for (
                DailyAction action
                :
                actions
        ) {

            if (
                    action == null
            ) {

                continue;
            }


            if (
                    action.level()
                            ==
                            level
            ) {

                result.add(
                        action
                );
            }
        }


        return result;
    }



    private int normalizeLevel(
            int level
    ) {

        if (
                level < 1
        ) {

            return 1;
        }


        if (
                level > 4
        ) {

            return 4;
        }


        return level;
    }



    private boolean containsAny(

            String text,

            String... keywords
    ) {

        for (
                String keyword
                :
                keywords
        ) {

            if (
                    keyword != null
                            &&
                            !keyword.isBlank()
                            &&
                            text.contains(
                                    keyword
                            )
            ) {

                return true;
            }
        }


        return false;
    }



    private record DailyAction(

            String id,

            int level,

            String text
    ) {
    }



    public record DailyClassification(

            int level,

            String mode
    ) {
    }
}