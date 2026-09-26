package com.youthcompanion.controller;

import com.youthcompanion.model.ChatMessage;
import com.youthcompanion.model.DailyChatRequest;
import com.youthcompanion.model.DailyChatResponse;

import com.youthcompanion.service.AiService;
import com.youthcompanion.service.CrisisService;
import com.youthcompanion.service.DailyService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/daily-chat")
public class DailyChatController {

    /*
     * ==========================================
     * 服务
     * ==========================================
     */

    private final AiService aiService;

    private final CrisisService crisisService;

    private final DailyService dailyService;



    /*
     * ==========================================
     * 构造方法
     * ==========================================
     */

    public DailyChatController(

            AiService aiService,

            CrisisService crisisService,

            DailyService dailyService
    ) {

        this.aiService =
                aiService;


        this.crisisService =
                crisisService;


        this.dailyService =
                dailyService;
    }



    /*
     * ==========================================
     * POST /api/daily-chat
     * ==========================================
     */

    @PostMapping
    public DailyChatResponse chat(

            @RequestBody
            DailyChatRequest request
    ) {

        /*
         * ======================================
         * 1. 基础参数检查
         * ======================================
         */

        if (
                request == null
                        ||
                        request.messages() == null
                        ||
                        request.messages().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "messages 不能为空"
            );
        }



        /*
         * ======================================
         * 2. 清理前端消息
         * ======================================
         */

        List<ChatMessage> messages =
                cleanMessages(
                        request.messages()
                );


