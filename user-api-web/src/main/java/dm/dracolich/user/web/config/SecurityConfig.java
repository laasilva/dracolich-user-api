package dm.dracolich.user.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dm.dracolich.forge.response.DmdResponse;
import dm.dracolich.forge.security.JwtAuthenticationWebFilter;
import dm.dracolich.forge.security.JwtTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http,
                                                       JwtAuthenticationWebFilter jwtFilter,
                                                       ObjectMapper objectMapper) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper))
                )
                .build();
    }

    @Bean
    public JwtAuthenticationWebFilter jwtAuthenticationWebFilter(JwtTokenValidator tokenValidator) {
        return new JwtAuthenticationWebFilter(tokenValidator);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private ServerAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (exchange, ex) -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DmdResponse<?> response = new DmdResponse<>(null, HttpStatus.UNAUTHORIZED, "Authentication required.");
            return writeResponse(exchange.getResponse(), objectMapper, response);
        };
    }

    private ServerAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (exchange, denied) -> {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DmdResponse<?> response = new DmdResponse<>(null, HttpStatus.FORBIDDEN, "Access denied.");
            return writeResponse(exchange.getResponse(), objectMapper, response);
        };
    }

    private Mono<Void> writeResponse(org.springframework.http.server.reactive.ServerHttpResponse response,
                                      ObjectMapper objectMapper, DmdResponse<?> body) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(body))
                .flatMap(bytes -> {
                    DataBuffer buffer = response.bufferFactory().wrap(bytes);
                    return response.writeWith(Mono.just(buffer));
                });
    }
}
