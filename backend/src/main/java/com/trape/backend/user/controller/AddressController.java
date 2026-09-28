package com.trape.backend.user.controller;

import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.security.UserPrincipal;
import com.trape.backend.user.dto.UserDtos.AddressRequest;
import com.trape.backend.user.dto.UserDtos.AddressResponse;
import com.trape.backend.user.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(addressService.list(principal.getId()));
    }

    @PostMapping
    public ApiResponse<AddressResponse> add(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.add(principal.getId(), request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AddressResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        addressService.delete(principal.getId(), id);
        return ApiResponse.ok(null);
    }
}