        if (
                messages.isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "没有有效消息"
            );
        }



        /*
         * ======================================
         * 3. 最后一条必须来自用户
         * ======================================
         */

        ChatMessage lastMessage =
                messages.get(
                        messages.size()
                                -
                                1
                );


        if (
                !"user".equals(
                        lastMessage.role()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "最后一条消息必须来自 user"
            );
        }



        String latestUserText =
                lastMessage
                        .content()
                        .trim();



        /*
         * ======================================
         * 4. 安全风险判断
         * ======================================
         *
         * 三层：
         *
         * Java 敏感词检测
         * +
         * Java 即时危机检测
         * +
         * AI 根据完整上下文分类
         */

        boolean sensitiveWordDetected =
                crisisService
                        .containsSensitiveTerms(
                                latestUserText
                        );


        boolean immediateCrisisDetected =
                crisisService
                        .containsImmediateCrisis(
                                latestUserText
                        );


        String aiRisk =
                aiService
                        .classifyRisk(
                                messages
                        );


        String riskLevel =
                crisisService
                        .combineRisk(
                                aiRisk,
                                sensitiveWordDetected,
                                immediateCrisisDetected
                        );



        /*
         * ======================================
         * 5. concern / crisis
         * ======================================
         *
         * 一旦不是 normal，
         *
         * 就停止：
         *
         * - 日程规划
         * - 任务规划
         * - 行动推荐
         * - 陪做
         *
         * 直接进入安全陪伴。
         */

        if (
                !"normal".equals(
                        riskLevel
                )
        ) {

            String reply =
                    aiService
                            .generateReply(
                                    messages,
                                    riskLevel
                            );


            return new DailyChatResponse(

                    reply,

                    0,

                    "safety",

                    List.of(),

                    List.of(),

                    riskLevel,

                    "crisis".equals(
                            riskLevel
                    )
            );
        }



        /*
         * ======================================
         * 6. AI 判断
         *
         * level
         * +
         * mode
         * ======================================
         */

        String rawClassification =
                aiService
                        .classifyDailyContext(
                                messages
                        );


        DailyService.DailyClassification
                classification =

                dailyService
                        .parseClassification(
                                rawClassification
                        );



        /*
         * ======================================
         * 7. 当前行动等级
         * ======================================
         *
         * 这个 level 只在后台使用。
         *
         * 不直接展示给用户。
         */

        int level =
                classification
                        .level();



        /*
         * ======================================
         * 8. 最终模式
         * ======================================
         *
         * AI 负责理解上下文。
         *
         * Java 再检查最后一句中
         * 是否存在非常明确的意图。
         *
         * 例如：
         *
         * “一步一步陪我”
         * → guided_step
         *
         * “帮我安排下午”
         * → daily_plan
         *
         * “我有作业和50个单词，
         *  帮我调整”
         * → task_plan
         */

        String mode =
                dailyService
                        .resolveMode(
                                latestUserText,
                                classification.mode()
                        );



        /*
         * ======================================
         * 可选调试
         * ======================================
         *
         * 如果你要看模式识别过程，
         * 可以暂时取消注释。
         *
         * 正式展示时建议保持注释。
         */

        /*
        System.out.println(
                "DAILY DEBUG"
                +
                " | AI raw = "
                +
                rawClassification
                +
                " | level = "
                +
                level
                +
                " | AI mode = "
                +
                classification.mode()
                +
                " | final mode = "
                +
                mode
        );
        */



        /*
         * ======================================
         * 9. 准备返回数据
         * ======================================
         */

        List<String> recommendations =
                new ArrayList<>();


        List<String> plan =
                new ArrayList<>();


        String reply;



        /*
         * ======================================
         * 10. task_plan
         * ======================================
         *
         * 用户已经有明确任务。
         *
         * 例如：
         *
         * - 一项作业
         * - 50个单词
         * - 复习两章
         * - 准备一个PPT
         *
         * 这里不能再简单随机返回
         * “看窗外 / 摸墙 / 擦杯子”。
         *
         * 正确流程：
         *
         * ① 从行动库取适合当前状态的
         *    放松 / 恢复候选
         *
         * ② 把：
         *
         *    用户任务
         *    +
         *    当前状态
         *    +
         *    恢复动作
         *
         *    一起交给 AI
         *
         * ③ AI 重新拟合：
         *
         *    任务
         *    +
         *    休息
         *    +
         *    任务
         *
         * ④ 推荐卡片也改成
         *    与真实任务相关的最小启动动作。
         */

        if (
                "task_plan".equals(
                        mode
                )
        ) {

            /*
             * 行动库在 task_plan 中
             * 不直接作为推荐卡片。
             *
             * 这里只作为“恢复动作候选”。
             */
            List<String> relaxationCandidates =
                    dailyService
                            .getRelaxationCandidates(
                                    level
                            );


            /*
             * AI 根据：
             *
             * - 完整聊天上下文
             * - 当前 level
             * - 放松动作候选
             *
             * 生成新的任务型计划。
             */
            AiService.DailyTaskPlanResult
                    taskResult =

                    aiService
                            .generateTaskAwarePlan(

                                    messages,

                                    level,

                                    relaxationCandidates
                            );



            /*
             * AI自然语言回复
             */
            reply =
                    taskResult
                            .reply();



            /*
             * 完整轻量任务日程
             */
            if (
                    taskResult.plan()
                            !=
                            null
            ) {

                plan =
                        new ArrayList<>(
                                taskResult
                                        .plan()
                        );
            }



            /*
             * 与用户任务相关的
             * 3个最小启动动作。
             */
            if (
                    taskResult.recommendations()
                            !=
                            null
            ) {

                recommendations =
                        new ArrayList<>(
                                taskResult
                                        .recommendations()
                        );
            }



            /*
             * ======================================
             * 11. 普通 daily_plan
             * ======================================
             *
             * 用户没有明确具体任务，
             * 只是：
             *
             * “帮我安排下午”
             *
             * “今天怎么过”
             *
             * 此时仍然从行动库组合
             * 一个轻量生活日程。
             */

        } else if (
                "daily_plan".equals(
                        mode
                )
        ) {

            plan =
                    dailyService
                            .buildLightPlan(
                                    level
                            );


            reply =
                    aiService
                            .generateDailyReply(

                                    messages,

                                    level,

                                    mode,

                                    recommendations,

                                    plan
                            );



            /*
             * ======================================
             * 12. guided_step
             * ======================================
             *
             * 一次只返回一个下一步。
             */

        } else if (
                "guided_step".equals(
                        mode
                )
        ) {

            recommendations =
                    dailyService
                            .getGuidedStep(
                                    level
                            );


            reply =
                    aiService
                            .generateDailyReply(

                                    messages,

                                    level,

                                    mode,

                                    recommendations,

                                    plan
                            );



            /*
             * ======================================
             * 13. micro_action
             * ======================================
             *
             * 默认返回3个低压力行动。
             */

        } else {

            mode =
                    "micro_action";


            recommendations =
                    dailyService
                            .getRecommendations(
                                    level,
                                    3
                            );


            reply =
                    aiService
                            .generateDailyReply(

                                    messages,

                                    level,

                                    mode,

                                    recommendations,

                                    plan
                            );
        }



        /*
         * ======================================
         * 14. 返回前端
         * ======================================
         */

        return new DailyChatResponse(

                reply,

                level,

                mode,

                recommendations,

                plan,

                "normal",

                false
        );
    }



    /*
     * ==========================================
     * 清理聊天消息
     * ==========================================
     *
     * 作用：
     *
     * 1. 只保留最近16条
     *
     * 2. 忽略空消息
     *
     * 3. 单条最多3000字符
     *
     * 4. 只接受：
     *
     *    user
     *    assistant
     *
     * 5. 禁止前端伪造 system
     */

    private List<ChatMessage> cleanMessages(

            List<ChatMessage> source
    ) {

        List<ChatMessage> result =
                new ArrayList<>();



        if (
                source == null
                        ||
                        source.isEmpty()
        ) {

            return result;
        }



        /*
         * 最多保留最近16条。
         */
        int start =
                Math.max(
                        0,
                        source.size()
                                -
                                16
                );



        for (
                int i = start;
                i < source.size();
                i++
        ) {

            ChatMessage message =
                    source.get(
                            i
                    );


            /*
             * 空对象忽略。
             */
            if (
                    message == null
                            ||
                            message.content() == null
            ) {

                continue;
            }



            String content =
                    message
                            .content()
                            .trim();



            /*
             * 空文本忽略。
             */
            if (
                    content.isEmpty()
            ) {

                continue;
            }



            /*
             * 防止一次提交过大的文本。
             */
            if (
                    content.length()
                            >
                            3000
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "单条消息不能超过3000字符"
                );
            }



            String role =
                    message.role();



            /*
             * 只允许 user / assistant。
             *
             * 不允许前端发送 system，
             * 防止用户覆盖后台 Prompt。
             */
            if (
                    !"user".equals(
                            role
                    )
                            &&
                            !"assistant".equals(
                                    role
                            )
            ) {

                continue;
            }



            result.add(

                    new ChatMessage(

                            role,

                            content
                    )
            );
        }



        return result;
    }
}