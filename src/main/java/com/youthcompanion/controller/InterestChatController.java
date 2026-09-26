package com.youthcompanion.controller;

import com.youthcompanion.model.ChatMessage;
import com.youthcompanion.model.ChatResponse;
import com.youthcompanion.model.InterestChatRequest;
import com.youthcompanion.service.AiService;
import com.youthcompanion.service.CrisisService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/interest-chat")
public class InterestChatController {

    private final AiService aiService;

    private final CrisisService crisisService;


    public InterestChatController(
            AiService aiService,
            CrisisService crisisService
    ) {

        this.aiService =
                aiService;

        this.crisisService =
                crisisService;
    }


    @PostMapping
    public ChatResponse chat(
            @RequestBody InterestChatRequest request
    ) {

        /*
         * 检查项目内容
         */

        if (
                request == null
                        ||
                        request.interestTitle() == null
                        ||
                        request.projectIdea() == null
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "兴趣项目信息不能为空"
            );
        }


        if (
                request.messages() == null
                        ||
                        request.messages().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "messages 不能为空"
            );
        }


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


        /*
         * -----------------------------------------
         * 安全检测
         * -----------------------------------------
         */

        boolean sensitiveWordDetected =
                crisisService
                        .containsSensitiveTerms(
                                lastMessage.content()
                        );


        boolean immediateCrisisDetected =
                crisisService
                        .containsImmediateCrisis(
                                lastMessage.content()
                        );


        String aiRisk =
                aiService
                        .classifyRisk(
                                messages
                        );


        String finalRisk =
                crisisService
                        .combineRisk(
                                aiRisk,
                                sensitiveWordDetected,
                                immediateCrisisDetected
                        );


        /*
         * -----------------------------------------
         * 正常状态：
         * 使用兴趣陪伴 Prompt
         *
         * concern / crisis：
         * 切换成安全陪伴 Prompt
         * -----------------------------------------
         */

        String reply;


        if (
                "normal".equals(
                        finalRisk
                )
        ) {

            reply =
                    aiService
                            .generateInterestReply(
                                    request.interestTitle(),
                                    request.mode(),
                                    request.projectIdea(),
                                    messages
                            );

        } else {

            reply =
                    aiService
                            .generateReply(
                                    messages,
                                    finalRisk
                            );
        }


        boolean showCrisisModal =
                "crisis".equals(
                        finalRisk
                );


        return new ChatResponse(
                reply,
                finalRisk,
                showCrisisModal
        );
    }



    /*
     * 清理前端聊天内容
     */
    private List<ChatMessage> cleanMessages(
            List<ChatMessage> source
    ) {

        List<ChatMessage> result =
                new ArrayList<>();


        /*
         * 兴趣项目暂时给最近20条上下文。
         */
        int start =
                Math.max(
                        0,
                        source.size() - 20
                );


        for (
                int i = start;
                i < source.size();
                i++
        ) {

            ChatMessage message =
                    source.get(i);


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


            if (content.isEmpty()) {

                continue;
            }


            if (
                    content.length() > 3000
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "单条消息过长"
                );
            }


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