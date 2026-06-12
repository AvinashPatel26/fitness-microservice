package com.fitness.aiservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service


public class GeminiService {

	private final WebClient webClient;

	@Value("${gemini.api.url}")
	private String geminiApiUrl;

	@Value("${gemini.api.key}")
	private String geminiApiKey;

	public GeminiService(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.build();
	}

	public String getRecommendation(String details) {

		Map<String, Object> requestBody = Map.of(
				"contents", List.of(
						Map.of(
								"parts", List.of(
										Map.of(
												"text",
												"You are a fitness coach. Give a short recommendation based on: "
														+ details
												)
										)
								)
						)
				);

		String response = webClient.post()
				.uri(geminiApiUrl + "?key=" + geminiApiKey)
				.bodyValue(requestBody)
				.retrieve()
				.bodyToMono(String.class)
				.block();


		return response;
	}
}
