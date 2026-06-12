package com.fitness.aiservice.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService activityAIService;
    private final RecommendationRepository recommendationRepository;

    @KafkaListener(
            topics = "${kafka.topic.name}",
            groupId = "activity-processor-group"
    )
    public void processActivity(Activity activity) {

        log.info(
                "Received Activity for processing: {}",
                activity.getUserId()
        );

        Recommendation recommendation =
                activityAIService.generateRecommendation(
                        activity
                );

        Recommendation savedRecommendation =
                recommendationRepository.save(
                        recommendation
                );

        log.info(
                "Recommendation saved successfully with id: {}",
                savedRecommendation.getId()
        );
    }
}