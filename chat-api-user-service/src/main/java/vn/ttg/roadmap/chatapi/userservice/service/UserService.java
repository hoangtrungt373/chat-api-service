package vn.ttg.roadmap.chatapi.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttg.roadmap.chatapi.userservice.dto.OAuth2UserInfo;
import vn.ttg.roadmap.chatapi.userservice.entity.User;
import vn.ttg.roadmap.chatapi.userservice.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User registerOAuth2User(OAuth2UserInfo userInfo, User.Provider provider) {
        log.info("Registering new OAuth2 user: {} with provider: {}", userInfo.getEmail(), provider);
        
        User user = User.builder()
                .userUuid(UUID.randomUUID().toString())  // Generate USER_UUID
                .username(generateUsername(userInfo.getName()))
                .email(userInfo.getEmail())
                .password(passwordEncoder.encode("temp"))  // OAuth2 users don't have password
                .firstName(extractFirstName(userInfo.getName()))
                .lastName(extractLastName(userInfo.getName()))
                .profilePicture(userInfo.getImageUrl())
                .provider(provider)
                .providerId(userInfo.getId())
                .emailVerified(true)
                .enabled(true)
                .status(User.UserStatus.OFFLINE)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Successfully registered OAuth2 user: {} (USER_ID: {}, USER_UUID: {})", 
                savedUser.getEmail(), savedUser.getId(), savedUser.getUserUuid());
        return savedUser;
    }
    
    public User updateOAuth2User(User existingUser, OAuth2UserInfo userInfo) {
        log.info("Updating OAuth2 user: {} (USER_ID: {})", existingUser.getEmail(), existingUser.getId());
        
        existingUser.setUsername(generateUsername(userInfo.getName()));
        existingUser.setFirstName(extractFirstName(userInfo.getName()));
        existingUser.setLastName(extractLastName(userInfo.getName()));
        existingUser.setProfilePicture(userInfo.getImageUrl());
        existingUser.setUsrLastModification("system");  // Update audit field
        
        User updatedUser = userRepository.save(existingUser);
        log.info("Successfully updated OAuth2 user: {} (USER_ID: {})", 
                updatedUser.getEmail(), updatedUser.getId());
        return updatedUser;
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public User findByUserUuid(String userUuid) {
        return userRepository.findByUserUuid(userUuid).orElse(null);
    }
    
    public User findByProviderAndProviderId(User.Provider provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
    }
    
    public void updateStatus(Integer userId, User.UserStatus status) {
        userRepository.updateStatus(userId, status);
        log.info("Updated status for user ID: {} to {}", userId, status);
    }
    
    public Optional<User> findById(Integer userId) {
        return userRepository.findById(userId);
    }
    
    public Optional<User> findByUserUuidOptional(String userUuid) {
        return userRepository.findByUserUuid(userUuid);
    }
    
    private String generateUsername(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "user_" + System.currentTimeMillis();
        }
        
        String baseUsername = fullName.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + counter;
            counter++;
        }
        
        return username;
    }
    
    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }
    
    private String extractLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 1) {
            return String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        }
        
        return null;
    }
}
