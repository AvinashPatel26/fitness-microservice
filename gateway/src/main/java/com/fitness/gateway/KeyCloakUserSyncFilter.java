package com.fitness.gateway;

import java.text.ParseException;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeyCloakUserSyncFilter implements WebFilter {

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        RegisterRequest registerRequest = getUserDetails(token);

        if (userId == null && registerRequest != null) {
            userId = registerRequest.getKeyCloakId();
        }

        final String finalUserId = userId;

        if (finalUserId != null && token != null) {

            return userService.validateUser(finalUserId)
                    .flatMap(exist -> {

                        if (!exist) {

                            if (registerRequest != null) {

                                return userService.registerUser(registerRequest)
                                        .then();

                            } else {

                                return Mono.empty();
                            }

                        } else {

                            log.info("User already exist, Skipping sync");

                            return Mono.empty();
                        }
                    })
                    .then(Mono.defer(() -> {

                        ServerHttpRequest mutatedRequest = exchange.getRequest()
                                .mutate()
                                .header("X-User-ID", finalUserId)
                                .build();

                        return chain.filter(
                                exchange.mutate()
                                        .request(mutatedRequest)
                                        .build());
                    }));
        }

        return chain.filter(exchange);
    }

    private RegisterRequest getUserDetails(String token) {
        if (token == null) return null;
        try {
            String tokenWithoutBearer = token.replace("Bearer", "").trim();

            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            RegisterRequest request = new RegisterRequest();
            request.setEmail(claims.getStringClaim("email"));
            request.setKeyCloakId(claims.getStringClaim("sub"));
            request.setFirstName(claims.getStringClaim("given_name"));
            request.setLastName(claims.getStringClaim("family_name"));
            request.setPassword("dummy@123");

            return request;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}