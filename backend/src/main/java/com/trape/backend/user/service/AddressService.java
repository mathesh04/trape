package com.trape.backend.user.service;

import com.trape.backend.common.exception.ApiException;
import com.trape.backend.user.dto.UserDtos.AddressRequest;
import com.trape.backend.user.dto.UserDtos.AddressResponse;
import com.trape.backend.user.entity.Address;
import com.trape.backend.user.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public AddressResponse add(UUID userId, AddressRequest request) {
        if (request.isDefault()) clearExistingDefault(userId);
        Address address = new Address(userId, request.label(), request.line1(), request.line2(),
                request.city(), request.state(), request.pincode(), request.phone(), request.isDefault());
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Address"));
        if (request.isDefault() && !address.isDefault()) clearExistingDefault(userId);
        address.setLabel(request.label());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPincode(request.pincode());
        address.setPhone(request.phone());
        address.setDefault(request.isDefault());
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Address"));
        addressRepository.delete(address);
    }

    private void clearExistingDefault(UUID userId) {
        addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .filter(Address::isDefault)
                .forEach(a -> {
                    a.setDefault(false);
                    addressRepository.save(a);
                });
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPincode(), a.getPhone(), a.isDefault());
    }
}
