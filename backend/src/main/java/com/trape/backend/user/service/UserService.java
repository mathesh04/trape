package com.trape.backend.user.service;

import com.trape.backend.auth.entity.User;
import com.trape.backend.auth.repository.UserRepository;
import com.trape.backend.common.exception.ApiException;
import com.trape.backend.user.dto.UserDtos.ProfileResponse;
import com.trape.backend.user.dto.UserDtos.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        return toResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.phone() != null) user.setPhone(request.phone());
        return toResponse(userRepository.save(user));
    }

    private ProfileResponse toResponse(User u) {
        return new ProfileResponse(u.getId(), u.getEmail(), u.getPhone(), u.getFullName(), u.getRole());
    }
}
