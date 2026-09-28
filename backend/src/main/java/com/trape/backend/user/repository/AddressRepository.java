package com.trape.backend.user.repository;

import com.trape.backend.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);
}
