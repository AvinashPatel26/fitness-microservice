package com.fitness.gateway.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final WebClient userServiceWebClient;

    public Mono<Boolean> validateUser(String userId) {

        log.info("Calling user service for {}", userId);

        try {

            return userServiceWebClient.get()
                    .uri("/api/users/{userId}/validate", userId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .onErrorResume(WebClientResponseException.class, e -> {

                        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(
                                    new RuntimeException("User not found: " + userId));
                        }

                        if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                            return Mono.error(
                                    new RuntimeException("Invalid userId: " + userId));
                        }

                        return Mono.error(
                                new RuntimeException("Unexpected error: "
                                        + e.getStatusCode()));
                    });

        } catch (WebClientRequestException e) {

            log.error("Error calling User Service", e);

            return Mono.just(false);
        }
        
        
    }
    
    public Mono<UserResponse> registerUser(RegisterRequest registerRequest) {

        log.info("Calling User Registration for {}", registerRequest.getEmail());

        return userServiceWebClient.post()
                .uri("/api/users/register")
                .bodyValue(registerRequest)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .onErrorResume(WebClientResponseException.class, e -> {

                    
                    if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        return Mono.error(
                                new RuntimeException("Bad request : "
                                        + e.getMessage()));
                    }

                    return Mono.error(
                            new RuntimeException("Unexpected error : "
                                    + e.getMessage()));
                });
    }
}