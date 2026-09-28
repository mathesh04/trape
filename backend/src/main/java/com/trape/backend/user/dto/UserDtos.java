package com.trape.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class UserDtos {

    public record ProfileResponse(UUID id, String email, String phone, String fullName, String role) {
    }

    public record UpdateProfileRequest(String fullName, String phone) {
    }

    public record AddressRequest(
            String label,
            @NotBlank String line1,
            String line2,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String pincode,
            @NotBlank String phone,
            boolean isDefault
    ) {
    }

    public record AddressResponse(
            UUID id, String label, String line1, String line2,
            String city, String state, String pincode, String phone, boolean isDefault
    ) {
    }
}
