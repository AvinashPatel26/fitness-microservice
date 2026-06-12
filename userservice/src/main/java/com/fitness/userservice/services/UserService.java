package com.fitness.userservice.services;


import org.springframework.stereotype.Service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.models.User;
import com.fitness.userservice.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;

    public UserResponse register(RegisterRequest request) {

        if (repository.existsByEmail(request.getEmail())) {
        	User existingUser = repository.findByEmail(request.getEmail());
        	UserResponse userResponse = new UserResponse();
            userResponse.setId(existingUser.getId());
            userResponse.setEmail(existingUser.getEmail());
            userResponse.setFirstName(existingUser.getFirstName());
            
            userResponse.setLastName(existingUser.getLastName());
            userResponse.setPassword(existingUser.getPassword());
            userResponse.setCreatedAt(existingUser.getCreatedAt());
            userResponse.setUpdatedAt(existingUser.getUpdatedAt());

            return userResponse;
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setKeyCloakId(request.getKeyCloakId());
        user.setLastName(request.getLastName());
        user.setPassword(request.getPassword());

        User savedUser = repository.save(user);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(savedUser.getId());
        userResponse.setEmail(savedUser.getEmail());
        userResponse.setKeyCloakId(savedUser.getKeyCloakId());
        userResponse.setFirstName(savedUser.getFirstName());
        userResponse.setLastName(savedUser.getLastName());
        userResponse.setPassword(savedUser.getPassword());
        userResponse.setCreatedAt(savedUser.getCreatedAt());
        userResponse.setUpdatedAt(savedUser.getUpdatedAt());

        return userResponse;
    }
    public UserResponse getUserProfile(String userId) {

        User user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPassword(user.getPassword());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }
	public Boolean existByUserId(String userId) {
		log.info("calling user service for {}", userId);

		return repository.existsByKeyCloakId(userId);
		
	}
}