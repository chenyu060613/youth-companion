package com.youthcompanion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;


@Service
public class CrisisAlertMailService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CrisisAlertMailService.class
            );


    private final JavaMailSender mailSender;

    private final String from;

    private final String alertRecipients;

    private final boolean enabled;


    public CrisisAlertMailService(

            JavaMailSender mailSender,

            @Value("${alert.mail.from}")
            String from,

            @Value("${alert.mail.to}")
            String alertRecipients,

            @Value("${alert.mail.enabled:true}")
            boolean enabled
    ) {

        this.mailSender =
                mailSender;

        this.from =
                from;

        this.alertRecipients =
                alertRecipients;

        this.enabled =
                enabled;
    }


    /*
     * =========================================================
     * 发送危机安全预警邮件
     * =========================================================
     *
     * 使用 @Async：
     *
     * 邮件发送不会阻塞聊天接口。
     *
     * 即使 SMTP 比较慢，
     * 用户也不需要一直等待。
     */
    @Async
    public void sendCrisisAlert(

            String userMessage,

            String finalRisk,

            String aiRisk,

            boolean immediateCrisisDetected
    ) {

        /*
         * 可以通过配置临时关闭邮件。
         */
        if (!enabled) {

            log.warn(
                    "危机邮件功能当前已关闭"
            );

            return;
        }


        try {

            String[] recipients =
                    Arrays.stream(
                                    alertRecipients.split(",")
                            )
                            .map(String::trim)
                            .filter(
                                    item ->
                                            !item.isBlank()
                            )
                            .toArray(
                                    String[]::new
                            );


            if (recipients.length == 0) {

                log.error(
                        "没有配置安全预警邮件收件人"
                );

                return;
            }


            /*
             * 当前时间。
             */
            String time =
                    LocalDateTime
                            .now()
                            .format(
                                    DateTimeFormatter
                                            .ofPattern(
                                                    "yyyy-MM-dd HH:mm:ss"
                                            )
                            );


            /*
             * 构建邮件。
             */
            SimpleMailMessage mail =
                    new SimpleMailMessage();


            mail.setFrom(
                    from
            );


            mail.setTo(
                    recipients
            );


            mail.setSubject(
                    "【Youth Companion 安全预警】检测到高风险自伤/自杀表达"
            );


            String content =
                    """
                    Youth Companion 安全系统检测到一条高风险信息。

                    ==============================
                    风险信息
                    ==============================

                    时间：
                    %s

                    最终风险等级：
                    %s

                    AI风险判断：
                    %s

                    Java即时危机检测：
                    %s


                    ==============================
                    用户当前消息
                    ==============================

                    %s


                    ==============================
                    说明
                    ==============================

                    系统检测到用户可能存在当前自杀、自残
                    或其他迫近的人身安全风险。

                    请由有权限并承担安全响应职责的人员
                    根据既定安全处理流程进行人工确认。

                    此邮件由 Youth Companion
                    自动安全预警系统发送。
                    """
                            .formatted(

                                    time,

                                    finalRisk,

                                    aiRisk,

                                    immediateCrisisDetected
                                            ? "是"
                                            : "否",

                                    userMessage
                            );


            mail.setText(
                    content
            );


            /*
             * 真正发送邮件。
             */
            mailSender.send(
                    mail
            );


            log.warn(
                    "危机安全预警邮件已发送，risk={}",
                    finalRisk
            );


        } catch (Exception e) {

            /*
             * 非常重要：
             *
             * 邮件发送失败不能让聊天接口一起失败。
             */
            log.error(
                    "危机安全预警邮件发送失败",
                    e
            );
        }
    }
}