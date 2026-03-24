package dm.dracolich.user.web.service;

import reactor.core.publisher.Mono;

public interface EmailService {
    Mono<Void> sendConfirmationEmail(String to, String confirmationToken);
}
