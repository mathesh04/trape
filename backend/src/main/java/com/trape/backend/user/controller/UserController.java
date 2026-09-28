package com.trape.backend.user.controller;

import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.security.UserPrincipal;
import com.trape.backend.user.dto.UserDtos.ProfileResponse;
import com.trape.backend.user.dto.UserDtos.UpdateProfileRequest;
import com.trape.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getProfile(principal.getId()));
    }

    @PatchMapping
    public ApiResponse<ProfileResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                        @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(principal.getId(), request));
    }
}
