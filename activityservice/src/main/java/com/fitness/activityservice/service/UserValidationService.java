package com.fitness.activityservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {
	private final WebClient userServiceWebClient;


	public boolean validateUser(String userId) {
		log.info("calling user service for {}", userId);
		try {
			return userServiceWebClient.get()
					.uri("/api/users/{userId}/validate", userId)
					.retrieve()
					.bodyToMono(Boolean.class)
					.block();
		} catch (WebClientRequestException e){
			e.printStackTrace();
		}
		return false;
	}
}
