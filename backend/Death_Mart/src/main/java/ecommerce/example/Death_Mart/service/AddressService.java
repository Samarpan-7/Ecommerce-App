package ecommerce.example.Death_Mart.service;

import ecommerce.example.Death_Mart.dto.request.AddressRequest;
import ecommerce.example.Death_Mart.dto.response.AddressResponse;
import ecommerce.example.Death_Mart.entity.Address;
import ecommerce.example.Death_Mart.entity.User;
import ecommerce.example.Death_Mart.exception.BadRequestException;
import ecommerce.example.Death_Mart.exception.ResourceNotFoundException;
import ecommerce.example.Death_Mart.repository.AddressRepository;
import ecommerce.example.Death_Mart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<Address> existing = addressRepository.findByUserId(userId);
        boolean isFirst = existing.isEmpty();
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) || isFirst;

        if (makeDefault) {
            existing.forEach(a -> a.setIsDefault(false));
            addressRepository.saveAll(existing);
        }

        Address address = Address.builder()
                .user(user)
                .street(request.getStreet().trim())
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .zipCode(request.getZipCode().trim())
                .country(request.getCountry() != null ? request.getCountry().trim() : "India")
                .isDefault(makeDefault)
                .build();

        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            List<Address> existing = addressRepository.findByUserId(userId);
            existing.forEach(a -> a.setIsDefault(false));
            addressRepository.saveAll(existing);
            address.setIsDefault(true);
        }

        address.setStreet(request.getStreet().trim());
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setZipCode(request.getZipCode().trim());
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry().trim());
        }

        Address updated = addressRepository.save(address);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address target = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        List<Address> userAddresses = addressRepository.findByUserId(userId);
        for (Address a : userAddresses) {
            a.setIsDefault(a.getId().equals(addressId));
        }
        addressRepository.saveAll(userAddresses);

        return mapToResponse(target);
    }

    public AddressResponse mapToResponse(Address address) {
        if (address == null) return null;
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }
}
