package com.youthcompanion.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MailTestController {

    private final JavaMailSender mailSender;

    private final String from;

    private final String to;


    public MailTestController(

            JavaMailSender mailSender,

            @Value("${alert.mail.from}")
            String from,

            @Value("${alert.mail.to}")
            String to
    ) {

        this.mailSender = mailSender;

        this.from = from;

        this.to = to;
    }


    @GetMapping("/api/mail-test")
    public String testMail() {
        String password =
                System.getenv("ALERT_MAIL_PASSWORD");

        System.out.println(
                "PASSWORD EXISTS = "
                        + (password != null)
        );

        System.out.println(
                "PASSWORD LENGTH = "
                        + (
                        password == null
                                ? 0
                                : password.length()
                )
        );

        System.out.println(
                "PASSWORD STARTS WITH SPACE = "
                        + (
                        password != null
                                && !password.isEmpty()
                                && Character.isWhitespace(
                                password.charAt(0)
                        )
                )
        );

        System.out.println(
                "PASSWORD ENDS WITH SPACE = "
                        + (
                        password != null
                                && !password.isEmpty()
                                && Character.isWhitespace(
                                password.charAt(
                                        password.length() - 1
                                )
                        )
                )
        );

        try {

            System.out.println(
                    "===== MAIL TEST ====="
            );

            System.out.println(
                    "FROM = " + from
            );

            System.out.println(
                    "TO = " + to
            );


            SimpleMailMessage message =
                    new SimpleMailMessage();


            message.setFrom(
                    from
            );


            message.setTo(
                    to.split(",")
            );


            message.setSubject(
                    "Youth Companion 邮件测试"
            );


            message.setText(
                    """
                    如果你收到这封邮件，
                    说明 Youth Companion 的 SMTP 配置已经正常工作。

                    这是一封测试邮件。
                    """
            );


            mailSender.send(
                    message
            );


            System.out.println(
                    "邮件发送成功"
            );


            return "邮件发送成功，请检查收件箱";


        } catch (Exception e) {

            /*
             * 为了调试，
             * 这里直接把错误完整输出到 IDEA 控制台。
             */
            e.printStackTrace();


            return "邮件发送失败：" +
                    e.getClass().getName()
                    +
                    "："
                    +
                    e.getMessage();
        }
    }
}