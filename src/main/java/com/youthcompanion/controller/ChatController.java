package com.youthcompanion.controller;

import com.youthcompanion.model.ChatMessage;
import com.youthcompanion.model.ChatRequest;
import com.youthcompanion.model.ChatResponse;

import com.youthcompanion.service.AiService;
import com.youthcompanion.service.CrisisAlertMailService;
import com.youthcompanion.service.CrisisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ChatController.class
            );


    private final AiService aiService;

    private final CrisisService crisisService;

    private final CrisisAlertMailService crisisAlertMailService;


    /*
     * =========================================================
     * 构造方法
     * =========================================================
     */
    public ChatController(

            AiService aiService,

            CrisisService crisisService,

            CrisisAlertMailService crisisAlertMailService
    ) {

        this.aiService =
                aiService;

        this.crisisService =
                crisisService;

        this.crisisAlertMailService =
                crisisAlertMailService;
    }


    /*
     * =========================================================
     * POST /api/chat
     * =========================================================
     */
    @PostMapping
    public ChatResponse chat(

            @RequestBody
            ChatRequest request
    ) {


        /*
         * =====================================================
         * 1. 基础校验
         * =====================================================
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
         * =====================================================
         * 2. 清理前端传来的消息
         * =====================================================
         */
        List<ChatMessage> messages =
                cleanMessages(
                        request.messages()
                );


        if (messages.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "没有有效消息"
            );
        }


        /*
         * =====================================================
         * 3. 最后一条必须来自 user
         * =====================================================
         */
        ChatMessage lastMessage =
                messages.get(
                        messages.size() - 1
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


        String userMessage =
                lastMessage.content();


        /*
         * =====================================================
         * 4. 第一层：
         *
         * Java 本地快速安全检测
         * =====================================================
         */
        boolean sensitiveWordDetected =
                crisisService
                        .containsSensitiveTerms(
                                userMessage
                        );


        boolean immediateCrisisDetected =
                crisisService
                        .containsImmediateCrisis(
                                userMessage
                        );


        /*
         * =====================================================
         * 5. 风险判断
         * =====================================================
         *
         * 如果 Java 已经检测到非常明确的危机，
         * 不需要等待 AI 再确认。
         *
         * 直接 crisis。
         */
        String aiRisk;

        String finalRisk;


        if (immediateCrisisDetected) {

            /*
             * Java 已经明确识别为即时危机。
             */
            aiRisk =
                    "skipped";

            finalRisk =
                    "crisis";


        } else {

            /*
             * Java 没有检测到明确即时危机，
             * 再让 AI 根据完整聊天上下文判断。
             */
            try {

                aiRisk =
                        aiService
                                .classifyRisk(
                                        messages
                                );


            } catch (Exception e) {

                /*
                 * AI 风险分类失败时：
                 *
                 * 如果出现敏感内容，
                 * 至少按 concern 处理。
                 *
                 * 避免 AI API 故障直接让安全机制消失。
                 */
                log.error(
                        "AI风险分类失败",
                        e
                );


                aiRisk =
                        sensitiveWordDetected
                                ? "concern"
                                : "normal";
            }


            /*
             * Java + AI 合并。
             */
            finalRisk =
                    crisisService
                            .combineRisk(
                                    aiRisk,
                                    sensitiveWordDetected,
                                    false
                            );
        }


        /*
         * =====================================================
         * 6. 安全日志
         * =====================================================
         */
        log.info(
                "SAFETY CHECK | sensitive={} | immediate={} | aiRisk={} | finalRisk={}",
                sensitiveWordDetected,
                immediateCrisisDetected,
                aiRisk,
                finalRisk
        );


        /*
         * =====================================================
         * 7. 如果最终结果是 crisis：
         *
         * 立即触发安全预警邮件
         * =====================================================
         *
         * 两种情况都会进来：
         *
         * 1. Java 明确判断 crisis
         * 2. AI 根据上下文判断 crisis
         */
        if (
                "crisis".equals(
                        finalRisk
                )
        ) {

            crisisAlertMailService
                    .sendCrisisAlert(

                            userMessage,

                            finalRisk,

                            aiRisk,

                            immediateCrisisDetected
                    );
        }


        /*
         * =====================================================
         * 8. 生成陪伴回复
         * =====================================================
         */
        String reply;


        try {

            reply =
                    aiService
                            .generateReply(
                                    messages,
                                    finalRisk
                            );


        } catch (Exception e) {

            log.error(
                    "AI生成回复失败，risk={}",
                    finalRisk,
                    e
            );


            /*
             * =================================================
             * 非常重要：
             *
             * 如果是 crisis，
             * 即使 AI API 挂了，
             * 也不能只给用户 HTTP 500。
             *
             * 返回一个本地固定安全回复。
             * =================================================
             */
            if (
                    "crisis".equals(
                            finalRisk
                    )
            ) {

                reply =
                        """
                        我看到你现在可能正处在一个很危险、很难熬的时刻。

                        现在先不要一个人待着，尽量去到有其他人在的地方，也尽量和可能伤害自己的物品拉开一点距离。

                        如果可以，现在联系一个你信任的人，让对方知道你目前的情况。如果危险正在发生、你已经受伤，或者感觉自己马上可能行动，请优先寻求现实中的紧急帮助。
                        """;


            } else {

                /*
                 * 非 crisis 情况下，
                 * 保持原来的错误处理。
                 */
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI服务暂时不可用"
                );
            }
        }


        /*
         * =====================================================
         * 9. crisis 才打开危机弹窗
         * =====================================================
         */
        boolean showCrisisModal =
                "crisis".equals(
                        finalRisk
                );


        /*
         * =====================================================
         * 10. 返回浏览器
         * =====================================================
         */
        return new ChatResponse(

                reply,

                finalRisk,

                showCrisisModal
        );
    }


    /*
     * =========================================================
     * 对前端消息做基础清理
     * =========================================================
     */
    private List<ChatMessage> cleanMessages(

            List<ChatMessage> source
    ) {

        List<ChatMessage> result =
                new ArrayList<>();


        /*
         * 第一版只允许最近 12 条。
         */
        int start =
                Math.max(
                        0,
                        source.size() - 12
                );


        for (
                int i = start;
                i < source.size();
                i++
        ) {

            ChatMessage message =
                    source.get(i);


            if (message == null) {
                continue;
            }


            if (message.content() == null) {
                continue;
            }


            String content =
                    message
                            .content()
                            .trim();


            if (content.isEmpty()) {
                continue;
            }


            /*
             * 防止用户一次发送非常大的文本。
             */
            if (content.length() > 3000) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "单条消息过长"
                );
            }


            /*
             * 前端不允许发送 system。
             */
            if (
                    !"user".equals(
                            message.role()
                    )
                            &&
                            !"assistant".equals(
                                    message.role()
                            )
            ) {

                continue;
            }


            result.add(
                    new ChatMessage(
                            message.role(),
                            content
                    )
            );
        }


        return result;
    }
}