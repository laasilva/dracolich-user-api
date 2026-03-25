package dm.dracolich.user.web.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.confirmation-url}")
    private String confirmationUrl;

    @Value("${app.from-address}")
    private String fromAddress;

    @Override
    public Mono<Void> sendConfirmationEmail(String to, String confirmationToken) {
        return Mono.fromCallable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Dracolich - Confirm your account");
            message.setText("""
                    Welcome to Dracolich!

                    Please confirm your account by clicking the link below:

                    %s?token=%s

                    This link will expire in 24 hours.

                    If you did not create this account, please ignore this email.
                    """.formatted(confirmationUrl, confirmationToken));
            mailSender.send(message);
            return true;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
