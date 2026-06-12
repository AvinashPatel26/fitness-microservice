package com.fitness.aiservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public Recommendation generateRecommendation(Activity activity) {

        String prompt = createPromptForActivity(activity);

        String aiResponse = geminiService.getRecommendation(prompt);

        log.info(
                "Received Gemini response ({} chars)",
                aiResponse != null ? aiResponse.length() : 0
        );

        return processAIResponse(activity, aiResponse);
    }

    private Recommendation processAIResponse(
            Activity activity,
            String aiResponse) {

        try {

            JsonNode rootNode =
                    objectMapper.readTree(aiResponse);

            JsonNode candidates =
                    rootNode.path("candidates");

            if (!candidates.isArray()
                    || candidates.isEmpty()) {

                throw new IllegalStateException(
                        "No candidates returned from Gemini"
                );
            }

            JsonNode textNode =
                    candidates.get(0)
                            .path("content")
                            .path("parts")
                            .path(0)
                            .path("text");

            String responseText =
                    textNode.asText();

            if (responseText == null
                    || responseText.isBlank()) {

                throw new IllegalStateException(
                        "Gemini returned empty content"
                );
            }

            String jsonContent =
                    extractJson(responseText);

            JsonNode recommendationJson =
                    objectMapper.readTree(jsonContent);

            String completeJsonResponse =
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(
                                    recommendationJson
                            );

            List<String> improvements =
                    extractImprovements(
                            recommendationJson.path(
                                    "recommendations")
                    );

            List<String> suggestions =
                    extractSuggestions(
                            recommendationJson.path(
                                    "suggestedWorkouts")
                    );

            List<String> safety =
                    extractSafetyTips(
                            recommendationJson.path(
                                    "safetyTips")
                    );

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .type(activity.getType().name())
                    .recommendation(
                            completeJsonResponse
                    )
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safety)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {

            log.error(
                    "Error processing AI response",
                    e
            );

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .type(activity.getType().name())
                    .recommendation(
                            "Unable to generate recommendation."
                    )
                    .improvements(
                            Collections.singletonList(
                                    "No recommendations available"
                            )
                    )
                    .suggestions(
                            Collections.singletonList(
                                    "No workout suggestions available"
                            )
                    )
                    .safety(
                            Collections.singletonList(
                                    "Stay hydrated and exercise safely."
                            )
                    )
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    private String extractJson(String text) {

        text = text
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start >= 0 && end > start) {

            return text.substring(
                    start,
                    end + 1
            );
        }

        throw new IllegalArgumentException(
                "No valid JSON found in AI response"
        );
    }

    private List<String> extractImprovements(
            JsonNode improvementsNode) {

        List<String> improvements =
                new ArrayList<>();

        if (improvementsNode.isArray()) {

            improvementsNode.forEach(
                    improvement -> {

                        String area =
                                improvement.path("area")
                                        .asText();

                        String recommendation =
                                improvement.path("recommendation")
                                        .asText();

                        improvements.add(
                                String.format(
                                        "%s: %s",
                                        area,
                                        recommendation
                                )
                        );
                    }
            );
        }

        return improvements.isEmpty()
                ? Collections.singletonList(
                        "No specific improvement provided"
                )
                : improvements;
    }

    private List<String> extractSuggestions(
            JsonNode workoutsNode) {

        List<String> suggestions =
                new ArrayList<>();

        if (workoutsNode.isArray()) {

            workoutsNode.forEach(
                    workout -> {

                        String workoutName =
                                workout.path("workout")
                                        .asText();

                        String description =
                                workout.path("description")
                                        .asText();

                        suggestions.add(
                                workoutName + ": " + description
                        );
                    }
            );
        }

        return suggestions.isEmpty()
                ? Collections.singletonList(
                        "No workout suggestions available"
                )
                : suggestions;
    }

    private List<String> extractSafetyTips(
            JsonNode safetyNode) {

        List<String> safetyTips =
                new ArrayList<>();

        if (safetyNode.isArray()) {

            safetyNode.forEach(
                    tip -> safetyTips.add(
                            tip.asText()
                    )
            );
        }

        return safetyTips.isEmpty()
                ? Collections.singletonList(
                        "Stay hydrated and listen to your body."
                )
                : safetyTips;
    }

    private String createPromptForActivity(
            Activity activity) {

        return """
                You are an expert fitness coach and sports scientist.

                Analyze the workout and return ONLY valid JSON.

                IMPORTANT:

                Return a raw JSON object only.

                Do not use markdown.
                Do not use ```json.
                Do not add explanations.
                Do not add notes.
                Do not add text before JSON.
                Do not add text after JSON.

                Your first character must be {
                Your last character must be }

                Return exactly this structure:

                {
                  "analysis": {
                    "overall": "",
                    "pace": "",
                    "heartRate": "",
                    "caloriesBurned": ""
                  },
                  "recommendations": [
                    {
                      "area": "",
                      "recommendation": ""
                    }
                  ],
                  "suggestedWorkouts": [
                    {
                      "workout": "",
                      "description": ""
                    }
                  ],
                  "safetyTips": [
                    ""
                  ]
                }

                USER ACTIVITY DATA

                User ID: %s
                Activity Type: %s
                Duration: %d minutes
                Calories Burned: %d
                Start Time: %s
                Additional Metrics: %s
                """
                .formatted(
                        activity.getUserId(),
                        activity.getType(),
                        activity.getDuration(),
                        activity.getCaloriesBurned(),
                        activity.getStartTime(),
                        activity.getAdditionalMetrics()
                );
    }
}